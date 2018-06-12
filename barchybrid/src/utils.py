from collections import Counter
import re
import os,time
from itertools import chain
from operator import itemgetter
import random
import codecs

# default random seeds for parser. hard-coded here to enable sharing between other modules
#dynet_seed = 123456789
python_seed = 1

class ConllEntry:
    def __init__(self, id, form, lemma, pos, cpos, feats=None, parent_id=None, relation=None, deps=None, misc=None):
        self.id = id
        self.form = form
        self.norm = normalize(form)
        #why was the uppercasing necessary?
        #self.cpos = cpos.upper()
        #self.pos = pos.upper()
        self.cpos = cpos
        self.pos = pos
        self.parent_id = parent_id
        self.relation = relation

        self.lemma = lemma
        self.feats = feats
        self.deps = deps
        self.misc = misc

        self.pred_parent_id = None
        self.pred_relation = None
        self.language_id = None
        self.language_name = None

        self.pred_pos = None
        self.pred_cpos = None


    def __str__(self):
        values = [str(self.id), self.form, self.lemma, \
                  self.pred_cpos if self.pred_cpos else self.cpos,\
                  self.pred_pos if self.pred_pos else self.pos,\
                  self.feats, str(self.pred_parent_id) if self.pred_parent_id \
                  is not None else str(self.parent_id), self.pred_relation if\
                  self.pred_relation is not None else self.relation, \
                  self.deps, self.misc]
        return '\t'.join(['_' if v is None else v for v in values])

class Treebank(object):
    def __init__(self,trainfile,devfile,testfile):
        self.name = 'noname'
        self.trainfile = trainfile
        self.devfile = devfile
        self.dev_gold = devfile
        self.test_gold = testfile
        self.testfile = testfile
        self.outfilename = None

class UDtreebank(Treebank):
    def __init__(self,treebank_info,location, shared_task=False, shared_task_data_dir=None):
        """
        Read treebank info to a treebank object
        The treebank_info element contains different information if in
        shared_task mode or not
        If not: it contains a tuple with name + iso ID
        Else: it contains a dictionary with some information
        """
        if shared_task:
            self.lcode = treebank_info['lcode']
            if treebank_info['tcode'] == '0':
                self.iso_id = treebank_info['lcode']
            else:
                self.iso_id =treebank_info['lcode'] + '_' + treebank_info['tcode']
            #self.testfile = location + self.iso_id + '.conllu'
            #setting where I have Yan's output as input
            self.testfile = location + self.iso_id + '.txt'
            if not os.path.exists(self.testfile):
                self.testfile = shared_task_data_dir + self.iso_id + '.conllu'
            self.dev_gold = shared_task_data_dir + self.iso_id + '.conllu'
            self.test_gold = shared_task_data_dir + self.iso_id + '.conllu'
            self.outfilename = treebank_info['outfile']
        else:
            self.name, self.iso_id = treebank_info
            files_prefix = location + "/" + self.name + "/" + self.iso_id
            self.trainfile = files_prefix + "-ud-train.conllu"
            self.devfile = files_prefix + "-ud-dev.conllu"
            #TODO: change if using data that has the test sets
            self.testfile = files_prefix + "-ud-dev.conllu"
            self.test_gold= files_prefix + "-ud-dev.conllu"
            self.dev_gold= files_prefix + "-ud-dev.conllu"
            self.outfilename = self.iso_id + '.conllu'

class ParseForest:
    def __init__(self, sentence):
        self.roots = list(sentence)

        for root in self.roots:
            root.children = []
            root.scores = None
            root.parent = None
            root.pred_parent_id = None
            root.pred_relation = None
            root.vecs = None
            root.lstms = None

    def __len__(self):
        return len(self.roots)


    def Attach(self, parent_index, child_index):
        parent = self.roots[parent_index]
        child = self.roots[child_index]

        child.pred_parent_id = parent.id
        del self.roots[child_index]


def isProj(sentence):
    forest = ParseForest(sentence)
    unassigned = {entry.id: sum([1 for pentry in sentence if pentry.parent_id == entry.id]) for entry in sentence}

    for _ in xrange(len(sentence)):
        for i in xrange(len(forest.roots) - 1):
            if forest.roots[i].parent_id == forest.roots[i+1].id and unassigned[forest.roots[i].id] == 0:
                unassigned[forest.roots[i+1].id]-=1
                forest.Attach(i+1, i)
                break
            if forest.roots[i+1].parent_id == forest.roots[i].id and unassigned[forest.roots[i+1].id] == 0:
                unassigned[forest.roots[i].id]-=1
                forest.Attach(i, i+1)
                break

    return len(forest.roots) == 1


