import os,sys
import utils
import copy

#TODO: this whole file is now quite hacky - used to be mostly useful for
#pseudoProj
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
        #this is now useless
        options.drop_proj = False


def prepare_data(treebank,outdir,options,conllu=True):
    if conllu:
        ext = '.conllu'
    else:
        ext = '.conll'
    if not options.shared_task:
        treebank.dev_gold = treebank.devfile
        treebank.test_gold = treebank.testfile
