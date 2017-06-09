# -*- coding: utf-8 -*-
import json
import sys
import os

outdir=sys.argv[2]
surprise_languages = ['bxr','kmr','sme','hsb','kk','ug']
#languages_we_failed = ['en','en_pud']

bash_wt = open('bash_segmenter.sh', 'w')

bash_wt.write('#!/bin/bash\n\n')
tok_dir = os.path.join(outdir,'tokenized')
if not os.path.exists(tok_dir):
    os.makedirs(tok_dir)
parse_temp_dir = os.path.join(outdir,'parse_temp')
if not os.path.exists(parse_temp_dir):
    os.makedirs(parse_temp_dir)

bash_wt.write('cd segmenter/\n\n')

json_file = open(sys.argv[1] + '/metadata.json', 'r').read()

json_items = json.loads(json_file)

valid_UD_dir = os.listdir('segmenter/ud-treebanks-conll2017/')

dir_dict = {} #map a ltcode to a proper UD_dir

for v_dir in valid_UD_dir:
    if os.path.isdir('segmenter/ud-treebanks-conll2017/' + v_dir):
        for f_name in os.listdir('segmenter/ud-treebanks-conll2017/' + v_dir):
            if '.conllu' in f_name:
                lt_code = f_name[:f_name.index('-ud')]
                dir_dict[lt_code] = v_dir
                break

parser_include_file = open('include.txt','w')
surprise_lang_file = open('surpriselang.txt','w')


for j_i in json_items:

    if j_i['tcode'] == '0':
        lt_code = j_i['lcode']
    else:
        lt_code = j_i['lcode'] + '_' + j_i['tcode']

    if lt_code not in surprise_languages:
        #if lt_code in languages_we_failed:
        parser_include_file.write(lt_code)
        parser_include_file.write("\n")
    else:
        surprise_lang_file.write(lt_code)
        surprise_lang_file.write("\n")

    if lt_code in dir_dict:
        model_path = dir_dict[lt_code]
        if 'Buryat' in model_path:
            model_path = 'UD_Bulgarian'
        elif 'Kazakh' in model_path:
            model_path = 'UD_Russian'
        elif 'Kurmanji' in model_path:
            model_path = 'UD_Turkish'
        elif 'North_Sami' in model_path:
            model_path = 'UD_Slovenian'
        elif 'Upper_Sorbian' in model_path:
            model_path = 'UD_Czech'
        elif 'Uyghur' in model_path:
            model_path = 'UD_Persian'
    else: #for parallel test sets
        assert j_i['lcode'] in dir_dict
        lt_code = j_i['lcode']
        model_path = dir_dict[lt_code]

    #if lt_code in languages_we_failed or lt_code in surprise_languages:
    bash_wt.write('python segmenter.py tag -p ud-treebanks-conll2017/' + model_path + ' -r ' + sys.argv[1] + j_i['rawfile'] + ' -opth ' + tok_dir + '/' + j_i['rawfile'] + '\n\n')
    bash_wt.write('wait\n\n')

bash_wt.write('cd ..\n\n')

bash_wt.close()
parser_include_file.close()
surprise_lang_file.close()
