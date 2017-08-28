from optparse import OptionParser
from arc_hybrid import ArcHybridLSTM
from options_manager import OptionsManager
import pickle, utils, os, time, sys, copy, itertools

def run(om,options,i):
    outdir = options.output
    if options.multi_monoling:
        cur_treebank = om.languages[i]
        outdir = cur_treebank.outdir
        modelDir = cur_treebank.modelDir
    else:
        outdir = options.output
        modelDir = om.languages[i].modelDir

    if options.shared_task:
        outdir = options.shared_task_outdir

    if not options.include:
        cur_treebank = om.treebank

    if not options.predictFlag:

        print 'Preparing vocab'
        if options.multiling:
            words, w2i, pos, cpos, rels, langs, ch= utils.vocab(om.languages,path_is_dir=True)

        else:
            words, w2i, pos, cpos, rels, langs, ch= utils.vocab(cur_treebank.trainfile)

        with open(os.path.join(outdir, options.params), 'w') as paramsfp:
            pickle.dump((words, w2i, pos, rels, cpos, langs,
                         options, ch), paramsfp)
            print 'Finished collecting vocab'

        print 'Initializing blstm arc hybrid:'
        parser = ArcHybridLSTM(words, pos, rels, cpos, langs, w2i,
                               ch, options)

        for epoch in xrange(options.first_epoch-1, options.first_epoch-1+options.epochs):
            if options.multiling:
                traindata=list(utils.read_conll_dir(om.languages, "train", options.drop_proj, options.maxCorpus))
                devdata=enumerate(utils.read_conll_dir(om.languages,"dev"))

            else:
                conllFP=open(cur_treebank.trainfile, 'r')
                traindata = list(utils.read_conll(conllFP, options.drop_proj,
                                                  cur_treebank.iso_id))
                if os.path.exists(cur_treebank.devfile):
                    conllFP = open(cur_treebank.devfile, 'r')
                    devdata = enumerate(utils.read_conll(conllFP, False,
                                                         cur_treebank.iso_id))
                else:
                    tot_sen = len(traindata)
                    #take a bit less than 5% of train sentences for dev
                    if tot_sen > 1000:
                        import random
                        random.shuffle(traindata)
                        dev_len = int(0.05*tot_sen)
                        #gen object * 2
                        devdata, dev_gold = itertools.tee(traindata[:dev_len])
                        devdata = enumerate(devdata)
                        dev_gold_f = os.path.join(outdir,'dev_gold' + '.conllu')
                        utils.write_conll(dev_gold_f,dev_gold)
                        cur_treebank.dev_gold = dev_gold_f
                        traindata = traindata[dev_len:]
                    else:
                        devdata = None

            print 'Starting epoch', epoch
            parser.Train(traindata)

            if options.multiling:
                for l in om.languages:
                    l.outfilename = os.path.join(l.outdir, 'dev_epoch_' +
                                                 str(epoch+1) + '.conllu')
                pred = list(parser.Predict(devdata))
                if len(pred)>0:
                    utils.write_conll_multiling(pred,om.languages)
            else:
                cur_treebank.outfilename = os.path.join(outdir, 'dev_epoch_' + str(epoch+1) + ('.conll' if not om.conllu else '.conllu'))
                if devdata:
                    pred = list(parser.Predict(devdata))
                    utils.write_conll(cur_treebank.outfilename, pred)


            if options.multiling:
                for l in om.languages:
                    utils.evaluate(l.dev_gold,l.outfilename,om.conllu)
            else:
                utils.evaluate(cur_treebank.dev_gold,cur_treebank.outfilename,om.conllu)

            print 'Finished predicting dev'
            parser.Save(os.path.join(outdir, options.model + str(epoch+1)))

    else: #if predict - so
        params = os.path.join(modelDir,options.params)
        with open(params, 'r') as paramsfp:
            words, w2i, pos,rels, cpos,langs, stored_opt,  ch= pickle.load(paramsfp)

            parser = ArcHybridLSTM(words, pos, rels, cpos, langs, w2i,
                               ch, stored_opt)
            model = os.path.join(modelDir, options.model)
            parser.Load(model)

            if options.multiling:
                testdata=enumerate(utils.read_conll_dir(om.languages,"test"))

            if not options.multiling:
                conllFP = open(cur_treebank.testfile, 'r')
                testdata = enumerate(utils.read_conll(conllFP, False,
                                                      cur_treebank.iso_id))

            ts = time.time()

            if options.multiling:
                for l in om.languages:
                    l.outfilename = os.path.join(outdir, l.outfilename)
                pred = list(parser.Predict(testdata))
                utils.write_conll_multiling(pred,om.languages)
            else:
                cur_treebank.outfilename = os.path.join(outdir,cur_treebank.outfilename)
                utils.write_conll(cur_treebank.outfilename, parser.Predict(testdata))

            te = time.time()


            if options.predEval:
                if options.multiling:
                    for l in om.languages:
                        utils.evaluate(l.test_gold,l.outfilename,om.conllu)
                else:
                    utils.evaluate(cur_treebank.test_gold,cur_treebank.outfilename,om.conllu)

            print 'Finished predicting test',te-ts

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("--train", dest="conll_train", help="Annotated CONLL train file", metavar="FILE", default="../data/PTB_SD_3_3_0/train.conll")
    parser.add_option("--dev", dest="conll_dev", help="Annotated CONLL dev file", metavar="FILE", default="../data/PTB_SD_3_3_0/dev.conll")
    parser.add_option("--test", dest="conll_test", help="Annotated CONLL test file", metavar="FILE", default="../data/PTB_SD_3_3_0/test.conll")
    parser.add_option("--params", dest="params", help="Parameters file", metavar="FILE", default="params.pickle")
    parser.add_option("--extrn", dest="external_embedding", help="External embeddings", metavar="FILE")
    parser.add_option("--model", dest="model", help="Load/Save model file", metavar="FILE", default="barchybrid.model")
    parser.add_option("--wembedding", type="int", dest="wembedding_dims", default=100)
    parser.add_option("--pembedding", type="int", dest="pembedding_dims", default=20)
    parser.add_option("--rembedding", type="int", dest="rembedding_dims", default=15)
    parser.add_option("--lstmdims", type="int", dest="lstm_dims", default=100)
    parser.add_option("--lembedding", type="int", dest="lembedding_dims", default=12)
    parser.add_option("--cembedding", type="int", dest="cembedding_dims", default=12)
    parser.add_option("--chlstmdims", type="int", dest="chlstm_dims", default=50)
    parser.add_option("--epochs", type="int", dest="epochs", default=30)
    parser.add_option("--hidden", type="int", dest="hidden_units", default=100)
    parser.add_option("--hidden2", type="int", dest="hidden2_units", default=0)
    parser.add_option("--k", type="int", dest="window", default=3)
    parser.add_option("--lr", type="float", dest="learning_rate", default=0.001)
    parser.add_option("--outdir", type="string", dest="output", default="EXP")
    parser.add_option("--shared_task_outdir", type="string", dest="shared_task_outdir", default="EXP")
    parser.add_option("--modeldir", type="string", dest="modelDir", default="EXP")
    parser.add_option("--activation", type="string", dest="activation", default="tanh")
    parser.add_option("--dynet-seed", type="int", dest="seed", default=7)
    parser.add_option("--disableoracle", action="store_false", dest="oracle", default=True)
    parser.add_option("--usehead", action="store_true", dest="headFlag", default=False)
    #bug that they are swapped
    parser.add_option("--userlmost", action="store_true", dest="rlFlag", default=False)
    parser.add_option("--userl", action="store_true", dest="rlMostFlag", default=False)
    parser.add_option("--predict", action="store_true", dest="predictFlag", default=False)
    parser.add_option("--dynet-mem", type="int", dest="cnn_mem", default=512)
    parser.add_option("--disablePredEval", action="store_false", dest="predEval", default=True)
    parser.add_option("--multiling", action="store_true", dest="multiling", default=False)
    parser.add_option("--datadir", dest="datadir", help="UD Dir -obligatory if\
                      using include", default=None)
    parser.add_option("--include", dest="include", default =None,\
                        help="The languages to be run if using UD - None\
                        by default - if None - need to specify dev,train,test.\
                      \n Used in combination with multiling: trains a common \
                      parser for all languages. Otherwise, train monolingual \
                      parsers for each")
    parser.add_option("--continue", dest="continueTraining", action="store_true", default=False)
    parser.add_option("--continueModel", dest="continueModel", help="Load model file, when continuing to train a previously trained model", metavar="FILE", default=None)
    parser.add_option("--first-epoch", type="int", dest="first_epoch", default=1)
    parser.add_option("--debug", action="store_true", dest="debug", default=False)
    parser.add_option("--shared_task", action="store_true", dest="shared_task", default=False)
    parser.add_option("--metadata_file", dest="metadataF", default='src/utils/metadata.json')
    parser.add_option("--shared_task_datadir", type="string", dest="shared_task_datadir", default="EXP")
    parser.add_option("--max-sentences", type="int", dest="maxCorpus", default=-1)

    (options, args) = parser.parse_args()
    print 'Using external embedding:', options.external_embedding

    om = OptionsManager(options)
    for i in range(om.iterations):
        run(om,options,i)

