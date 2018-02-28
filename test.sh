#!/bin/bash


cd barchybrid

outdir=../EXP
datadir=../data

python2 src/parser.py --dynet-seed 123456789 --outdir $outdir --trainfile $datadir/train.conllu --devfile $datadir/dev.conllu --epochs 1 --dynet-mem 5000 

