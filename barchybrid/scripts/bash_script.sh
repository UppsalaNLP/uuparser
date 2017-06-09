#!/bin/bash
#usage: ./bash_script inputdir outputdir

test_dir=$1
outdir=$2

python json_parser.py $test_dir/ $outdir
wait

chmod +x bash_segmenter.sh

./bash_segmenter.sh

wait

#Parsing

./parse_multi_monoling.sh $outdir $test_dir
wait
./parse_surprise_languages.sh $outdir $test_dir
wait
