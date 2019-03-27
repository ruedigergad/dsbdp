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

/**
 *  This is an experiment.
 *  The purpose is primarily to compare the efficiency of the Clojure
 *  way of implementing something like this via loop and recur.
 */
public class ProcessingLoop {
    
    private final Thread thread;
    private volatile boolean running;

    public ProcessingLoop (final Runnable runnable) {
        this(null, runnable);
    }

    public ProcessingLoop (final String id, final Runnable runnable) {
        this.thread = new Thread (new Runnable () {
            @Override
            public void run () {
                while (running) {
                    runnable.run();
                }
            }
        });
        if (id != null) {
            this.thread.setName(id);
        }
    }

    public void start () {
        running = true;
        thread.start();
    }

    public void stop () {
        running = false;
    }

    public void interrupt () {
        running = false;
        thread.interrupt();
    }

}

