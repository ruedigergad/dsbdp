#!/bin/bash


rm -f queue-setup.cfg
lein uberjar &> simple-pmap_no-op_collection_size_comparison_benchmarks.compile_out


PREFIX="simple-pmap_no-op_collection_size_comparison_collection-size_"
SUFFIX="_2016-05-22_1"
echo ${PREFIX}X${SUFFIX}

for i in 4 16 64 256 1024 4096 16384
do
    OUT_FILE_PREFIX="${PREFIX}${i}${SUFFIX}"
    echo ${OUT_FILE_PREFIX}
    ./get_cpu_stats.sh "${OUT_FILE_PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${OUT_FILE_PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -s no-op-simple-pmap -c ${i} -p $((i/4)) -L > "${OUT_FILE_PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done

