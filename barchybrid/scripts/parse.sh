#!/bin/bash

outdir=$1
datadir=$outdir/tokenized/
shared_task_datadir=$2
language=$3
modeldir=/media/data/parsing_models
tempdir=$outdir/parse_temp/

python src/parser.py --predict --outdir $tempdir --modeldir $modeldir --datadir $datadir --include $language --pseudo-proj --shared_task --shared_task_outdir $outdir --disablePredEval --shared_task_datadir $shared_task_datadir