def vocab(conll_path, path_is_dir=False):
    """
    Collect frequencies of words, cpos, pos and deprels + languages.
    """
    wordsCount = Counter()
    charsCount = Counter()
    posCount = Counter()
    cposCount = Counter()
    relCount = Counter()
    langCounter = Counter()

    if path_is_dir:
        data = read_conll_dir(conll_path,"train")
    else:
        data = read_conll(conll_path, vocab_prep=True)

    for sentence in data:
        wordsCount.update([node.norm for node in sentence if isinstance(node, ConllEntry)])
        for node in sentence:
            if isinstance(node, ConllEntry) and not node.form == u"*root*":
                charsCount.update(node.form)
        #TODO: aren't counters an overkill if we then just use the keys?
        posCount.update([node.pos for node in sentence if isinstance(node, ConllEntry)])
        cposCount.update([node.cpos for node in sentence if isinstance(node, ConllEntry)])
        relCount.update([node.relation for node in sentence if isinstance(node, ConllEntry)])
        #TODO:this is hacky to do that at every word - should do it at every new lang
        if path_is_dir:
            langCounter.update([node.language_id for node in sentence if
                                isinstance(node, ConllEntry)])

    return (wordsCount, {w: i for i, w in enumerate(wordsCount.keys())},
            posCount.keys(), cposCount.keys(), relCount.keys(),
            langCounter.keys() if langCounter else None, charsCount.keys())


def conll_dir_to_list(
    languages, data_dir,
    shared_task=False,
    shared_task_data_dir=None,
    treebanks_from_json=True,
):
    import json
    if not treebanks_from_json:
        treebank_metadata = []
        for entry in os.listdir(data_dir):
            candidate_dir = os.path.join(data_dir, entry)
            if os.path.isdir(candidate_dir):
                for filename in os.listdir(candidate_dir):
                    fields = filename.split('-ud-')
                    if len(fields) == 2 and fields[1] == 'train.conllu':
                        treebank_metadata.append((
                            entry.decode('utf-8'),
                            fields[0].decode('utf-8')
                        ))
    elif shared_task:
        metadataFile = shared_task_data_dir +'/metadata.json'
        metadata = codecs.open(metadataFile, 'r',encoding='utf-8')
        json_str = metadata.read()
        treebank_metadata = json.loads(json_str)
    else:
        ud_iso_file = codecs.open('./src/utils/ud_iso.json',encoding='utf-8')
        json_str = ud_iso_file.read()
        iso_dict = json.loads(json_str)
        treebank_metadata = iso_dict.items()
    ud_treebanks = [UDtreebank(treebank_info, data_dir, shared_task, shared_task_data_dir) \
            for treebank_info in treebank_metadata ]
    return ud_treebanks

def read_conll_dir(languages,filetype,maxSize=-1):
    #print "Max size for each corpus: ", maxSize
    if filetype == "train":
        return chain(*(read_conll(lang.trainfile,lang.name,maxSize) for lang in languages))
    elif filetype == "dev":
        return chain(*(read_conll(lang.devfile,lang.name) for lang in languages if lang.pred_dev))
    elif filetype == "test":
        return chain(*(read_conll(lang.testfile,lang.name) for lang in languages))

def read_conll(filename, language=None, maxSize=-1, hard_lim=False, vocab_prep=False, drop_nproj=False):
    # hard lim means capping the corpus size across the whole training procedure
    # soft lim means using a sample of the whole corpus at each epoch
    fh = codecs.open(filename,'r',encoding='utf-8')
    print "Reading " + filename
    if vocab_prep and not hard_lim:
        maxSize = -1 # when preparing the vocab with a soft limit we need to use the whole corpus
    ts = time.time()
    dropped = 0
    read = 0
    root = ConllEntry(0, '*root*', '*root*', 'ROOT-POS', 'ROOT-CPOS', '_', -1, 'rroot', '_', '_')
    root.language_id = language
    tokens = [root]
    yield_count = 0
    if maxSize > 0 and not hard_lim:
        all_tokens = []
    for line in fh:
        tok = line.strip().split('\t')
        if not tok or line.strip() == '':
            if len(tokens)>1:
                conll_tokens = [t for t in tokens if isinstance(t,ConllEntry)]
                if not drop_nproj or isProj(conll_tokens): # keep going if it's projective or we're not dropping non-projective sents
                #dropping the proj for exploring swap
                #if not isProj([t for t in tokens if isinstance(t, ConllEntry)]):
                    inorder_tokens = inorder(conll_tokens)
                    for i,t in enumerate(inorder_tokens):
                        t.projective_order = i
                    for tok in conll_tokens:
                        tok.rdeps = [i.id for i in conll_tokens if i.parent_id == tok.id]
                        if tok.id != 0:
                            tok.parent_entry = [i for i in conll_tokens if i.id == tok.parent_id][0]
                    if maxSize > 0:
                        if not hard_lim:
                            all_tokens.append(tokens)
                        else:
                            yield tokens
                            yield_count += 1
                            if yield_count == maxSize:
                                print "Capping size of corpus at " + str(yield_count) + " sentences"
                                break;
                    else:
                        yield tokens
                else:
                    #print 'Non-projective sentence dropped'
                    dropped += 1
                read += 1
            tokens = [root]
        else:
            if line[0] == '#' or '-' in tok[0] or '.' in tok[0]:
                tokens.append(line.strip())
            else:
                token = ConllEntry(int(tok[0]), tok[1], tok[2], tok[4], tok[3], tok[5], int(tok[6]) if tok[6] != '_' else -1, tok[7], tok[8], tok[9])
                token.language_id = language
                tokens.append(token)
    if hard_lim and yield_count < maxSize:
        print 'Warning: unable to yield ' + str(maxSize) + ' sentences, only ' + str(yield_count) + ' found'

