#!/bin/sh

SUFFIX="_2015-12-29_1"

for i in LinkedBlockingQueue_put LinkedBlockingQueue_offer ArrayBlockingQueue_offer ArrayBlockingQueue_put LinkedTransferQueue_transfer LinkedTransferQueue_tryTransfer LinkedTransferQueue_transfer-counted-no-sleep LinkedTransferQueue_transfer-counted-sleep-1ms LinkedTransferQueue_tryTransfer-counted-no-timeout LinkedTransferQueue_tryTransfer-counted-1ms LinkedTransferQueue_tryTransfer-counted-10ms LinkedTransferQueue_tryTransfer-counted-100ms
do
    echo $i
    echo $i > queue-setup.cfg

    echo "Compiling..."
    lein uberjar &> "${i}${SUFFIX}.compile_out"

    echo "Running..."
    ./run_test.sh "${i}${SUFFIX}"

    echo "Sleeping..."
    sleep 10s
    rm queue-setup.cfg
done

