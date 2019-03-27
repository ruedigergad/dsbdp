#!/bin/sh

for s in ABQ_offer_1 ABQ_put_1 LBQ_offer_1 LBQ_put_1 LTQ_tr-c-no-sleep_1 LTQ_tr-c-sleep-1ms_1 LTQ_tr_1 LTQ_tTr-c-1ms_1 LTQ_tTr-c-10ms_1 LTQ_tTr-c-100ms_1 LTQ_tTr-c-no-timeout_1 LTQ_tTr_1
do
    echo $s
    RESULTS=""
    for i in $(seq 1 8)
    do
        echo $i
        VALUES=$(grep ${s} queue_impl_comparison_pipeline_length_${i}.data | awk '{print $3,$5}')
        RESULTS="${RESULTS}${i} ${VALUES}\n"
    done
    echo -e $RESULTS
    echo -e $RESULTS | head -n 8 > per_queue_setup_results_${s}.data

    echo "Copying .gpl file..."
    cp per_queue_setup_results_ABQ_offer_1.gpl per_queue_setup_results_${s}.gpl
    sed -i "" "s/ABQ_offer_1/"${s}"/" per_queue_setup_results_${s}.gpl
    gnuplot per_queue_setup_results_${s}.gpl
done

