import os
import sys

#usage:
    #python file.txt trained_models_dir
    # where the file contains iso codes of languages for which you want a model
    # and trained_models_dir is a directory containing trained models and their
    # evaluation on the dev set for the languages of interest

if len(sys.argv) < 3:
    raise Exception("You must specify at least a file with language codes and a directory with models")
else:
    include_file = sys.argv[1]
    trained_models_dir = sys.argv[2].strip("/")
    #make sure there are no annoying spaces
    print(f'Removing leading and trailing spaces from {include_file}')
    os.system(f"sed -i 's/\\s*//g' {include_file}")
    print('Finding best iteration for each language and storing in best_epochs.txt')
    cmd = f'./scripts/best_res.sh {include_file} {trained_models_dir} >best_epochs.txt'
    os.system(cmd)
    d = {}
    outdir = trained_models_dir
if len(sys.argv) == 4:
    outdir = sys.argv[3]

if not os.path.exists(outdir):
    print(f'Creating directory {outdir}')
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
        cmd = f'./scripts/get_last_epoch.sh {lang} {trained_models_dir}'
        lastEpoch = os.popen(cmd)

for lang in d:
    lpath = outdir + '/' + lang + '/'
    if not os.path.exists(lpath):
        print(f'Creating directory {lpath}')
        os.mkdir(lpath)
    infile = trained_models_dir + '/' + lang + '/barchybrid.model' + str(d[lang])
    outfile = lpath + 'barchybrid.model'
    if os.path.exists(infile):
        print(f'Copying {infile} to {outfile}'')
        os.system(f'cp {infile} {outfile}')
    if outdir != trained_models_dir: 
        paramfile = trained_models_dir + '/' + lang + '/params.pickle'
        print(f'Copying {paramfile} to {lpath}'')
        os.system(f'cp {paramfile} {lpath}')
