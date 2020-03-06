import os,sys,re
import copy
import random
from collections import namedtuple

from uuparser import utils

class OptionsManager(object):

    def __init__(self,options):
        """
        input: parser options
        object to harmonise the way we deal with the parser
        """

        # load these straight away to make sure they're always available
        #TODO: options compatibility TB vs GB
        utils.load_iso_dict(options.json_isos)
        utils.load_reverse_iso_dict(options.json_isos)

        if options.include:
            if not options.predict and not options.datadir:
                raise Exception("You need to specify --datadir")
            elif options.shared_task and not options.testdir:
                raise Exception("You need to specify --testdir")
            if options.predict and not (options.datadir or options.testdir or
                                        options.testfile):
                raise Exception("You need to specify --testdir")

        if not options.predict:
            if not options.include and not options.trainfile:
                raise Exception("If not using the --include option, you must specify your training data with --trainfile")
        else:
            if not options.include and not options.testfile:
                raise Exception("If not using the --include option, you must specify your test data with --testfile")
            if not options.modeldir:
                options.modeldir = options.outdir # set model directory to output directory by default
            model = os.path.join(options.modeldir,options.model)
            # in monoling case we check later on language by language basis
            if options.multiling and not os.path.exists(model):
                raise Exception("Model not found. Path tried: %s"%model)

        if not options.outdir:
            raise Exception("You must specify an output directory via the --outdir option")
        elif not os.path.exists(options.outdir): # create output directory if it doesn't exist
            print("Creating output directory " + options.outdir)
            os.mkdir(options.outdir)

        if not options.graph_based and (not options.predict and not
                                        (options.rlFlag or options.rlMostFlag or
                                         options.headFlag)):
            raise Exception("Must include either head, rl or rlmost (For example, if you specified --disable-head and --disable-rlmost, you must specify --userl)")

        if not options.graph_based and (options.rlFlag and options.rlMostFlag):
            print('Warning: Switching off rlMostFlag to allow rlFlag to take precedence')
            options.rlMostFlag = False

        if options.word_emb_size == 0 and options.pos_emb_size == 0 and\
            options.char_lstm_output_size == 0 and not options.external_embedding:
            raise Exception("All embeddings switched off: must use one of words, pos tags, chars, or external embeddings")

        if not options.multiling:
            options.tbank_emb_size = 0

        options.conllu = True #default


    def create_experiment_list(self,options):
        """
        Create a list of experiments.
        This list is designed to be looped over in the main body of our program.
        """
        experiment = namedtuple('Experiment','treebanks, outdir, modeldir')
        experiments = [] # will be a list of namedtuples

        if not options.include:
            treebanks = self.create_vanila_treebank_list(options)
            experiments.append(experiment(treebanks, options.outdir, options.modeldir))
        else:
            treebanks = self.create_UD_treebank_list(options)
            if options.multiling: # one experiment with several treebanks
                experiments.append(experiment(treebanks, options.outdir, options.modeldir))
            else: # several experiments with one treebank each
                for treebank in treebanks:
                    experiments.append(experiment([treebank],treebank.outdir,treebank.modeldir))

        return experiments

    def create_vanila_treebank_list(self,options):
        """
        Create list of vanilla (i.e. non-UD) treebanks. Currently only one treebank is supported, so the list will always
        have one element. This is for consistency with the UD treebanks case where multi-monlingual experiments are allowed
        """
        treebank = utils.Treebank(options.trainfile, \
                                       options.devfile, options.testfile)
        treebank.iso_id = None
        treebank.outdir = options.outdir
        treebank.modeldir = options.modeldir
        #just one model specified by train/dev and/or test
        if options.predict:
            if not os.path.exists(options.testfile):
                raise Exception("Test file " + options.testfile + " not found")
            else:
                options.conllu = (os.path.splitext(options.testfile.lower())[1] == '.conllu') # test if file in conllu format
                treebank.test_gold = options.testfile
        else:
            self.prepareDev(treebank,options)
            if options.devfile:
                options.conllu = (os.path.splitext(options.devfile.lower())[1] == '.conllu')
            elif options.create_dev:
                options.conllu = (os.path.splitext(options.trainfile.lower())[1] == '.conllu')

        if options.debug:
            self.createDebugData(treebank,options)

        return [treebank] # make it a list of one element just for the sake of consistency with the "include" case

    def create_UD_treebank_list(self,options):
        """
        Create list of UD Treebanks for experiments.
        Output will either be a list where each element is a single treebank (monolingual or multi-monolingual case)
        or a list where the first element is a list of treebanks (multilingual case).
        This makes it easier to loop over the outer list in our main parser function
        """
        options.conllu = True # file is in conllu format
        all_treebanks = utils.get_all_treebanks(options) # returns a UD treebank for all possible UD languages
        treebank_dict = {treebank.iso_id: treebank for treebank in all_treebanks}
        treebanks = [] # the treebanks we need
        iso_list = utils.parse_list_arg(options.include) # languages requested by the user via the include flag
        for iso in iso_list:
            proxy_tbank = None
            m = re.search(r'^(.*):(.*)$',iso)
            if m:
                iso = m.group(1)
                proxy_tbank = m.group(2)
            if iso in treebank_dict:
                treebank = treebank_dict[iso]
                treebank.proxy_tbank = proxy_tbank
                if not options.shared_task:
                    treebank.outdir= os.path.join(options.outdir,treebank.iso_id)
                else:
                    treebank.outdir = options.outdir
                if not os.path.exists(treebank.outdir): # create language-specific output folder if it doesn't exist
                    print("Creating language-specific output directory " + treebank.outdir)
                    os.mkdir(treebank.outdir)
                else:
                    print("Warning: language-specific subdirectory " + treebank.outdir
                        + " already exists, contents may be overwritten")

                if not options.predict:
                    self.prepareDev(treebank,options)

                if options.debug: # it is important that prepareDev be called before createDebugData
                    self.createDebugData(treebank,options)

                if options.predict and not options.multiling:
                    treebank.modeldir = os.path.join(options.modeldir,treebank.iso_id)
                    model = os.path.join(treebank.modeldir,options.model)
                    if not os.path.exists(model):
                        raise Exception("Model not found. Path tried: %s"%model)
                else:
                    treebank.modeldir = None

                treebanks.append(treebank)
            else:
                print("Warning: skipping invalid language code " + iso)

        return treebanks


    # creates dev data by siphoning off a portion of the training data (when necessary)
    # sets up treebank for prediction and model selection on dev data
    def prepareDev(self,treebank,options):
        treebank.pred_dev = options.pred_dev # even if options.pred_dev is True, might change treebank.pred_dev to False later if no dev data available
        if not treebank.devfile or not os.path.exists(treebank.devfile):
            if options.create_dev: # create some dev data from the training data
                train_data = list(utils.read_conll(treebank.trainfile))
                tot_sen = len(train_data)
                if tot_sen > options.min_train_sents: # need to have at least min_train_sents to move forward
                    dev_file = os.path.join(treebank.outdir,'dev-split' + '.conllu') # location for the new dev file
                    train_file = os.path.join(treebank.outdir,'train-split' + '.conllu') # location for the new train file
                    dev_len = int(0.01*options.dev_percent*tot_sen)
                    print("Taking " + str(dev_len) + " of " + str(tot_sen)
                            + " sentences from training data as new dev data for " + treebank.name)
                    random.shuffle(train_data)
                    dev_data = train_data[:dev_len]
                    utils.write_conll(dev_file,dev_data) # write the new dev data to file
                    train_data = train_data[dev_len:] # put the rest of the training data in a new file too
                    utils.write_conll(train_file,train_data)
                    # update some variables with the new file locations
                    treebank.dev_gold = dev_file
                    treebank.devfile = dev_file
                    treebank.trainfile = train_file
                else: # not enough sentences
                    print("Warning: not enough sentences in training data to create dev set for "
                        + treebank.name + " (minimum required --min-train-size: " + str(options.min_train_sents) + ")")
                    treebank.pred_dev = False
            else: # option --create-dev not set
                print("Warning: No dev data for " + treebank.name
                        + ", consider adding option --create-dev to create dev data from training set")
                treebank.pred_dev = False
        if options.model_selection and not treebank.pred_dev:
            print("Warning: can't do model selection for " + treebank.name + " as prediction on dev data is off")

    # if debug options is set, we read in the training, dev and test files as appropriate, cap the number of sentences and store
    # new files with these smaller data sets
    def createDebugData(self,treebank,options):
        ext = '.conllu' if options.conllu else '.conll'
        print('Creating smaller data sets for debugging')
        if not options.predict:
            train_data = list(utils.read_conll(treebank.trainfile,maxSize=options.debug_train_sents,hard_lim=True))
            train_file = os.path.join(treebank.outdir,'train-debug' + ext) # location for the new train file
            utils.write_conll(train_file,train_data) # write the new dev data to file
            treebank.trainfile = train_file
            if treebank.devfile and os.path.exists(treebank.devfile) and options.pred_dev:
                dev_data = list(utils.read_conll(treebank.devfile,maxSize=options.debug_dev_sents,hard_lim=True))
                dev_file = os.path.join(treebank.outdir,'dev-debug' + ext) # location for the new dev file
                utils.write_conll(dev_file,dev_data) # write the new dev data to file
                # have to create a separate debug gold file if not the same as input file
                if treebank.dev_gold != treebank.devfile:
                    dev_gold_data = list(utils.read_conll(treebank.dev_gold,maxSize=options.debug_dev_sents,hard_lim=True))
                    dev_gold_file = os.path.join(treebank.outdir,'dev-gold-debug' + ext) # location for the new dev file
                    utils.write_conll(dev_gold_file,dev_gold_data) # write the new dev gold data to file
                    treebank.dev_gold = dev_gold_file
                else:
                    treebank.dev_gold = dev_file
                treebank.devfile = dev_file # important to do this last
        else:
            test_data = list(utils.read_conll(treebank.testfile,maxSize=options.debug_test_sents,hard_lim=True))
            test_file = os.path.join(treebank.outdir,'test-debug' + ext) # location for the new dev file
            utils.write_conll(test_file,test_data) # write the new dev data to file
            if treebank.test_gold != treebank.testfile:
                test_gold_data = list(utils.read_conll(treebank.test_gold,maxSize=options.debug_test_sents,hard_lim=True))
                test_gold_file = os.path.join(treebank.outdir,'test-gold-debug' + ext) # location for the new dev file
                utils.write_conll(test_gold_file,test_gold_data) # write the new dev data to file
                treebank.test_gold = test_gold_file
            else:
                treebank.test_gold = test_file
            treebank.testfile = test_file
