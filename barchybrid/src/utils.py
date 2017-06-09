from collections import Counter
import re
import os,time
from itertools import chain
from operator import itemgetter
import random

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
        """
        Write predicted values for each field if they are else, write what was
        on the input
        """
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
        self.testfile = testfile

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
            self.testfile = location + self.iso_id + '.txt'
            if not os.path.exists(self.testfile):
                self.testfile = shared_task_data_dir + self.iso_id + '.conllu'
            self.test_gold = shared_task_data_dir + self.iso_id + '.conllu'
            self.outfilename = treebank_info['outfile']
            self.name = self.iso_id
        else:
            self.name, self.iso_id = treebank_info
            files_prefix = location + self.name + "/" + self.iso_id
            self.trainfile = files_prefix + "-ud-train.conllu"
            self.devfile = files_prefix + "-ud-dev.conllu"
            #TODO: change when we actually have test data
            self.testfile = files_prefix + "-ud-dev.conllu"
            self.test_gold= files_prefix + "-ud-dev.conllu"
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


def vocab(conll_path, path_is_dir=False, max_rels=200):
    wordsCount = Counter()
    charsCount = Counter()
    posCount = Counter()
    cposCount = Counter()
    relCount = Counter()

    if path_is_dir:
        data = read_conll_dir(conll_path,"train")
        langCounter = Counter()
    else:
        conllFP = open(conll_path,'r')
        data = read_conll(conllFP,False)
        langCounter = None

    for sentence in data:
        wordsCount.update([node.norm for node in sentence if isinstance(node, ConllEntry)])
        for node in sentence:
            if isinstance(node, ConllEntry):
                charsCount.update(node.form)
        posCount.update([node.pos for node in sentence if isinstance(node, ConllEntry)])
        cposCount.update([node.cpos for node in sentence if isinstance(node, ConllEntry)])
        relCount.update([node.relation for node in sentence if isinstance(node, ConllEntry)])
        #TODO:refactor: who needs a language id counter??
        if path_is_dir:
            langCounter.update([node.language_id for node in sentence if isinstance(node, ConllEntry)])

    relsNum = min(len(relCount), max_rels)
    droppedRels = [ rel for rel, val in sorted(relCount.iteritems(), key=itemgetter(1), reverse=True) ]
    droppedRels = droppedRels[:relsNum]
    print "dropped relations", len(relCount) - len(droppedRels), "out of", len(relCount)

    return (wordsCount, {w: i for i, w in enumerate(wordsCount.keys())},
            posCount.keys(), droppedRels, cposCount.keys(),
            langCounter.keys() if langCounter else None,
            charsCount.keys())

def conll_dir_to_list(languages, data_dir,shared_task=False, shared_task_data_dir=None):
    import json
    if shared_task:
        metadataFile = shared_task_data_dir +'/metadata.json'
        metadata = open(metadataFile, 'r')
        json_str = metadata.read()
        treebank_metadata = json.loads(json_str)
    else:
        ud_iso_file = open('./src/utils/ud_iso.json')
        json_str = ud_iso_file.read()
        iso_dict = json.loads(json_str)
        treebank_metadata = iso_dict.items()
    json_treebanks= [UDtreebank(treebank_info,data_dir,shared_task, shared_task_data_dir) \
            for treebank_info in treebank_metadata ]
    return json_treebanks

def read_conll_dir(languages,filetype,drop_proj=False,maxSize=-1):
    #print "Max size for each corpus: ", maxSize
    #TODO: somehow this is quick for train but really slow for dev - even more
    #for French  NO CLUE WHY
    if filetype == "train":
        if maxSize > 0:
            return chain(*(read_conll_max(open(lang.trainfile,'r'),maxSize,drop_proj,lang.name) for lang in languages))
        return chain(*(read_conll(open(lang.trainfile,'r'),drop_proj,lang.name) for lang in languages))
    elif filetype == "dev":
        return chain(*(read_conll(open(lang.devfile,'r'),drop_proj,lang.name) for lang in languages if os.path.exists(lang.devfile)))
    elif filetype == "test":
        return chain(*(read_conll(open(lang.testfile,'r'),drop_proj,lang.name) for lang in languages))


