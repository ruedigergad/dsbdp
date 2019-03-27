#!/bin/sh



rm queue-setup.cfg
lein uberjar &> "lein_uberjar.compile_out"


for i in 1 2 4 8 16 32 64
do
    PREFIX="async-pipeline-divisor_${i}_no-op_2016-05-26_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -l $i -s "no-op-async-pipeline" -L > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done


for i in 1 2 4 8 16 32 64
do
    PREFIX="async-pipeline-divisor_${i}_pcap-json_2016-05-26_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -l $i -s "pcap-json-async-pipeline" -L > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done


for i in 1 2 4 8 16 32 64
do
    PREFIX="async-pipeline-divisor_${i}_opennlp-single_2016-05-26_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -l $i -s "opennlp-single-async-pipeline" -L > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done



