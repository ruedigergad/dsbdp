#!/usr/bin/fish
for i in *.stats_out ; echo $i \t\t\t\t (grep out-delta $i | awk '{print $14}' | tail -n +15 | head -n -5 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")') ; end

