from glob import glob
import os, re, json, sys

# creates a json dictionary mapping treebank names to treebank ids
# usage: python create_json.py ud_treebank_dir json_file_name

paths = glob(sys.argv[1] + "*")
outfile_json = sys.argv[2]

isos = {}

for path in paths:
    treebank_name = os.path.split(path)[1]
    testfiles = glob(path + "/*test.conllu") # returns list
    trainfiles = glob(path + "/*train.conllu")
    if testfiles:
        testfiles = testfiles[0]
        m = re.match(r'(.*)-ud-test.conllu',os.path.basename(testfiles))
        isos[treebank_name] = m.group(1)
    elif trainfiles:
        trainfiles = trainfiles[0]
        m = re.match(r'(.*)-ud-train.conllu',os.path.basename(trainfiles))
        isos[treebank_name] = m.group(1)
    else:
        print "No data found for %s"%treebank_name

with open(outfile_json, 'w') as fh:
    json.dump(isos, fh, indent=2)

