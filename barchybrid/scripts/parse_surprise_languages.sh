#!/bin/bash
outdir=$1
datadir=$2
parserdir=/home/Uppsala/bist-parser/uuparser/bist-parser/barchybrid
cp 'surpriselang.txt' $parserdir
cd $parserdir

input="surpriselang.txt"
while IFS= read -r var
do
    /home/Uppsala/parse_multiling_option.sh $outdir $datadir $var
    wait
done < "$input"
