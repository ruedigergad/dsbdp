#!/bin/sh

for d in queue*
do
    echo $d
    cd $d
    ../extract_out_stats.sh
    cd -
done

