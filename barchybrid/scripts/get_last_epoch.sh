#!/bin/bash

var=$1
modeldir=$2
modelname='barchybrid.model'

ncar=$((${#var} + ${#modelname} + 3 + ${#modeldir}))
lastepoch=$(ls $modeldir/$var/barchybrid.model* | sort -nr -k 1.$ncar | awk 'NR==1')
lastepochNum=$(echo $lastepoch | tail -c 3 | grep -oh [0-9]*)
echo $lastepochNum
