#!/bin/sh

OUT_FILE="$1"

rm -f $OUT_FILE

while true
do
    grep "cpu MHz" /proc/cpuinfo | tr -d '\n ' | sed 's/://g' | sed 's/cpuMHz//g' | sed 's/\s\+/,/g' | sed 's/^,//' | sed 's/$/\n/' >> $OUT_FILE
    sleep 1s
done

