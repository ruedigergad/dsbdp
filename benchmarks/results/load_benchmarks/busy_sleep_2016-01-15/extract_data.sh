#!/usr/bin/bash

SUFFIX="2016-01-15"
OUT_FILE="extracted_out_stats.data"

RESULT=""

for i in $(seq 1 8)
do
    echo $i

    LINE="${i}"

    for j in 1000 10000 100000 200000 400000 1000000
    do
        echo $j

        DIRECT_RESULT=$(grep out-delta busy_sleep_${j}_dips_scenario_1_${SUFFIX}.stats_out | awk '{print $14}' | tail -n +15 | head -n -5 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(mean(d), sep=" ")')

        NTH_RESULT=$(grep out-delta busy_sleep_${j}_dips_scenario_${i}_${SUFFIX}.stats_out | awk '{print $14}' | tail -n +15 | head -n -5 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(mean(d), sep=" ")')

        LINE="${LINE} ${DIRECT_RESULT} ${NTH_RESULT}"
    done

    RESULT="${RESULT}\n${LINE}"
    
    echo -e ${RESULT} > ${OUT_FILE}
    cat ${OUT_FILE}
done

