#!/bin/bash


echo "OneToOneConcurrentArrayQueue3_add-counted_remove" > queue-setup.cfg
lein uberjar &> busy_sleep_tests.compile_out




PREFIX="busy_sleep_1000_dips_scenario_1_2016-01-31_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep-direct -i "[1000000]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


IN_DATA=("_" "_" \
"[500000 500000]" \
"[333333 333333 333333]" \
"[250000 250000 250000 250000]" \
"[200000 200000 200000 200000 200000]" \
"[166667 166667 166667 166667 166667 166667]" \
"[142857 142857 142857 142857 142857 142857 142857]" \
"[125000 125000 125000 125000 125000 125000 125000 125000]")

for i in $(seq 2 8)
do
    PREFIX="busy_sleep_1000_dips_scenario_${i}_2016-01-31_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep -i "${IN_DATA[${i}]}" > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done




PREFIX="busy_sleep_10000_dips_scenario_1_2016-01-31_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep-direct -i "[100000]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


IN_DATA=("_" "_" \
"[50000 50000]" \
"[33333 33333 33333]" \
"[25000 25000 25000 25000]" \
"[20000 20000 20000 20000 20000]" \
"[16667 16667 16667 16667 16667 16667]" \
"[14286 14286 14286 14286 14286 14286 14286]" \
"[12500 12500 12500 12500 12500 12500 12500 12500]")

for i in $(seq 2 8)
do
    PREFIX="busy_sleep_10000_dips_scenario_${i}_2016-01-31_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep -i "${IN_DATA[${i}]}" > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done




PREFIX="busy_sleep_100000_dips_scenario_1_2016-01-31_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep-direct -i "[10000]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


IN_DATA=("_" "_" \
"[5000 5000]" \
"[3333 3333 3333]" \
"[2500 2500 2500 2500]" \
"[2000 2000 2000 2000 2000]" \
"[1667 1667 1667 1667 1667 1667]" \
"[1429 1429 1429 1429 1429 1429 1429]" \
"[1250 1250 1250 1250 1250 1250 1250 1250]")

for i in $(seq 2 8)
do
    PREFIX="busy_sleep_100000_dips_scenario_${i}_2016-01-31_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep -i "${IN_DATA[${i}]}" > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done



PREFIX="busy_sleep_200000_dips_scenario_1_2016-01-31_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep-direct -i "[5000]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


IN_DATA=("_" "_" \
"[2500 2500]" \
"[1667 1667 1667]" \
"[1250 1250 1250 1250]" \
"[1000 1000 1000 1000 1000]" \
"[833 833 833 833 833 833]" \
"[714 714 714 714 714 714 714]" \
"[625 625 625 625 625 625 625 625]")

for i in $(seq 2 8)
do
    PREFIX="busy_sleep_200000_dips_scenario_${i}_2016-01-31_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep -i "${IN_DATA[${i}]}" > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done



PREFIX="busy_sleep_400000_dips_scenario_1_2016-01-31_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep-direct -i "[2500]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


IN_DATA=("_" "_" \
"[1250 1250]" \
"[833 833 833]" \
"[625 625 625 625]" \
"[500 500 500 500 500]" \
"[417 417 417 417 417 417]" \
"[357 357 357 357 357 357 357]" \
"[313 313 313 313 313 313 313 313]")

for i in $(seq 2 8)
do
    PREFIX="busy_sleep_400000_dips_scenario_${i}_2016-01-31_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep -i "${IN_DATA[${i}]}" > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done




PREFIX="busy_sleep_1000000_dips_scenario_1_2016-01-31_1"
echo ${PREFIX}
./get_cpu_stats.sh "${PREFIX}.cpu_out" &
dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep-direct -i "[1000]" > "${PREFIX}.stats_out"

sleep 5s

killall get_cpu_stats.sh
killall dstat
sleep 20s


IN_DATA=("_" "_" \
"[500 500]" \
"[333 333 333]" \
"[250 250 250 250]" \
"[200 200 200 200 200]" \
"[167 167 167 167 167 167]" \
"[143 143 143 143 143 143 143]" \
"[125 125 125 125 125 125 125 125]")

for i in $(seq 2 8)
do
    PREFIX="busy_sleep_1000000_dips_scenario_${i}_2016-01-31_1"
    echo ${PREFIX}
    ./get_cpu_stats.sh "${PREFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${PREFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 2000 -d 1 -s busy-sleep -i "${IN_DATA[${i}]}" > "${PREFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat
    sleep 20s
done

