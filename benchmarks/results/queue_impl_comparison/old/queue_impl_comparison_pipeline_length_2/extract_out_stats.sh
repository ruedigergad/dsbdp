#!/usr/bin/fish
for i in *.out ; echo $i \t\t\t\t (grep out-delta $i | awk '{print $14}' | tail -n +20 | head -n -10 | Rscript -e 'd<-scan("stdin", quiet=TRUE); cat(min(d), mean(d), max(d), sd(d), sep=" ")') ; end

