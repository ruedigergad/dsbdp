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

/**
 * This is a simple probe for measuring latency.
 */
public class LatencyProbe {

    private final Object data;
    private final long startTime;
    private long endTime;

    public LatencyProbe() {
        this(null);
    }

    public LatencyProbe(Object data) {
        this.data = data;
        this.startTime = System.nanoTime();
    }

    public void done() {
        this.endTime = System.nanoTime();
    }

    public Object getData() {
        return this.data;
    }

    public long getDelta() {
        return this.endTime - this.startTime;
    }

}

