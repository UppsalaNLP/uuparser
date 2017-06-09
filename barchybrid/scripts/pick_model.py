import os
import sys

#usage:
    #python file.txt trained_models_dir
    # where the file contains iso codes of languages for which you want a model
    # and trained_models_dir is a directory containing trained models and their
    # evaluation on the dev set for the languages of interest

include_file = sys.argv[1]
trained_models_dir = sys.argv[2]
cmd = './best_res.sh %s %s >best_epochs.txt'%(include_file, trained_models_dir)
os.system(cmd)
d = {}
outdir = 'models/'
if not os.path.exists(outdir):
    os.mkdir(outdir)
for line in open('best_epochs.txt','r'):
    try:
        needed = line.split('dev_epoch_')
        lang = needed[0].split(trained_models_dir)[1].strip("/")
        epoch = needed[1].split(".conllu")[0]
        d[lang] = epoch
    except:
        IndexError
        lang = line.strip()
        #TODO: HACKY!! MODIFY!!
        d[lang] = 20

for lang in d:
    lpath = outdir + lang + '/'
    if not os.path.exists(lpath):
        os.mkdir(lpath)
    infile = trained_models_dir + lang + '/barchybrid.model' + str(d[lang])
    outfile = lpath + 'barchybrid.model'
    if os.path.exists(infile):
        os.system('cp %s %s'%(infile,outfile))
    paramfile = trained_models_dir + lang + '/params.pickle'
    os.system('cp %s %s'%(paramfile,lpath))
