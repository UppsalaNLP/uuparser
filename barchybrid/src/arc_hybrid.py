from utils import ParseForest, read_conll, write_conll
from operator import itemgetter
from itertools import chain
import utils, time, random
import numpy as np
from copy import deepcopy
from collections import defaultdict
import codecs, json

class ArcHybridLSTM:
    def __init__(self, vocab, options):

        # import here so we don't load Dynet if just running parser.py --help for example
        from multilayer_perceptron import MLP
        from feature_extractor import FeatureExtractor
        import dynet as dy
        global dy

        global LEFT_ARC, RIGHT_ARC, SHIFT, SWAP
        LEFT_ARC, RIGHT_ARC, SHIFT, SWAP = 0,1,2,3

        self.model = dy.ParameterCollection()
        self.trainer = dy.AdamTrainer(self.model, alpha=options.learning_rate)

        self.activations = {'tanh': dy.tanh, 'sigmoid': dy.logistic, 'relu':
                            dy.rectify, 'tanh3': (lambda x:
                            dy.tanh(dy.cwise_multiply(dy.cwise_multiply(x, x), x)))}
        self.activation = self.activations[options.activation]

        self.oracle = options.oracle

        self.headFlag = options.headFlag
        self.rlMostFlag = options.rlMostFlag
        self.rlFlag = options.rlFlag
        self.k = options.k

        #dimensions depending on extended features
        self.nnvecs = (1 if self.headFlag else 0) + (2 if self.rlFlag or self.rlMostFlag else 0)
        self.feature_extractor = FeatureExtractor(self.model, options, vocab, self.nnvecs)
        self.irels = self.feature_extractor.irels

        if options.no_bilstms > 0:
            mlp_in_dims = options.lstm_output_size*2*self.nnvecs*(self.k+1)
        else:
            mlp_in_dims = self.feature_extractor.lstm_input_size*self.nnvecs*(self.k+1)

        self.unlabeled_MLP = MLP(self.model, 'unlabeled', mlp_in_dims, options.mlp_hidden_dims,
                                 options.mlp_hidden2_dims, 4, self.activation)
        self.labeled_MLP = MLP(self.model, 'labeled' ,mlp_in_dims, options.mlp_hidden_dims,
                               options.mlp_hidden2_dims,2*len(self.irels)+2,self.activation)


    def __evaluate(self, stack, buf, train):
        """
        ret = [left arc,
               right arc
               shift]

        RET[i] = (rel, transition, score1, score2) for shift, l_arc and r_arc
         shift = 2 (==> rel=None) ; l_arc = 0; r_acr = 1

        ret[i][j][2] ~= ret[i][j][3] except the latter is a dynet
        expression used in the loss, the first is used in rest of training
        """

        #feature rep
        empty = self.feature_extractor.empty
        topStack = [ stack.roots[-i-1].lstms if len(stack) > i else [empty] for i in xrange(self.k) ]
        topBuffer = [ buf.roots[i].lstms if len(buf) > i else [empty] for i in xrange(1) ]

        input = dy.concatenate(list(chain(*(topStack + topBuffer))))
        output = self.unlabeled_MLP(input)
        routput = self.labeled_MLP(input)


        #scores, unlabeled scores
        scrs, uscrs = routput.value(), output.value()

        #transition conditions
        left_arc_conditions = len(stack) > 0
        right_arc_conditions = len(stack) > 1
        shift_conditions = buf.roots[0].id != 0
        swap_conditions = len(stack) > 0 and stack.roots[-1].id < buf.roots[0].id

        if not train:
            #(avoiding the multiple roots problem: disallow left-arc from root
            #if stack has more than one element
            left_arc_conditions = left_arc_conditions and not (buf.roots[0].id == 0 and len(stack) > 1)

        uscrs0 = uscrs[0]
        uscrs1 = uscrs[1]
        uscrs2 = uscrs[2]
        uscrs3 = uscrs[3]

        if train:
            output0 = output[0]
            output1 = output[1]
            output2 = output[2]
            output3 = output[3]


            ret = [ [ (rel, LEFT_ARC, scrs[2 + j * 2] + uscrs2, routput[2 + j * 2 ] + output2) for j, rel in enumerate(self.irels) ] if left_arc_conditions else [],
                   [ (rel, RIGHT_ARC, scrs[3 + j * 2] + uscrs3, routput[3 + j * 2 ] + output3) for j, rel in enumerate(self.irels) ] if right_arc_conditions else [],
                   [ (None, SHIFT, scrs[0] + uscrs0, routput[0] + output0) ] if shift_conditions else [] ,
                    [ (None, SWAP, scrs[1] + uscrs1, routput[1] + output1) ] if swap_conditions else [] ]
        else:
            s1,r1 = max(zip(scrs[2::2],self.irels))
            s2,r2 = max(zip(scrs[3::2],self.irels))
            s1 += uscrs2
            s2 += uscrs3
            ret = [ [ (r1, LEFT_ARC, s1) ] if left_arc_conditions else [],
                   [ (r2, RIGHT_ARC, s2) ] if right_arc_conditions else [],
                   [ (None, SHIFT, scrs[0] + uscrs0) ] if shift_conditions else [] ,
                    [ (None, SWAP, scrs[1] + uscrs1) ] if swap_conditions else [] ]
        return ret


    def Save(self, filename):
        print 'Saving model to ' + filename
        self.model.save(filename)

    def Load(self, filename):
        print 'Loading model from ' + filename
        self.model.populate(filename)


    def apply_transition(self,best,stack,buf,hoffset):
        if best[1] == SHIFT:
            stack.roots.append(buf.roots[0])
            del buf.roots[0]

        elif best[1] == SWAP:
            child = stack.roots.pop()
            buf.roots.insert(1,child)

        elif best[1] == LEFT_ARC:
            child = stack.roots.pop()
            parent = buf.roots[0]

        elif best[1] == RIGHT_ARC:
            child = stack.roots.pop()
            parent = stack.roots[-1]

        if best[1] == LEFT_ARC or best[1] == RIGHT_ARC:
            #attach
            child.pred_parent_id = parent.id
            child.pred_relation = best[0]
            #update head representation
            if self.rlMostFlag:
                #deepest leftmost/rightmost descendant
                parent.lstms[best[1] + hoffset] = child.lstms[best[1] + hoffset]
            if self.rlFlag:
                #leftmost/rightmost child
                parent.lstms[best[1] + hoffset] = child.vec

    def calculate_cost(self,scores,s0,s1,b,beta,stack_ids):
        if len(scores[LEFT_ARC]) == 0:
            left_cost = 1
        else:
            left_cost = len(s0[0].rdeps) + int(s0[0].parent_id != b[0].id and s0[0].id in s0[0].parent_entry.rdeps)


        if len(scores[RIGHT_ARC]) == 0:
            right_cost = 1
        else:
            right_cost = len(s0[0].rdeps) + int(s0[0].parent_id != s1[0].id and s0[0].id in s0[0].parent_entry.rdeps)


        if len(scores[SHIFT]) == 0:
            shift_cost = 1
            shift_case = 0
        elif len([item for item in beta if item.projective_order < b[0].projective_order and item.id > b[0].id ])> 0:
            shift_cost = 0
            shift_case = 1
        else:
            shift_cost = len([d for d in b[0].rdeps if d in stack_ids]) + int(len(s0)>0 and b[0].parent_id in stack_ids[:-1] and b[0].id in b[0].parent_entry.rdeps)
            shift_case = 2


        if len(scores[SWAP]) == 0 :
            swap_cost = 1
        elif s0[0].projective_order > b[0].projective_order:
            swap_cost = 0
            #disable all the others
            left_cost = right_cost = shift_cost = 1
        else:
            swap_cost = 1

        costs = (left_cost, right_cost, shift_cost, swap_cost,1)
        return costs,shift_case


    def oracle_updates(self,best,b,s0,stack_ids,shift_case):
        if best[1] == SHIFT:
            if shift_case ==2:
                if b[0].parent_entry.id in stack_ids[:-1] and b[0].id in b[0].parent_entry.rdeps:
                    b[0].parent_entry.rdeps.remove(b[0].id)
                blocked_deps = [d for d in b[0].rdeps if d in stack_ids]
                for d in blocked_deps:
                    b[0].rdeps.remove(d)

        elif best[1] == LEFT_ARC or best[1] == RIGHT_ARC:
            s0[0].rdeps = []
            if s0[0].id in s0[0].parent_entry.rdeps:
                s0[0].parent_entry.rdeps.remove(s0[0].id)

    def Predict(self, treebanks, datasplit, options):
        reached_max_swap = 0
        char_map = {}
        if options.char_map_file:
            char_map_fh = codecs.open(options.char_map_file,encoding='utf-8')
            char_map = json.loads(char_map_fh.read())
        # should probably use a namedtuple in get_vocab to make this prettier
        _, test_words, test_chars, _, _, _, test_treebanks, test_langs = utils.get_vocab(treebanks,datasplit,char_map)

        # get external embeddings for the set of words and chars in the
        # test vocab but not in the training vocab
        test_embeddings = defaultdict(lambda: {})
        if options.word_emb_size > 0 and options.ext_word_emb_file:
            new_test_words = \
                set(test_words) - self.feature_extractor.words.viewkeys()

            print "Number of OOV word types at test time: %i (out of %i)" % (
                len(new_test_words), len(test_words))

            if len(new_test_words) > 0:
                # no point loading embeddings if there are no words to look for
                for lang in test_langs:
                    embeddings = utils.get_external_embeddings(
                        options,
                        emb_file=options.ext_word_emb_file,
                        lang=lang,
                        words=new_test_words
                    )
                    test_embeddings["words"].update(embeddings)
                if len(test_langs) > 1 and test_embeddings["words"]:
                    print "External embeddings found for %i words "\
                          "(out of %i)" % \
                          (len(test_embeddings["words"]), len(new_test_words))

        if options.char_emb_size > 0:
            new_test_chars = \
                set(test_chars) - self.feature_extractor.chars.viewkeys()
            print "Number of OOV char types at test time: %i (out of %i)" % (
                len(new_test_chars), len(test_chars))

            if len(new_test_chars) > 0:
                for lang in test_langs:
                    embeddings = utils.get_external_embeddings(
                        options,
                        emb_file=options.ext_char_emb_file,
                        lang=lang,
                        words=new_test_chars,
                        chars=True
                    )
                    test_embeddings["chars"].update(embeddings)
                if len(test_langs) > 1 and test_embeddings["chars"]:
                    print "External embeddings found for %i chars "\
                          "(out of %i)" % \
                          (len(test_embeddings["chars"]), len(new_test_chars))

        data = utils.read_conll_dir(treebanks,datasplit,char_map=char_map)
        for iSentence, osentence in enumerate(data,1):
            sentence = deepcopy(osentence)
            reached_swap_for_i_sentence = False
            max_swap = 2*len(sentence)
            iSwap = 0
            self.feature_extractor.Init(options)
            conll_sentence = [entry for entry in sentence if isinstance(entry, utils.ConllEntry)]
            conll_sentence = conll_sentence[1:] + [conll_sentence[0]]
            self.feature_extractor.getWordEmbeddings(conll_sentence, False, options, test_embeddings)
            stack = ParseForest([])
            buf = ParseForest(conll_sentence)

            hoffset = 1 if self.headFlag else 0

            for root in conll_sentence:
                root.lstms = [root.vec] if self.headFlag else []
                root.lstms += [root.vec for _ in range(self.nnvecs - hoffset)]
                root.relation = root.relation if root.relation in self.irels else 'runk'


            while not (len(buf) == 1 and len(stack) == 0):
                scores = self.__evaluate(stack, buf, False)
                best = max(chain(*(scores if iSwap < max_swap else scores[:3] )), key = itemgetter(2) )
                if iSwap == max_swap and not reached_swap_for_i_sentence:
                    reached_max_swap += 1
                    reached_swap_for_i_sentence = True
                    print "reached max swap in %d out of %d sentences"%(reached_max_swap, iSentence)
                self.apply_transition(best,stack,buf,hoffset)
                if best[1] == SWAP:
                    iSwap += 1

            dy.renew_cg()

            #keep in memory the information we need, not all the vectors
            oconll_sentence = [entry for entry in osentence if isinstance(entry, utils.ConllEntry)]
            oconll_sentence = oconll_sentence[1:] + [oconll_sentence[0]]
            for tok_o, tok in zip(oconll_sentence, conll_sentence):
                tok_o.pred_relation = tok.pred_relation
                tok_o.pred_parent_id = tok.pred_parent_id
            yield osentence


    def Train(self, trainData, options):
        mloss = 0.0
        eloss = 0.0
        eerrors = 0
        lerrors = 0
        etotal = 0
        ninf = -float('inf')


        beg = time.time()
        start = time.time()

        random.shuffle(trainData) # in certain cases the data will already have been shuffled after being read from file or while creating dev data
        print "Length of training data: ", len(trainData)

        errs = []

        self.feature_extractor.Init(options)

        for iSentence, sentence in enumerate(trainData,1):
            if iSentence % 100 == 0:
                loss_message = 'Processing sentence number: %d'%iSentence + \
                ' Loss: %.3f'%(eloss / etotal)+ \
                ' Errors: %.3f'%((float(eerrors)) / etotal)+\
                ' Labeled Errors: %.3f'%(float(lerrors) / etotal)+\
                ' Time: %.2gs'%(time.time()-start)
                print loss_message
                start = time.time()
                eerrors = 0
                eloss = 0.0
                etotal = 0
                lerrors = 0

            sentence = deepcopy(sentence) # ensures we are working with a clean copy of sentence and allows memory to be recycled each time round the loop

            conll_sentence = [entry for entry in sentence if isinstance(entry, utils.ConllEntry)]
            conll_sentence = conll_sentence[1:] + [conll_sentence[0]]
            self.feature_extractor.getWordEmbeddings(conll_sentence, True, options)
            stack = ParseForest([])
            buf = ParseForest(conll_sentence)
            hoffset = 1 if self.headFlag else 0

            for root in conll_sentence:
                root.lstms = [root.vec] if self.headFlag else []
                root.lstms += [root.vec for _ in range(self.nnvecs - hoffset)]
                root.relation = root.relation if root.relation in self.irels else 'runk'

            while not (len(buf) == 1 and len(stack) == 0):
                scores = self.__evaluate(stack, buf, True)

                #to ensure that we have at least one wrong operation
                scores.append([(None, 4, ninf ,None)])

                stack_ids = [sitem.id for sitem in stack.roots]

                s1 = [stack.roots[-2]] if len(stack) > 1 else []
                s0 = [stack.roots[-1]] if len(stack) > 0 else []
                b = [buf.roots[0]] if len(buf) > 0 else []
                beta = buf.roots[1:] if len(buf) > 1 else []

                costs, shift_case = self.calculate_cost(scores,s0,s1,b,beta,stack_ids)

                bestValid = list(( s for s in chain(*scores) if costs[s[1]] == 0 and ( s[1] == SHIFT or s[1] == SWAP or  s[0] == s0[0].relation ) ))

                bestValid = max(bestValid, key=itemgetter(2))
                bestWrong = max(( s for s in chain(*scores) if costs[s[1]] != 0 or ( s[1] != SHIFT and s[1] != SWAP and s[0] != s0[0].relation ) ), key=itemgetter(2))

                #force swap
                if costs[SWAP]== 0:
                    best = bestValid
                else:
                    #select a transition to follow
                    # + aggresive exploration
                    #1: might want to experiment with that parameter
                    if bestWrong[1] == SWAP:
                        best = bestValid
                    else:
                        best = bestValid if ( (not self.oracle) or (bestValid[2] - bestWrong[2] > 1.0) or (bestValid[2] > bestWrong[2] and random.random() > 0.1) ) else bestWrong

                if best[1] == LEFT_ARC or best[1] ==RIGHT_ARC:
                    child = s0[0]

                #updates for the dynamic oracle
                if self.oracle:
                    self.oracle_updates(best,b,s0,stack_ids,shift_case)

                self.apply_transition(best,stack,buf,hoffset)

                if bestValid[2] < bestWrong[2] + 1.0:
                    loss = bestWrong[3] - bestValid[3]
                    mloss += 1.0 + bestWrong[2] - bestValid[2]
                    eloss += 1.0 + bestWrong[2] - bestValid[2]
                    errs.append(loss)

                #labeled errors
                if best[1] == LEFT_ARC or best[1] ==RIGHT_ARC:
                    if (child.pred_parent_id != child.parent_id or child.pred_relation != child.relation):
                        lerrors += 1
                        #attachment error
                        if child.pred_parent_id != child.parent_id:
                            eerrors += 1

                #??? when did this happen and why?
                if best[1] == 0 or best[1] == 2:
                    etotal += 1

            #footnote 8 in Eli's original paper
            if len(errs) > 50: # or True:
                eerrs = dy.esum(errs)
                scalar_loss = eerrs.scalar_value() #forward
                eerrs.backward()
                self.trainer.update()
                errs = []
                lerrs = []

                dy.renew_cg()
                self.feature_extractor.Init(options)

        if len(errs) > 0:
            eerrs = (dy.esum(errs))
            eerrs.scalar_value()
            eerrs.backward()
            self.trainer.update()

            errs = []
            lerrs = []

            dy.renew_cg()

        self.trainer.update()
        print "Loss: ", mloss/iSentence
        print "Total Training Time: %.2gs"%(time.time()-beg)