#write a version with lists instead of generators for the maxSize version, and have an if clause above!!
def read_conll_max(fh, maxSize, proj=True, language=None):
    ts = time.time()
    dropped = 0
    read = 0
    root = ConllEntry(0, '*root*', '*root*', 'ROOT-POS', 'ROOT-CPOS', '_', -1, 'rroot', '_', '_')
    root.language_id = language
    tokens = [root]
    all_tokens = []
    for line in fh:
        tok = line.strip().split('\t')
        if not tok or line.strip() == '':
            if len(tokens)>1:
                if not proj or isProj([t for t in tokens if isinstance(t, ConllEntry)]):
                    all_tokens.append(list(tokens))
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
    #print "TOKENS: ", tokens
    random.shuffle(all_tokens)
    all_tokens = all_tokens[:maxSize]
    #print "MAX SIZE fixing: ", maxSize, "  ", len(all_tokens)
    #yield all_tokens

    te = time.time()
    print dropped, 'dropped non-projective sentences.'
    print read, 'sentences read.'
    print 'Time: %.2gs'%(te-ts)
    return iter(all_tokens)


def read_conll(fh, proj=True, language=None):
    ts = time.time()
    dropped = 0
    read = 0
    root = ConllEntry(0, '*root*', '*root*', 'ROOT-POS', 'ROOT-CPOS', '_', -1, 'rroot', '_', '_')
    root.language_id = language
    tokens = [root]
    for line in fh:
        tok = line.strip().split('\t')
        if not tok or line.strip() == '':
            if len(tokens)>1:
                if not proj or isProj([t for t in tokens if isinstance(t, ConllEntry)]):
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
    if len(tokens) > 1:
        yield tokens

    te = time.time()
    print dropped, 'dropped non-projective sentences.'
    print read, 'sentences read.'
    print 'Time: %.2gs'%(te-ts)


def write_conll(fn, conll_gen):
    with open(fn, 'w') as fh:
        for sentence in conll_gen:
            for entry in sentence[1:]:
                fh.write(str(entry) + '\n')
                #print str(entry)
            fh.write('\n')

def write_conll_multiling(conll_gen, languages):
    lang_dict = {language.name:language for language in languages}
    cur_lang = conll_gen[0][0].language_id
    outfile = open(cur_lang.outfilename,'w')
    for sentence in conll_gen:
        if cur_lang != sentence[0].language_id:
            outfile.close()
            cur_lang = sentence[0].language_id
            outfile = open(cur_lang.outfilename,'w')
        for entry in sentence[1:]:
            outfile.write(str(entry) + '\n')
        outfile.write('\n')


def parse_list_arg(l):
    """Return a list of line values if it's a file or a list of values if it
    is a string"""
    if os.path.isfile(l):
        f = open(l, 'r')
        return [line.strip("\n") for line in f]
    else:
        return [el for el in l.split(" ")]

numberRegex = re.compile("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+");
def normalize(word):
    return 'NUM' if numberRegex.match(word) else word.lower()

def projectivise(infile,outfile,projectiviser='deleteme'):
    if not os.path.exists(outfile):
        pp_cmd = "java -jar -Xmx2g ./lib/maltparser-1.9.0/maltparser-1.9.0.jar\
        -c " + projectiviser + " -m proj -i " + infile + " -o "  + outfile + " -pp head"
        os.system(pp_cmd)

def deprojectivise(infile,outfile,projectiviser='deleteme'):
    pp_cmd = "java -jar -Xmx2g ./lib/maltparser-1.9.0/maltparser-1.9.0.jar\
            -c " + projectiviser + " -m deproj -i " + infile + " -o "  + outfile
    os.system(pp_cmd)
    #os.system("mv %s %s.proj"%(infile,infile))
    os.system("mv %s %s"%(outfile,infile))

def evaluate(gold,test,conllu):
    if not conllu:
        os.system('perl src/utils/eval.pl -g ' + gold + ' -s ' + test  + ' > ' + test + '.txt &')
    else:
        os.system('python src/utils/evaluation_script/conll17_ud_eval.py -v -w src/utils/evaluation_script/weights.clas ' + gold + ' ' + test + ' > ' + test+ '.txt')


def remove_ellipsis_lines(infile,outfile):
    out = open(outfile,'w')
    for line in open(infile,'r'):
        tok = line.strip().split('\t')
        if not tok or line.strip() == '':
            out.write('\n')
        else:
            if '.' not in tok[0]:
                out.write(line)
    out.close()

