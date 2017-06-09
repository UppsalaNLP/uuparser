#!/bin/bash
outdir=$1
datadir=$2
parserdir=/home/Uppsala/bist-parser/uuparser/bist-parser/barchybrid
cp 'include.txt' $parserdir
cd $parserdir

input="include.txt"
while IFS= read -r var
do
    /home/Uppsala/parse.sh $outdir $datadir $var
    wait
done < "$input"
