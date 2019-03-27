#!/bin/sh



rm queue-setup.cfg
lein uberjar &> "lein_uberjar.compile_out"


for i in 1 4 16 64 256 1024 4096 16384
do
    PREFIX="simple-pmap_${i}_pcap-java-map-inc_2016-05-22_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -c ${i} -s "pcap-java-map-simple-pmap" -L > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done

for i in 4 16 64 256 1024 4096 16384
do
    PREFIX="simple-reducers-map_${i}_pcap-java-map-inc_2016-05-22_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -c ${i} -p $((i / 4)) -s "pcap-java-map-simple-reducers-map" -L > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done

for i in 1 2 4 8 16 32 64
do
    PREFIX="simple-reducers-map-partition-divisor_${i}_pcap-java-map-inc_2016-05-22_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -c 16384 -p $((16384 / i)) -s "pcap-java-map-simple-reducers-map" -L > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done



