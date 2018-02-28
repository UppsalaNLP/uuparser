import dynet as dy
import random, codecs

class FeatureExtractor(object):
    def __init__(self,model,words,rels,langs,w2i,ch,nnvecs,options):
        self.model = model
        self.lstm_output_size = options.lstm_output_size
        self.char_lstm_output_size = options.char_lstm_output_size
        self.word_emb_size = options.word_emb_size
        self.char_emb_size = options.char_emb_size
        self.lang_emb_size = options.lang_emb_size
        self.wordsCount = words
        self.multiling = options.multiling and options.use_lembed
        self.vocab = {word: ind+2 for word, ind in w2i.iteritems()} # +2 for MLP padding vector and OOV vector
        self.chars = {char: ind+1 for ind, char in enumerate(ch)} # +1 for OOV vector
        self.rels = {word: ind for ind, word in enumerate(rels)}
        #ugly
        self.external_embedding = options.external_embedding
        self.nnvecs=nnvecs
        if langs:
            self.langs = {lang: ind+1 for ind, lang in enumerate(langs)} # +1 for padding vector
        else:
            self.langs = None
        self.irels = rels

        self.external_embedding = None
        if options.external_embedding is not None:
            self.get_external_embeddings(options.external_embedding)

        lstm_input_size = self.word_emb_size + (self.edim if self.external_embedding is\
                             not None else 0) + (self.lang_emb_size if
                                                 self.multiling else 0) + 2 * self.char_lstm_output_size

        #TODO: readd poss to do one or none
        self.surfaceBuilders = [dy.VanillaLSTMBuilder(1, lstm_input_size, self.lstm_output_size, self.model),
                                dy.VanillaLSTMBuilder(1, lstm_input_size, self.lstm_output_size, self.model)]
        self.bsurfaceBuilders = [dy.VanillaLSTMBuilder(1, 2* self.lstm_output_size,
                                                       self.lstm_output_size , self.model),
                                 dy.VanillaLSTMBuilder(1, 2* self.lstm_output_size,
                                                       self.lstm_output_size , self.model)]


        self.charBuilders = [dy.VanillaLSTMBuilder(1, self.char_emb_size, self.char_lstm_output_size, self.model),
                             dy.VanillaLSTMBuilder(1, self.char_emb_size, self.char_lstm_output_size, self.model)]

        #used in the PaddingVec
        #TODO: UGLY AND HACKY TO HAVE THIS HERE
        self.word2lstm = self.model.add_parameters((self.lstm_output_size * 2, lstm_input_size))
        self.word2lstmbias = self.model.add_parameters((self.lstm_output_size *2))
        self.chPadding = self.model.add_parameters((self.char_lstm_output_size *2))

        self.clookup = self.model.add_lookup_parameters((len(ch) + 1, self.char_emb_size))
        self.wlookup = self.model.add_lookup_parameters((len(words) + 2, self.word_emb_size))

        if self.multiling and self.lang_emb_size > 0:
            self.langslookup = self.model.add_lookup_parameters((len(langs) + 1, self.lang_emb_size))


    def Init(self):
        evec = self.elookup[1] if self.external_embedding is not None else None
        paddingWordVec = self.wlookup[1]
        paddingLangVec = self.langslookup[0] if self.multiling and self.lang_emb_size > 0 else None

        self.paddingVec = dy.tanh(self.word2lstm.expr() * dy.concatenate(filter(None,
                                                                          [paddingWordVec,
                                                                           evec,
                                                                           self.chPadding.expr(),
                                                                           paddingLangVec]))
                                  + self.word2lstmbias.expr() )
        self.empty = self.paddingVec if self.nnvecs == 1 else dy.concatenate([self.paddingVec for _ in xrange(self.nnvecs)])

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
                root.langvec = self.langslookup[self.langs[root.language_id]] if self.lang_emb_size > 0 else None
            else:
                root.langvec = None
            root.vec = dy.concatenate(filter(None, [root.wordvec,
                                                    root.evec,
                                                    root.chVec,
                                                    root.langvec]))
        self.run_bilstm(self.surfaceBuilders, sentence,train)
        self.run_bilstm(self.bsurfaceBuilders, sentence,train)

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


    def run_bilstm(self,surfaceBuilders, sentence,train):
        surfaceBuilders[0].set_dropout(0.33 if train else 0)
        surfaceBuilders[1].set_dropout(0.33 if train else 0)
        forward  = surfaceBuilders[0].initial_state()
        backward = surfaceBuilders[1].initial_state()

        for froot, rroot in zip(sentence, reversed(sentence)):
            forward = forward.add_input( froot.vec )
            backward = backward.add_input( rroot.vec )
            froot.fvec = forward.output()
            rroot.bvec = backward.output()

        for root in sentence:
            root.vec = dy.concatenate( [root.fvec, root.bvec] )

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
