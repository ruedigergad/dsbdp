#!/bin/sh


echo "OneToOneConcurrentArrayQueue3_add-counted_remove" > queue-setup.cfg
lein uberjar &> "lein_uberjar.compile_out"


PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_0_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-direct" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_1_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-inc" -m "[17]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_2_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-inc" -m "[8 9]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_3_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-inc" -m "[6 5 6]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_4_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-inc" -m "[7 3 3 4]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_5_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-inc" -m "[6 2 3 2 4]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s



PREFIX="OneToOneConcurrentArrayQueue3_add-counted_remove_pcap-java-map-inc_timestamp_scenario_6_2016-02-09_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-java-map-inc" -m "[5 2 3 2 3 2]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat


rm queue-setup.cfg


