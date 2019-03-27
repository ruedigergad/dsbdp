/*
 *
 * Copyright (C) 2016 Ruediger Gad
 *
 * This software is released under the terms of the Eclipse Public License 
 * (EPL) 1.0. You can find a copy of the EPL at: 
 * http://opensource.org/licenses/eclipse-1.0.php
 *
 */

package dsbdp;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a simple collector for gathering latency statistics in multi-threaded situations.
 */
public class LatencyProbeCollector {

    private long count = 0;
    private long sum = 0;

    private final Lock lock = new ReentrantLock();

    public void addProbe(LatencyProbe probe) {
        /*
         * In the current use case, we prefer to miss an addition in favor of performance.
         */
        if (lock.tryLock()) {
            count++;
            sum += probe.getDelta();
            lock.unlock();
        }
    }

    public void reset() {
        lock.lock();

        this.count = 0;
        this.sum = 0;
        
        lock.unlock();
    }

    public double getMean() {
        double ret;

        lock.lock();

        if (this.count > 0 && this.sum > 0) {
            ret = ((double) this.sum) / ((double) this.count);
        } else if (this.count == 0 && this.sum == 0) {
            ret = 0;
        } else {
            ret = -1;
        }

        lock.unlock();

        return ret;
    }

}

