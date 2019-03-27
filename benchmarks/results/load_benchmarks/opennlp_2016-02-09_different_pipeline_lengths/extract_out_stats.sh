#!/usr/bin/fish
for i in *.stats_out
    set BASENAME (basename -s .stats_out $i)

    set STATS (grep out-delta $i | awk '{print $14}' | tail -n +15 | head -n -5 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')
    set DSTAT_CPU (tail -n +15 "$BASENAME".dstat_out | head -n -10 | awk -F, '{print $1}' | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')
    set CPU_STATS (tail -n +15 "$BASENAME".cpu_out | head -n -10 | awk -F, '{OFS="\n"}{print $1,$2,$3,$4,$5,$6,$7,$8}' | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")')

    echo $i \t $STATS \t $DSTAT_CPU \t $CPU_STATS

end

