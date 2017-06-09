import os,sys
import utils
import copy

class OptionsManager(object):
    def __init__(self,options):
        """
        input: parser options
        object to harmonise the way we deal with the parser
        """
        if options.include and not options.datadir:
            raise Exception("You need to specify the data dir to include UD\
                            languages")
        #TODO: maybe add more sanity checks 
        if not options.predictFlag and not (options.rlFlag or options.rlMostFlag or options.headFlag):
            raise Exception("You must use either --userlmost or --userl or\
                            --usehead (you can use multiple)")
            #the diff between two is one is r/l/most child / the other is
            #element in the sentence
            #paper:
                #extended feature set
                # rightmost and leftmost modifiers of s0, s1 and s2 + leftmost
                # modifier of b0

        if not options.include:
            #just one model specified by train/dev and/or test
            if options.predictFlag:
                self.conllu = (os.path.splitext(options.conll_test.lower())[1] == '.conllu')
            else:
                self.conllu = (os.path.splitext(options.conll_dev.lower())[1] == '.conllu')
            self.treebank = utils.Treebank(options.conll_train, \
                                           options.conll_dev, options.conll_test)
            #this contains bullshit if I don't specify stuff...
            self.treebank.iso_id = None
            prepare_data(self.treebank, options.output,\
                                            options,self.conllu)

        else:
            #different models for all the languages
            self.conllu = True
            language_list = utils.parse_list_arg(options.include)
            json_treebanks = utils.conll_dir_to_list(language_list,options.datadir,options.shared_task,
                                    options.shared_task_datadir)
            self.languages = [lang for lang in json_treebanks if lang.iso_id in language_list]
            for language in self.languages:
                language.removeme = False
                language.outdir= "%s/%s"%(options.output,language.iso_id)
                language.modelDir= "%s/%s"%(options.modelDir,language.iso_id)
                model = "%s/%s"%(language.modelDir,options.model)
                if options.predictFlag and not os.path.exists(model):
                    for otherl in json_treebanks:
                        if otherl.lcode == language.lcode:
                            if otherl.lcode == otherl.iso_id:
                                language.modelDir = "%s/%s"%(options.modelDir,otherl.iso_id)
                                if options.pseudoProj:
                                    language.proj_mco = otherl.iso_id + '_pproj'
                else:
                    if options.pseudoProj:
                        language.proj_mco = language.iso_id + '_pproj'

                if not os.path.exists(language.outdir):
                    os.mkdir(language.outdir)
                try:
                    prepare_data(language, language.outdir, options,\
                             self.conllu)
                except Exception as e:
                    message = "Failing on %s. Error: %s \n"%(language.iso_id,str(e))
                    print message
                    sys.stderr.write(message)
                    language.removeme =True

            for language in self.languages:
                if language.removeme:
                    self.languages.remove(language)

        if options.include and not options.multiling:
            options.multi_monoling = True
            self.iterations = len(self.languages)
        else:
            options.multi_monoling = False
            self.iterations = 1
        options.drop_proj = not options.pseudoProj


def prepare_data(treebank,outdir,options,conllu=True):
    if conllu:
        ext = '.conllu'
    else:
        ext = '.conll'
    if options.pseudoProj:
        if not options.predictFlag:
            train_proj = os.path.join(outdir, 'train.pproj' + ext)
            treebank.dev_deproj = os.path.join(outdir, 'dev.deproj'+ext)
            treebank.proj_mco = treebank.iso_id + '_pproj'
            treebank.dev_gold = copy.deepcopy(treebank.devfile)
            #cleaning is a hack and ugly...
            clean_train = os.path.join(outdir,'train' + ext)
            clean_dev = os.path.join(outdir,'dev_clean' + ext)
            utils.remove_ellipsis_lines(treebank.trainfile,clean_train)
            if os.path.exists(treebank.devfile):
                utils.remove_ellipsis_lines(treebank.devfile,clean_dev)
            utils.projectivise(clean_train,train_proj,treebank.proj_mco)
            treebank.trainfile = train_proj
            treebank.devfile = clean_dev
        else:
            #treebank.proj_mco = treebank.name + '_pproj' 
            #TODO: pass it to params so that this does not happen?
            if not os.path.exists("%s.mco"%treebank.proj_mco):
                raise Exception("No projectiviser found")
            treebank.test_deproj = os.path.join(outdir, 'test.deproj'+ext)
            #TODO: reenable later
            #treebank.test_gold = copy.deepcopy(treebank.testfile)
            if not os.path.exists(treebank.testfile):
                raise Exception("Test file not found")
            clean_test = os.path.join(outdir,'test_clean' + ext)
            utils.remove_ellipsis_lines(treebank.testfile,clean_test)
            treebank.testfile = clean_test
    else:
        if not options.shared_task:
            treebank.dev_gold = treebank.devfile
            treebank.test_gold = treebank.testfile
