from optparse import OptionParser
from arc_hybrid import ArcHybridLSTM
from options_manager import OptionsManager
import pickle, utils, os, time, sys, copy, itertools, re, random
from shutil import copyfile
import codecs

def run(om,options,i):

    if options.multiling:
        outdir = options.outdir
    else:
        cur_treebank = om.languages[i]
        outdir = cur_treebank.outdir

    if options.shared_task:
        outdir = options.shared_task_outdir

    if not options.predictFlag: # training

        print 'Preparing vocab'
        if options.multiling:
            words, w2i, pos, cpos, rels, langs, ch = utils.vocab(om.languages, path_is_dir=True)

        else:
            words, w2i, pos, cpos, rels, langs, ch = utils.vocab(cur_treebank.trainfile)

        paramsfile = os.path.join(outdir, options.params)
        with open(paramsfile, 'w') as paramsfp:
            print 'Saving params to ' + paramsfile
            pickle.dump((words, w2i, pos, rels, cpos, langs,
                         options, ch), paramsfp)
            print 'Finished collecting vocab'

        print 'Initializing blstm arc hybrid:'
        parser = ArcHybridLSTM(words, pos, rels, cpos, langs, w2i,
                               ch, options)

        for epoch in xrange(options.first_epoch, options.first_epoch+options.epochs):

            print 'Starting epoch ' + str(epoch)

            if options.multiling:
                traindata = list(utils.read_conll_dir(om.languages, "train", options.maxCorpus))
            else:
                traindata = list(utils.read_conll(cur_treebank.trainfile, cur_treebank.iso_id,options.maxCorpus))

            parser.Train(traindata)
            print 'Finished epoch ' + str(epoch)

            model_file = os.path.join(outdir, options.model + str(epoch))
            parser.Save(model_file)

            if options.pred_dev: # use the model to predict on dev data

                if options.multiling:
                    pred_langs = [lang for lang in om.languages if lang.pred_dev] # languages which have dev data on which to predict
                    for lang in pred_langs:
                        lang.outfilename = os.path.join(lang.outdir, 'dev_epoch_' + str(epoch) + '.conllu')
                        print "Predicting on dev data for " + lang.name
                    devdata = utils.read_conll_dir(pred_langs,"dev")
                    pred = list(parser.Predict(devdata))
                    if len(pred)>0:
                        utils.write_conll_multiling(pred,pred_langs)
                    else:
                        print "Warning: prediction empty"
                    if options.pred_eval:
                        for lang in pred_langs:
                            print "Evaluating dev prediction for " + lang.name
                            utils.evaluate(lang.dev_gold,lang.outfilename,om.conllu)
                else: # monolingual case
                    if cur_treebank.pred_dev:
                        print "Predicting on dev data for " + cur_treebank.name
                        devdata = utils.read_conll(cur_treebank.devfile, cur_treebank.iso_id)
                        cur_treebank.outfilename = os.path.join(outdir, 'dev_epoch_' + str(epoch) + ('.conll' if not om.conllu else '.conllu'))
                        pred = list(parser.Predict(devdata))
                        utils.write_conll(cur_treebank.outfilename, pred)
                        if options.pred_eval:
                            print "Evaluating dev prediction for " + cur_treebank.name
                            utils.evaluate(cur_treebank.dev_gold,cur_treebank.outfilename,om.conllu)
                            if options.model_selection:
                                score = utils.get_LAS_score(cur_treebank.outfilename + '.txt')
                                if score > cur_treebank.dev_best[1]:
                                    cur_treebank.dev_best = [epoch,score]

            if epoch == options.epochs: # at the last epoch choose which model to copy to barchybrid.model
                if not options.model_selection:
                    best_epoch = options.epochs # take the final epoch if model selection off completely (for example multilingual case)
                else:
                    best_epoch = cur_treebank.dev_best[0] # will be final epoch by default if model selection not on for this treebank
                    if cur_treebank.model_selection:
                        print "Best dev score of " + str(cur_treebank.dev_best[1]) + " found at epoch " + str(cur_treebank.dev_best[0])

                bestmodel_file = os.path.join(outdir,"barchybrid.model" + str(best_epoch))
                model_file = os.path.join(outdir,"barchybrid.model")
                print "Copying " + bestmodel_file + " to " + model_file
                copyfile(bestmodel_file,model_file)

    else: #if predict - so

        if options.multiling:
            modeldir = options.modeldir
        else:
            modeldir = om.languages[i].modeldir

        params = os.path.join(modeldir,options.params)
        print 'Reading params from ' + params
        with open(params, 'r') as paramsfp:
            words, w2i, pos,rels, cpos,langs, stored_opt,  ch= pickle.load(paramsfp)

            parser = ArcHybridLSTM(words, pos, rels, cpos, langs, w2i,
                               ch, stored_opt)
            model = os.path.join(modeldir, options.model)
            parser.Load(model)

            if options.multiling:
                testdata = utils.read_conll_dir(om.languages,"test")
            else:
                testdata = utils.read_conll(cur_treebank.testfile,cur_treebank.iso_id)

            ts = time.time()

            if options.multiling:
                for l in om.languages:
                    l.outfilename = os.path.join(outdir, l.outfilename)
                pred = list(parser.Predict(testdata))
                utils.write_conll_multiling(pred,om.languages)
            else:
                if cur_treebank.outfilename:
                    cur_treebank.outfilename = os.path.join(outdir,cur_treebank.outfilename)
                else:
                    cur_treebank.outfilename = os.path.join(outdir, 'out' + ('.conll' if not om.conllu else '.conllu'))
                utils.write_conll(cur_treebank.outfilename, parser.Predict(testdata))

            te = time.time()

            if options.pred_eval:
                if options.multiling:
                    for l in om.languages:
                        print "Evaluating on " + l.name
                        score = utils.evaluate(l.test_gold,l.outfilename,om.conllu)
                        print "Obtained LAS F1 score of %.2f on %s" %(score,l.name)
                else:
                    print "Evaluating on " + cur_treebank.name
                    score = utils.evaluate(cur_treebank.test_gold,cur_treebank.outfilename,om.conllu)
                    print "Obtained LAS F1 score of %.2f on %s" %(score,cur_treebank.name)

            print 'Finished predicting'

