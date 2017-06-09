#!/bin/bash
input=$1
trained_models_dir=$2
while IFS= read -r var
do
    best=$(grep '^LAS' $trained_models_dir/$var/*.txt | awk '{print $(NF),$RS}' | sort -nr |awk 'NR==1')
    if [[ $best ]]; then
        echo $best
    else
        echo $var
    fi
done < "$input"
