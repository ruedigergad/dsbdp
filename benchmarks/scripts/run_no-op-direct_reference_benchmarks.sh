#!/bin/sh

rm -f queue-setup.cfg


OUTPUT_NAME_PREFIX="no-op-direct_flooding_2016-05-21_1"

echo "Compiling..."
lein uberjar &> "${OUTPUT_NAME_PREFIX}.compile_out"

echo "Running..."
./get_cpu_stats.sh "${OUTPUT_NAME_PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${OUTPUT_NAME_PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -s "no-op-direct" > "${OUTPUT_NAME_PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat    

echo "Sleeping..."
sleep 20s



OUTPUT_NAME_PREFIX="no-op-direct_flooding_with-latency-measurement_2016-05-21_1"

echo "Compiling..."
lein uberjar &> "${OUTPUT_NAME_PREFIX}.compile_out"

echo "Running..."
./get_cpu_stats.sh "${OUTPUT_NAME_PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${OUTPUT_NAME_PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -s "no-op-direct" -L > "${OUTPUT_NAME_PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat    