if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("--trainfile", dest="trainfile", help="Annotated CONLL(U) train file", metavar="FILE")
    parser.add_option("--devfile", dest="devfile", help="Annotated CONLL(U) dev file", metavar="FILE")
    parser.add_option("--testfile", dest="testfile", help="Annotated CONLL(U) test file", metavar="FILE")
    parser.add_option("--params", dest="params", help="Parameters file", metavar="FILE", default="params.pickle")
    parser.add_option("--extrn", dest="external_embedding", help="External embeddings", metavar="FILE")
    parser.add_option("--model", dest="model", help="Load/Save model file", metavar="FILE", default="barchybrid.model")
    parser.add_option("--wembedding", help="word embedding dimension", type="int", dest="wembedding_dims", default=100)
    parser.add_option("--lstmdims", help="dimension of the lstm", type="int", dest="lstm_dims", default=125)
    parser.add_option("--lembedding", help="dimension of the language\
                      embeddings", type="int", dest="lembedding_dims", default=12)
    parser.add_option("--cembedding", help="dimension of the character\
                      embedding", type="int", dest="cembedding_dims", default=12)
    parser.add_option("--chlstmdims", help="character LSTM dimension", type="int", dest="chlstm_dims", default=50)
    parser.add_option("--epochs", help='number of epochs', type="int", dest="epochs", default=30)
    parser.add_option("--hidden", help='hidden units of the MLP hidden layer', type="int", dest="hidden_units", default=100)
    parser.add_option("--hidden2", help='hidden units of the second layer of the MLP',  type="int", dest="hidden2_units", default=0)
    parser.add_option("--k", help='number of items in the stack fed to the MLP', type="int", dest="window", default=3)
    parser.add_option("--lr", help='learning rate', type="float", dest="learning_rate", default=0.001)
    parser.add_option("--outdir", help='output directory', type="string", dest="outdir", default="EXP")
    parser.add_option("--shared_task_outdir", type="string", dest="shared_task_outdir", default="EXP")
    parser.add_option("--modeldir", help='directory where models will be saved', type="string", dest="modelDir", default="EXP")
    parser.add_option("--activation", help='activation function in the MLP', type="string", dest="activation", default="tanh")
    parser.add_option("--dynet-seed", type="int", help="Random seed for Dynet", dest="dynet_seed", metavar="INTEGER")
    parser.add_option("--use-default-seed", action="store_true", dest="use_default_seed", help="Use default random seed for Python", default=False)
    parser.add_option("--disableoracle", help='use the static oracle instead of\
                      the dynamic oracle', action="store_false", dest="oracle", default=True)
    parser.add_option("--disable-head", help='disable using the head of word\
                      vectors fed to the MLP', action="store_false", dest="headFlag", default=True)
    parser.add_option("--disable-rlmost", help='disable using leftmost and\
                      rightmost dependents of words fed to the MLP', action="store_false", dest="rlMostFlag", default=True)
    parser.add_option("--userl", action="store_true", dest="rlFlag", default=False)
    parser.add_option("--predict", help='parse', action="store_true", dest="predictFlag", default=False)
    parser.add_option("--dynet-mem", type="int", dest="cnn_mem", default=512)
    parser.add_option("--disablebilstm", help='disable the BiLSTM feature\
                      extactor', action="store_true", dest="disableBilstm", default=False)
    parser.add_option("--disable-pred-eval", action="store_false", dest="pred_eval", default=True)
    parser.add_option("--disable-pred-dev", action="store_false", dest="pred_dev", default=True)
    parser.add_option("--multiling", help='train a multilingual parser with\
                      language embeddings', action="store_true", dest="multiling", default=False)
    parser.add_option("--datadir", help='input directory with train/dev/test\
                      files', dest="datadir", default=None)
    parser.add_option("--include", dest="include", default =None,\
                        help="The languages to be run if using UD - None\
                        by default - if None - need to specify dev,train,test.\
                      \n Used in combination with multiling: trains a common \
                      parser for all languages. Otherwise, train monolingual \
                      parsers for each")
    #TODO: reenable this
    parser.add_option("--continue", dest="continueTraining", action="store_true", default=False)
    parser.add_option("--continueModel", dest="continueModel", help="Load model file, \
                      when continuing to train a previously trained model", metavar="FILE", default=None)
    parser.add_option("--first-epoch", type="int", dest="first_epoch", default=1)
    parser.add_option("--debug", action="store_true", dest="debug", default=False)
    parser.add_option("--debug-train-sents", type="int", dest="debug_train_sents", default=150)
    parser.add_option("--debug-dev-sents", type="int", dest="debug_dev_sents", default=100)
    parser.add_option("--debug-test-sents", type="int", dest="debug_test_sents", default=50)
    parser.add_option("--shared_task", action="store_true", dest="shared_task", default=False)
    parser.add_option("--metadata_file", dest="metadataF", default='src/utils/metadata.json')
    parser.add_option("--shared_task_datadir", type="string", dest="shared_task_datadir", default="EXP")
    parser.add_option("--max-sentences", help='only train using n sentences', type="int", dest="maxCorpus", default=-1)
    parser.add_option("--create-dev", help='create dev data if no dev file is\
                      provided', action="store_true", dest="create_dev", default=False)
    parser.add_option("--min-train-sents", help='minimum number of training\
                      sentences required in order to create a dev file', type="int", dest="min_train_sents", default=1000)
    parser.add_option("--dev-percent", help='percentage of training data to use\
                      as dev data', type="float", dest="dev_percent", default=5)

    parser.add_option("--disable-model-selection", action="store_false", dest="model_selection", default=True)

    (options, args) = parser.parse_args()

    # really important to do this before anything else to make experiments reproducible
    utils.set_seeds(options)

    om = OptionsManager(options)
    for i in range(om.iterations):
        run(om,options,i)
