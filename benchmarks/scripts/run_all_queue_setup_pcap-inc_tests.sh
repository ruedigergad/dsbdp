#!/bin/sh

SUFFIX="_2016-05-21_1"

for i in ArrayBlockingQueue_add ArrayBlockingQueue_offer ArrayBlockingQueue_put ArrayBlockingQueue_add_remove ArrayBlockingQueue_add-counted-yield_remove-yield \
         LinkedBlockingQueue_add LinkedBlockingQueue_offer LinkedBlockingQueue_put LinkedBlockingQueue_add_remove LinkedBlockingQueue_add-counted-yield_remove-yield \
         OneToOneConcurrentArrayQueue3_add_remove OneToOneConcurrentArrayQueue3_add-counted-yield_remove-yield OneToOneConcurrentArrayQueue3_add-counted_remove \
         LinkedTransferQueue_transfer LinkedTransferQueue_tryTransfer \
         LinkedTransferQueue_transfer-counted-no-sleep LinkedTransferQueue_transfer-counted-sleep-1ms \
         LinkedTransferQueue_tryTransfer-counted-no-timeout \
         LinkedTransferQueue_tryTransfer-counted-1ms LinkedTransferQueue_tryTransfer-counted-10ms LinkedTransferQueue_tryTransfer-counted-100ms \
         clojure_core_async_chan
do
    echo $i
    echo $i > queue-setup.cfg

    echo "Compiling..."
    lein uberjar &> "${i}${SUFFIX}.compile_out"

    echo "Running..."
    ./get_cpu_stats.sh "${i}${SUFFIX}.cpu_out" &
    dstat --nocolor --noheaders --output "${i}${SUFFIX}.dstat_out" &> /dev/null &

    java -jar target/dsbdp-0.2.0-SNAPSHOT-standalone.jar -b 1000 -d 1 -s "pcap-json-inc" -m "[8 9]" -L > "${i}${SUFFIX}.stats_out"

    sleep 5s

    killall get_cpu_stats.sh
    killall dstat    

    echo "Sleeping..."
    sleep 20s
    rm queue-setup.cfg
done

