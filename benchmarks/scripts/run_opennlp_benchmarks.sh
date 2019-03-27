#!/bin/sh


echo "OneToOneConcurrentArrayQueue3_add-counted_remove" > queue-setup.cfg
lein uberjar &> "lein_uberjar.compile_out"


PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_opennlp_timestamp_scenario_0_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "opennlp-single-direct" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_opennlp_timestamp_scenario_1_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "opennlp-single-inc" -m "[4]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_opennlp_timestamp_scenario_2_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "opennlp-single-inc" -m "[2 2]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_opennlp_timestamp_scenario_3_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "opennlp-single-inc" -m "[1 1 2]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_opennlp_timestamp_scenario_4_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "opennlp-single-inc" -m "[1 1 1 1]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


