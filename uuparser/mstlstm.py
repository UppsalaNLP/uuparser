from operator import itemgetter
import time, random, decoder
from chuliu_edmonds import chuliu_edmonds_one_root
import numpy as np
from multilayer_perceptron import biMLP
from collections import defaultdict
from copy import deepcopy

from uuparser import utils

class MSTParserLSTM:
    def __init__(self, vocab, options):
        import dynet as dy
        from feature_extractor import FeatureExtractor
        global dy
        self.model = dy.ParameterCollection()
        self.trainer = dy.AdamTrainer(self.model, alpha=options.learning_rate)
        self.activations = {'tanh': dy.tanh, 'sigmoid': dy.logistic, 'relu':
                            dy.rectify, 'tanh3': (lambda x:
                                                  dy.tanh(dy.cwise_multiply(dy.cwise_multiply(x, x), x)))}
        self.activation = self.activations[options.activation]
        self.costaugFlag = options.costaugFlag
        self.feature_extractor = FeatureExtractor(self.model, options, vocab)
        self.labelsFlag=options.labelsFlag
        mlp_in_dims = options.lstm_output_size*2

        self.unlabeled_MLP = biMLP(self.model, mlp_in_dims, options.mlp_hidden_dims,
                                 options.mlp_hidden2_dims, 1, self.activation)
        if self.labelsFlag:
            self.labeled_MLP = biMLP(self.model, mlp_in_dims, options.mlp_hidden_dims,
                               options.mlp_hidden2_dims,len(self.feature_extractor.irels),self.activation)

        self.proj = options.proj


    def  __getExpr(self, sentence, i, j, train):
        output = self.unlabeled_MLP(sentence[i].vec, sentence[j].vec)
        return output


    def __evaluate(self, sentence, train):
        exprs = [ [self.__getExpr(sentence, i, j, train) for j in range(len(sentence))] for i in range(len(sentence)) ]
        scores = np.array([ [output.scalar_value() for output in exprsRow] for exprsRow in exprs ])
        return scores, exprs


    def __evaluateLabel(self, sentence, i, j):
        output = self.labeled_MLP(sentence[i].vec, sentence[j].vec)
        return output.value(), output


    def Save(self, filename):
        self.model.save(filename)


    def Load(self, filename):
        self.model.populate(filename)


    def Predict(self, treebanks, datasplit, options):
        char_map = {}
        if options.char_map_file:
            char_map_fh = open(options.char_map_file,encoding='utf-8')
            char_map = json.loads(char_map_fh.read())
        # should probably use a namedtuple in get_vocab to make this prettier
        _, test_words, test_chars, _, _, _, test_treebanks, test_langs = utils.get_vocab(treebanks,datasplit,char_map)

        # get external embeddings for the set of words and chars in the
        # test vocab but not in the training vocab
        test_embeddings = defaultdict(lambda: {})
        if options.word_emb_size > 0 and options.ext_word_emb_file:
            new_test_words = \
                    set(test_words) - self.feature_extractor.words.keys()

            print("Number of OOV word types at test time: %i (out of %i)" % (
                len(new_test_words), len(test_words)))

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
                        print("External embeddings found for %i words "\
                                "(out of %i)" % \
                                (len(test_embeddings["words"]), len(new_test_words)))

        if options.char_emb_size > 0:
            new_test_chars = \
                    set(test_chars) - self.feature_extractor.chars.keys()
            print("Number of OOV char types at test time: %i (out of %i)" % (
                len(new_test_chars), len(test_chars)))

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
                        print("External embeddings found for %i chars "\
                                "(out of %i)" % \
                                (len(test_embeddings["chars"]), len(new_test_chars)))

        data = utils.read_conll_dir(treebanks,datasplit,char_map=char_map)
        for iSentence, osentence in enumerate(data,1):
            sentence = deepcopy(osentence)
            self.feature_extractor.Init(options)
            conll_sentence = [entry for entry in sentence if isinstance(entry, utils.ConllEntry)]
            self.feature_extractor.getWordEmbeddings(conll_sentence, False, options, test_embeddings)

            scores, exprs = self.__evaluate(conll_sentence, True)
            if self.proj:
                heads = decoder.parse_proj(scores)
                #LATTICE solution to multiple roots
                # see https://github.com/jujbob/multilingual-bist-parser/blob/master/bist-parser/bmstparser/src/mstlstm.py
                ## ADD for handling multi-roots problem
                rootHead = [head for head in heads if head==0]
                if len(rootHead) != 1:
                    print("it has multi-root, changing it for heading first root for other roots")
                    rootHead = [seq for seq, head in enumerate(heads) if head == 0]
                    for seq in rootHead[1:]:heads[seq] = rootHead[0]
                ## finish to multi-roots

            else:
                heads = chuliu_edmonds_one_root(scores.T)

            for entry, head in zip(conll_sentence, heads):
                entry.pred_parent_id = head
                entry.pred_relation = '_'

            if self.labelsFlag:
                for modifier, head in enumerate(heads[1:]):
                    scores, exprs = self.__evaluateLabel(conll_sentence, head, modifier+1)
                    conll_sentence[modifier+1].pred_relation = self.feature_extractor.irels[max(enumerate(scores), key=itemgetter(1))[0]]

            dy.renew_cg()

            #keep in memory the information we need, not all the vectors
            oconll_sentence = [entry for entry in osentence if isinstance(entry, utils.ConllEntry)]
            for tok_o, tok in zip(oconll_sentence, conll_sentence):
                tok_o.pred_relation = tok.pred_relation
                tok_o.pred_parent_id = tok.pred_parent_id
            yield osentence

    def Train(self, trainData, options):
        errors = 0
        batch = 0
        eloss = 0.0
        mloss = 0.0
        eerrors = 0
        lerrors = 0
        etotal = 0
        beg = start = time.time()

        random.shuffle(trainData) # in certain cases the data will already have been shuffled after being read from file or while creating dev data

        errs = []
        lerrs = []
        eeloss = 0.0
        self.feature_extractor.Init(options)

        for iSentence, sentence in enumerate(trainData,1):
            if iSentence % 100 == 0 and iSentence != 0:
                loss_message = 'Processing sentence number: %d'%iSentence + \
                        ' Loss: %.3f'%(eloss / etotal)+ \
                        ' Errors: %.3f'%((float(eerrors)) / etotal)+\
                        ' Labeled Errors: %.3f'%(float(lerrors) / etotal)+\
                        ' Time: %.2gs'%(time.time()-start)
                print(loss_message)
                start = time.time()
                eerrors = 0
                eloss = 0.0
                etotal = 0
                lerrors = 0
                ltotal = 0

            conll_sentence = [entry for entry in sentence if isinstance(entry, utils.ConllEntry)]
            self.feature_extractor.getWordEmbeddings(conll_sentence, True, options)

            scores, exprs = self.__evaluate(conll_sentence, True)
            gold = [entry.parent_id for entry in conll_sentence]
            if self.proj:
                heads = decoder.parse_proj(scores, gold if self.costaugFlag else None)
            else:
                if self.costaugFlag:
                    #augment the score of non-gold arcs
                    for i in range(len(scores)):
                        for j in range(len(scores)):
                            if gold[j] != i:
                                scores[i][j] += 1.
                heads = chuliu_edmonds_one_root(scores.T)
                heads[0] = -1

            if self.labelsFlag:
                for modifier, head in enumerate(gold[1:]):
                    rscores, rexprs = self.__evaluateLabel(conll_sentence, head, modifier+1)
                    goldLabelInd = self.feature_extractor.rels[conll_sentence[modifier+1].relation]
                    wrongLabelInd = max(((l, scr) for l, scr in enumerate(rscores) if l != goldLabelInd), key=itemgetter(1))[0]
                    if rscores[goldLabelInd] < rscores[wrongLabelInd] + 1:
                        lerrs.append(rexprs[wrongLabelInd] - rexprs[goldLabelInd])
                        lerrors += 1 #not quite right but gives some indication

            e = sum([1 for h, g in zip(heads[1:], gold[1:]) if h != g])
            eerrors += e
            if e > 0:
                loss = [(exprs[h][i] - exprs[g][i]) for i, (h,g) in enumerate(zip(heads, gold)) if h != g]
                eloss += dy.esum(loss).scalar_value()
                mloss += dy.esum(loss).scalar_value()
                errs.extend(loss)

            etotal += len(conll_sentence)

            if iSentence % 1 == 0 or len(errs) > 0 or len(lerrs) > 0:
                eeloss = 0.0

                if len(errs) > 0 or len(lerrs) > 0:
                    eerrs = (dy.esum(errs + lerrs))
                    eerrs.scalar_value()
                    eerrs.backward()
                    self.trainer.update()
                    errs = []
                    lerrs = []

                dy.renew_cg()

        if len(errs) > 0:
            eerrs = (dy.esum(errs + lerrs))
            eerrs.scalar_value()
            eerrs.backward()
            self.trainer.update()

            errs = []
            lerrs = []
            eeloss = 0.0

            dy.renew_cg()

        self.trainer.update()
        print("Loss: ", mloss/iSentence)
        print("Total Training Time: %.2gs"%(time.time()-beg))
