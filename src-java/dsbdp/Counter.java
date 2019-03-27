/*
 *
 * Copyright (C) 2015 Ruediger Gad
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
 * This is a simple counter that is intended to be used in multi-threaded situations.
 */
public class Counter {

    private final static long INITIAL_VALUE = 0L;

    private long val = INITIAL_VALUE;
    private final Lock lock = new ReentrantLock();

    public void inc() {
        /*
         * In the current use case, we prefer to miss an increment in favor of performance.
         */
        if (lock.tryLock()) {
            val++;
            lock.unlock();
        }
    }

    public void reset() {
        lock.lock();
        val = INITIAL_VALUE;
        lock.unlock();
    }

    public long value() {
        lock.lock();
        long ret = val;
        lock.unlock();
        return ret;
    }

}

