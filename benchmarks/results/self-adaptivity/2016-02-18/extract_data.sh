#!/usr/bin/bash

PREFIX="self-adaptivity_experiment_2016-02-18_4"

grep out-delta $PREFIX.out | awk '{print $14}' | nl -v 0 -s " " -n ln -w2 > $PREFIX.out-delta

grep in-delta $PREFIX.out | awk '{print $11}' | nl -v 0 -s " " -n ln -w2 > $PREFIX.in-delta

grep mapping $PREFIX.out | grep -v options | sed 's/\[//' | sed 's/\]//' | awk '{print $2,$3,$4,$5}' | nl -v 0 -s " " -n ln -w2 > $PREFIX.mapping


