#!/usr/bin/fish
for i in *.stats_out
    set BASENAME (basename -s .stats_out $i)

    set STATS (grep out-delta $i | awk '{print $14}' | tail -n +15 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')
    set LATENCY_STATS (grep out-delta $i | awk '{print $17}' | tail -n +15 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')
#    set DSTAT_CPU (tail -n +15 "$BASENAME".dstat_out | awk -F, '{print $1}' | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')
#    set CPU_STATS (tail -n +15 "$BASENAME".cpu_out | awk -F, '{OFS="\n"}{print $1,$2,$3,$4,$5,$6,$7,$8}' | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')

    echo $i \t $STATS \t $LATENCY_STATS
    #\t $DSTAT_CPU \t $CPU_STATS

end

