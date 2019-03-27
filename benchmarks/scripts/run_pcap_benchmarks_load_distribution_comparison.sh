#!/bin/sh


echo "OneToOneConcurrentArrayQueue3_add-counted_remove" > queue-setup.cfg
lein uberjar &> "lein_uberjar.compile_out"


PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_0_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[14 1 1 1]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_1_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[11 2 2 2]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_2_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[8 3 3 3]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_3_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[5 4 4 4]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_4_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[2 5 5 5]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_5_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[4 4 4 5]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-json-inc_timestamp_scenario_6_2016-01-03_2"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[7 3 3 4]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat


rm queue-setup.cfg


