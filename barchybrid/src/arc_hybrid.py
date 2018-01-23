import dynet as dy
from utils import ParseForest, read_conll, write_conll
from operator import itemgetter
from itertools import chain
import utils, time, random
import numpy as np
from copy import deepcopy

class ArcHybridLSTM:
    def __init__(self, words, pos, rels, cpos, langs, w2i, ch, options):

        self.model = dy.ParameterCollection()
        self.trainer = dy.AdamTrainer(self.model, alpha=options.learning_rate)

        rels.append('runk')

        self.activations = {'tanh': dy.tanh, 'sigmoid': dy.logistic, 'relu':
                            dy.rectify, 'tanh3': (lambda x:
                                                  dy.tanh(dy.cwise_multiply(dy.cwise_multiply(x, x), x)))}
        self.activation = self.activations[options.activation]

        self.oracle = options.oracle
        self.disableBilstm = options.disableBilstm
        self.multiling = options.multiling
        self.ldims = options.lstm_dims
        self.cldims = options.chlstm_dims
        self.wdims = options.wembedding_dims
        self.cdims = options.cembedding_dims
        self.langdims = options.lembedding_dims
        self.wordsCount = words
        self.vocab = {word: ind+2 for word, ind in w2i.iteritems()} # +2 for MLP padding vector and OOV vector
        self.chars = {char: ind+1 for ind, char in enumerate(ch)} # +1 for OOV vector
        self.rels = {word: ind for ind, word in enumerate(rels)}
        if langs:
            self.langs = {lang: ind+1 for ind, lang in enumerate(langs)} # +1 for padding vector
        else:
            self.langs = None
        self.irels = rels
        self.debug = options.debug


        self.headFlag = options.headFlag
        self.rlMostFlag = options.rlMostFlag
        self.rlFlag = options.rlFlag
        self.k = options.window

        #dimensions depending on extended features
        self.nnvecs = (1 if self.headFlag else 0) + (2 if self.rlFlag or self.rlMostFlag else 0)

        self.external_embedding = None
        if options.external_embedding is not None:
            self.get_external_embeddings(options.external_embedding)

        dims = self.wdims + (self.edim if self.external_embedding is\
                                      not None else 0) + (self.langdims if
                                                          self.multiling else 0) + 2 * self.cldims

        if not self.disableBilstm:
            self.surfaceBuilders = [dy.VanillaLSTMBuilder(1, dims, self.ldims, self.model),
                                    dy.VanillaLSTMBuilder(1, dims, self.ldims, self.model)]
            self.bsurfaceBuilders = [dy.VanillaLSTMBuilder(1, 2* self.ldims,
                                                        self.ldims , self.model),
                                     dy.VanillaLSTMBuilder(1, 2* self.ldims,
                                                    self.ldims , self.model)]
        else:
            self.ldims = int(dims * 0.5)

        self.charBuilders = [dy.VanillaLSTMBuilder(1, self.cdims, self.cldims, self.model),
                             dy.VanillaLSTMBuilder(1, self.cdims, self.cldims, self.model)]

        self.hidden_units = options.hidden_units
        self.hidden2_units = options.hidden2_units

        self.clookup = self.model.add_lookup_parameters((len(ch) + 1, self.cdims))
        self.wlookup = self.model.add_lookup_parameters((len(words) + 2, self.wdims))
        if self.multiling and self.langdims > 0:
            self.langslookup = self.model.add_lookup_parameters((len(langs) + 1, self.langdims))

        #used in the PaddingVec
        self.word2lstm = self.model.add_parameters((self.ldims * 2, dims))
        self.word2lstmbias = self.model.add_parameters((self.ldims *2))
        self.chPadding = self.model.add_parameters((self.cldims *2))

        self.hidLayer = self.model.add_parameters((self.hidden_units, self.ldims * 2 * self.nnvecs * (self.k + 1)))
        self.hidBias = self.model.add_parameters((self.hidden_units))

        self.hid2Layer = self.model.add_parameters((self.hidden2_units, self.hidden_units))
        self.hid2Bias = self.model.add_parameters((self.hidden2_units))


        self.outLayer = self.model.add_parameters((4, self.hidden2_units if self.hidden2_units > 0 else self.hidden_units))
        self.outBias = self.model.add_parameters((4))

        # r stands for relation

        self.rhidLayer = self.model.add_parameters((self.hidden_units, self.ldims * 2 * self.nnvecs * (self.k + 1)))
        self.rhidBias = self.model.add_parameters((self.hidden_units))

        self.rhid2Layer = self.model.add_parameters((self.hidden2_units, self.hidden_units))
        self.rhid2Bias = self.model.add_parameters((self.hidden2_units))

        self.routLayer = self.model.add_parameters((2 * len(self.irels) + 2, self.hidden2_units if self.hidden2_units > 0 else self.hidden_units))
        self.routBias = self.model.add_parameters((2 * len(self.irels) + 2))

    def __evaluate(self, stack, buf, train):
        #feature rep
        topStack = [ stack.roots[-i-1].lstms if len(stack) > i else [self.empty] for i in xrange(self.k) ]
        topBuffer = [ buf.roots[i].lstms if len(buf) > i else [self.empty] for i in xrange(1) ]

        input = dy.concatenate(list(chain(*(topStack + topBuffer))))


        if self.hidden2_units > 0:
            #paper formula:
                # MLP\theta(x) = W2 * tanh(W1 * x * b1) + b2
                #x = input
                #W1 = (r)hidLayer; W2 = (r)outLayer
                #b1 = (r)hidBias; b2 = (r)outBias
                # MLP\theta(x) = (r)output

            routput = (self.routLayer.expr() * self.activation(self.rhid2Bias.expr() + self.rhid2Layer.expr() * self.activation(self.rhidLayer.expr() * input + self.rhidBias.expr())) + self.routBias.expr())
        else:
            routput = (self.routLayer.expr() * self.activation(self.rhidLayer.expr() * input + self.rhidBias.expr()) + self.routBias.expr())

        if self.hidden2_units > 0:
            output = (self.outLayer.expr() * self.activation(self.hid2Bias.expr() + self.hid2Layer.expr() * self.activation(self.hidLayer.expr() * input + self.hidBias.expr())) + self.outBias.expr())
        else:
            output = (self.outLayer.expr() * self.activation(self.hidLayer.expr() * input + self.hidBias.expr()) + self.outBias.expr())


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

        uscrs0 = uscrs[0] #shift
        uscrs1 = uscrs[1] #swap
        uscrs2 = uscrs[2] #left-arc
        uscrs3 = uscrs[3] #right-arc

        if train:
            output0 = output[0]
            output1 = output[1]
            output2 = output[2]
            output3 = output[3]

            # ret = [left arc,
            #       right arc
            #       shift]

            #RET[i] = (rel, transition, score1, score2) for shift, l_arc and r_arc
            # shift = 2 (==> rel=None) ; l_arc = 0; r_acr = 1

            #ret[i][j][2] ~= ret[i][j][3] except the latter is a dynet
            #expression used in the loss, the first is used in rest of training


            ret = [ [ (rel, 0, scrs[2 + j * 2] + uscrs2, routput[2 + j * 2 ] + output2) for j, rel in enumerate(self.irels) ] if left_arc_conditions else [],
                   [ (rel, 1, scrs[3 + j * 2] + uscrs3, routput[3 + j * 2 ] + output3) for j, rel in enumerate(self.irels) ] if right_arc_conditions else [],
                   [ (None, 2, scrs[0] + uscrs0, routput[0] + output0) ] if shift_conditions else [] ,
                    [ (None, 3, scrs[1] + uscrs1, routput[1] + output1) ] if swap_conditions else [] ]
        else:
            s1,r1 = max(zip(scrs[2::2],self.irels))
            s2,r2 = max(zip(scrs[3::2],self.irels))
            s1 += uscrs2
            s2 += uscrs3
            ret = [ [ (r1, 0, s1) ] if left_arc_conditions else [],
                   [ (r2, 1, s2) ] if right_arc_conditions else [],
                   [ (None, 2, scrs[0] + uscrs0) ] if shift_conditions else [] ,
                    [ (None, 3, scrs[1] + uscrs1) ] if swap_conditions else [] ]
        return ret


    def Save(self, filename):
        print 'Saving model to ' + filename
        self.model.save(filename)

    def Load(self, filename):
        print 'Loading model from ' + filename
        self.model.populate(filename)

    def Init(self):
        evec = self.elookup[1] if self.external_embedding is not None else None
        paddingWordVec = self.wlookup[1]
        paddingLangVec = self.langslookup[0] if self.multiling and self.langdims > 0 else None

        self.paddingVec = dy.tanh(self.word2lstm.expr() * dy.concatenate(filter(None,
                                                                          [paddingWordVec,
                                                                           evec,
                                                                           self.chPadding.expr(),
                                                                          paddingLangVec])) + self.word2lstmbias.expr() )
        self.empty = self.paddingVec if self.nnvecs == 1 else dy.concatenate([self.paddingVec for _ in xrange(self.nnvecs)])



    def get_external_embeddings(self,external_embedding_file):
        external_embedding_fp = codecs.open(external_embedding_file,'r',encoding='utf-8')
        external_embedding_fp.readline()
        self.external_embedding = {}
        for line in external_embedding_fp:
            line = line.strip().split()
            self.external_embedding[line[0]] = [float(f) for f in line[1:]]

        external_embedding_fp.close()

        self.edim = len(self.external_embedding.values()[0])
        self.noextrn = [0.0 for _ in xrange(self.edim)] #???
        self.extrnd = {word: i + 3 for i, word in enumerate(self.external_embedding)}
        self.elookup = self.model.add_lookup_parameters((len(self.external_embedding) + 3, self.edim))
        for word, i in self.extrnd.iteritems():
            self.elookup.init_row(i, self.external_embedding[word])
        self.extrnd['*PAD*'] = 1
        self.extrnd['*INITIAL*'] = 2

        print 'Load external embedding. Vector dimensions', self.edim

    def getWordEmbeddings(self, sentence, train):
        for root in sentence:
            wordcount = float(self.wordsCount.get(root.norm, 0))
            noDropFlag =  not train or (random.random() < (wordcount/(0.25+wordcount)))
            root.wordvec = self.wlookup[int(self.vocab.get(root.norm, 0)) if noDropFlag else 0]
            self.run_char_bilstm(root,train)

            if self.external_embedding is not None:
                if not noDropFlag and random.random() < 0.5:
                    root.evec = self.elookup[0]
                elif root.form in self.external_embedding:
                    root.evec = self.elookup[self.extrnd[root.form]]
                elif root.norm in self.external_embedding:
                    root.evec = self.elookup[self.extrnd[root.norm]]
                else:
                    root.evec = self.elookup[0]
            else:
                root.evec = None

            if self.multiling:
                root.langvec = self.langslookup[self.langs[root.language_id]] if self.langdims > 0 else None
            else:
                root.langvec = None
            root.word_ext_vec = dy.concatenate(filter(None, [root.wordvec,
                                                          root.evec,
                                                          root.chVec,
                                                          root.langvec]))
        if not self.disableBilstm:
            self.run_bilstms(sentence,train)
        else:
            for root in sentence:
                root.vec = root.word_ext_vec

    def run_char_bilstm(self,root,train):
        if root.form != "*root*": # no point running a character analysis over this placeholder token
            self.charBuilders[0].set_dropout(0.33 if train else 0)
            self.charBuilders[1].set_dropout(0.33 if train else 0)
            forward  = self.charBuilders[0].initial_state()
            backward = self.charBuilders[1].initial_state()

            for char, charRev in zip(root.form, reversed(root.form)):
                forward = forward.add_input(self.clookup[self.chars.get(char,0)])
                backward = backward.add_input(self.clookup[self.chars.get(charRev,0)])

            root.chVec = dy.concatenate([forward.output(), backward.output()])
        else:
            root.chVec = self.chPadding.expr() # use the padding vector if it's the root token

    def run_bilstms(self,sentence,train):
        self.surfaceBuilders[0].set_dropout(0.33 if train else 0)
        self.surfaceBuilders[1].set_dropout(0.33 if train else 0)
        forward  = self.surfaceBuilders[0].initial_state()
        backward = self.surfaceBuilders[1].initial_state()

        for froot, rroot in zip(sentence, reversed(sentence)):
            forward = forward.add_input( froot.word_ext_vec )
            backward = backward.add_input( rroot.word_ext_vec )
            froot.fvec = forward.output()
            rroot.bvec = backward.output()

        for root in sentence:
            root.vec = dy.concatenate( [root.fvec, root.bvec] )

        self.bsurfaceBuilders[0].set_dropout(0.33 if train else 0)
        self.bsurfaceBuilders[1].set_dropout(0.33 if train else 0)
        bforward  = self.bsurfaceBuilders[0].initial_state()
        bbackward = self.bsurfaceBuilders[1].initial_state()

        for froot, rroot in zip(sentence, reversed(sentence)):
            bforward = bforward.add_input(froot.vec)
            bbackward = bbackward.add_input( rroot.vec)
            froot.bfvec = bforward.output()
            rroot.bbvec = bbackward.output()

        for root in sentence:
            root.vec = dy.concatenate( [root.bfvec, root.bbvec] )

    def apply_transition(self,best,stack,buf,hoffset):
        if best[1] == 2:
            #SHIFT
            stack.roots.append(buf.roots[0])
            del buf.roots[0]

        elif best[1] == 3:
            #SWAP
            child = stack.roots.pop()
            buf.roots.insert(1,child)

        elif best[1] == 0:
            #LEFT-ARC
            child = stack.roots.pop()
            parent = buf.roots[0]

            #predict rel and label
            child.pred_parent_id = parent.id
            child.pred_relation = best[0]

        elif best[1] == 1:
            #RIGHT-ARC
            child = stack.roots.pop()
            parent = stack.roots[-1]

            child.pred_parent_id = parent.id
            child.pred_relation = best[0]

        #update the representation of head for attaching transitions
        if best[1] == 0 or best[1] == 1:
            #linear order #not really - more like the deepest
            if self.rlMostFlag:
                parent.lstms[best[1] + hoffset] = child.lstms[best[1] + hoffset]
                #actual children
            if self.rlFlag:
                parent.lstms[best[1] + hoffset] = child.vec

    def calculate_cost(self,scores,s0,s1,b,beta,stack_ids):
        if len(scores[0]) == 0:
            left_cost = 1
        else:
            left_cost = len(s0[0].rdeps) + int(s0[0].parent_id != b[0].id and s0[0].id in s0[0].parent_entry.rdeps)


        if len(scores[1]) == 0:
            right_cost = 1
        else:
            right_cost = len(s0[0].rdeps) + int(s0[0].parent_id != s1[0].id and s0[0].id in s0[0].parent_entry.rdeps)


        if len(scores[2]) == 0:
            shift_cost = 1
            shift_case = 0
        elif len([item for item in beta if item.projective_order < b[0].projective_order and item.id > b[0].id ])> 0:
            shift_cost = 0
            shift_case = 1
        else:
            shift_cost = len([d for d in b[0].rdeps if d in stack_ids]) + int(len(s0)>0 and b[0].parent_id in stack_ids[:-1] and b[0].id in b[0].parent_entry.rdeps)
            shift_case = 2


        if len(scores[3]) == 0 :
            swap_cost = 1
        elif s0[0].projective_order > b[0].projective_order:
            swap_cost = 0
            #disable all the others
            left_cost = right_cost = shift_cost = 1
        else:
            swap_cost = 1

        costs = (left_cost, right_cost, shift_cost, swap_cost,1)
        return costs,shift_case


    def Predict(self, data):
        reached_max_swap = 0
        for iSentence, sentence in enumerate(data,1):
            sentence = deepcopy(sentence)
            reached_swap_for_i_sentence = False
            max_swap = 2*len(sentence)
            iSwap = 0
            self.Init()
            conll_sentence = [entry for entry in sentence if isinstance(entry, utils.ConllEntry)]
            conll_sentence = conll_sentence[1:] + [conll_sentence[0]]
            self.getWordEmbeddings(conll_sentence, False)
            stack = ParseForest([])
            buf = ParseForest(conll_sentence)

            hoffset = 1 if self.headFlag else 0

            for root in conll_sentence:
                root.lstms = [root.vec] if self.headFlag else []
                root.lstms += [self.paddingVec for _ in range(self.nnvecs - hoffset)]
                root.relation = root.relation if root.relation in self.rels else 'runk'


            while not (len(buf) == 1 and len(stack) == 0):
                scores = self.__evaluate(stack, buf, False)
                best = max(chain(*(scores if iSwap < max_swap else scores[:3] )), key = itemgetter(2) )
                if iSwap == max_swap and not reached_swap_for_i_sentence:
                    reached_max_swap += 1
                    reached_swap_for_i_sentence = True
                    print "reached max swap in %d out of %d sentences"%(reached_max_swap, iSentence)
                self.apply_transition(best,stack,buf,hoffset)
                if best[1] == 3:
                    iSwap += 1

            dy.renew_cg()
            yield sentence


    def Train(self, trainData):
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

        self.Init()

        #if self.debug:
        #    trainData = trainData[:200]

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
            self.getWordEmbeddings(conll_sentence, True)
            stack = ParseForest([])
            buf = ParseForest(conll_sentence)
            hoffset = 1 if self.headFlag else 0

            for root in conll_sentence:
                root.lstms = [root.vec] if self.headFlag else []
                root.lstms += [self.paddingVec for _ in range(self.nnvecs - hoffset)]
                root.relation = root.relation if root.relation in self.rels else 'runk'

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

                bestValid = list(( s for s in chain(*scores) if costs[s[1]] == 0 and ( s[1] == 2 or s[1] == 3 or  s[0] == s0[0].relation ) ))
                if len(bestValid) <1:
                    print "===============dropping a sentence==============="
                    break

                bestValid = max(bestValid, key=itemgetter(2))
                bestWrong = max(( s for s in chain(*scores) if costs[s[1]] != 0 or ( s[1] != 2 and s[1] != 3 and s[0] != s0[0].relation ) ), key=itemgetter(2))

                #force swap
                if costs[3]== 0:
                    best = bestValid
                else:
                    #select a transition to follow
                    # + aggresive exploration
                    #1: might want to experiment with that parameter
                    if bestWrong[1] == 3:
                        best = bestValid
                    else:
                        best = bestValid if ( (not self.oracle) or (bestValid[2] - bestWrong[2] > 1.0) or (bestValid[2] > bestWrong[2] and random.random() > 0.1) ) else bestWrong

                #updates for the dynamic oracle
                if best[1] == 2:
                    #SHIFT
                    if shift_case ==2:
                        if b[0].parent_entry.id in stack_ids[:-1] and b[0].id in b[0].parent_entry.rdeps:
                            b[0].parent_entry.rdeps.remove(b[0].id)
                        blocked_deps = [d for d in b[0].rdeps if d in stack_ids]
                        for d in blocked_deps:
                            b[0].rdeps.remove(d)

                elif best[1] == 0 or best[1] == 1:
                    #LA or RA
                    child = s0[0]
                    s0[0].rdeps = []
                    if s0[0].id in s0[0].parent_entry.rdeps:
                        s0[0].parent_entry.rdeps.remove(s0[0].id)

                self.apply_transition(best,stack,buf,hoffset)

                if bestValid[2] < bestWrong[2] + 1.0:
                    loss = bestWrong[3] - bestValid[3]
                    mloss += 1.0 + bestWrong[2] - bestValid[2]
                    eloss += 1.0 + bestWrong[2] - bestValid[2]
                    errs.append(loss)

                #labeled errors
                if best[1] != 2 and best[1] !=3 and (child.pred_parent_id != child.parent_id or child.pred_relation != child.relation):
                    lerrors += 1
                    #attachment error
                    if child.pred_parent_id != child.parent_id:
                        eerrors += 1

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
                self.Init()

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