# TODO: deal with case where there are still unyielded tokens
# e.g. when there is no newline at end of file
#    if len(tokens) > 1:
#        yield tokens

    print read, 'sentences read'

    if maxSize > 0 and not hard_lim:
        random.shuffle(all_tokens)
        all_tokens = all_tokens[:maxSize]
        print "Yielding " + str(len(all_tokens)) + " random sentences"
        for toks in all_tokens:
            yield toks

    te = time.time()
    print 'Time: %.2gs'%(te-ts)

def write_conll(fn, conll_gen):
    print "Writing to " + fn
    sents = 0
    with codecs.open(fn, 'w', encoding='utf-8') as fh:
        for sentence in conll_gen:
            sents += 1
            for entry in sentence[1:]:
                fh.write(unicode(entry) + '\n')
                #print str(entry)
            fh.write('\n')
        print "Wrote " + str(sents) + " sentences"

def write_conll_multiling(conll_gen, languages):
    lang_dict = {language.name:language for language in languages}
    cur_lang = conll_gen[0][0].language_id
    outfile = lang_dict[cur_lang].outfilename
    fh = codecs.open(outfile,'w',encoding='utf-8')
    print "Writing to " + outfile
    for sentence in conll_gen:
        if cur_lang != sentence[0].language_id:
            fh.close()
            cur_lang = sentence[0].language_id
            outfile = lang_dict[cur_lang].outfilename
            fh = codecs.open(outfile,'w',encoding='utf-8')
            print "Writing to " + outfile
        for entry in sentence[1:]:
            fh.write(unicode(entry) + '\n')
        fh.write('\n')


def parse_list_arg(l):
    """Return a list of line values if it's a file or a list of values if it
    is a string"""
    if os.path.isfile(l):
        f = codecs.open(l, 'r', encoding='utf-8')
        return [line.strip("\n").split()[0] for line in f]
    else:
        return [el for el in l.split(" ")]

numberRegex = re.compile("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+");
def normalize(word):
    return 'NUM' if numberRegex.match(word) else word.lower()

def evaluate(gold,test,conllu):
    scoresfile = test + '.txt'
    print "Writing to " + scoresfile
    if not conllu:
        #os.system('perl src/utils/eval.pl -g ' + gold + ' -s ' + test  + ' > ' + scoresfile + ' &')
        os.system('perl src/utils/eval.pl -g ' + gold + ' -s ' + test  + ' > ' + scoresfile )
    else:
        os.system('python src/utils/evaluation_script/conll17_ud_eval.py -v -w src/utils/evaluation_script/weights.clas ' + gold + ' ' + test + ' > ' + scoresfile)
    score = get_LAS_score(scoresfile,conllu)
    return score

def inorder(sentence):
    queue = [sentence[0]]
    def inorder_helper(sentence,i):
        results = []
        left_children = [entry for entry in sentence[:i] if entry.parent_id == i]
        for child in left_children:
            results += inorder_helper(sentence,child.id)
        results.append(sentence[i])

        right_children = [entry for entry in sentence[i:] if entry.parent_id == i ]
        for child in right_children:
            results += inorder_helper(sentence,child.id)
        return results
    return inorder_helper(sentence,queue[0].id)

def set_python_seed(seed):
    random.seed(seed)

def generate_seed():
    return random.randint(0,10**9) # this range seems to work for Dynet and Python's random function

def set_seeds(options):
    global dynet_seed
    global python_seed
    if not options.predict: # seeds shouldn't make any different when predicting so can always use defaults
        # the order of everything here is crucial
        if options.use_default_seed: # user requests default seeds
            print "Using default Python seed"
            set_python_seed(python_seed)
#        else:
#            if options.parser_seed: # user specifies a parser seed
#                python_seed = options.parser_seed
#                print "Set parser seed to: " + str(python_seed)
#            else: # user says nothing about seeds - need to generate everything
#                python_seed = generate_seed()
#                print "Generated parser seed: " + str(python_seed)
#            set_python_seed(python_seed)
#            dynet_seed = generate_seed()
#            print "Generated Dynet seed: " + str(dynet_seed)

def get_LAS_score(filename, conllu=True):
    score = None
    with codecs.open(filename,'r',encoding='utf-8') as fh:
        if conllu:
            for line in fh:
                if re.match(r'^LAS',line):
                    elements = line.split()
                    score = float(elements[6]) # should extract the F1 score
        else:
            las_line = [line for line in fh][0]
            score = float(las_line.split('=')[1].split()[0])

    return score
