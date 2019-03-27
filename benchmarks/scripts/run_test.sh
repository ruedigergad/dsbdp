#!/bin/sh

./get_cpu_stats.sh "${1}.cpu_out" &
dstat --nocolor --noheaders --output "${1}.dstat_out" &> /dev/null

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar > "${1}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat

