#!/bin/sh



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_opennlp_timestamp_scenario_2_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "opennlp-single-inc" -m "[1 2 1]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



