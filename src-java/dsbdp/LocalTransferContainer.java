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
 * This is a simple container for transporting objects along the LocalPipeline.
 */
public class LocalTransferContainer {

    private Object in;
    private Object out;

    public LocalTransferContainer (Object in, Object out) {
        this.in = in;
        this.out = out;
    }

    public Object getIn () {
        return in;
    }

    public Object getOut () {
        return out;
    }

    public void setOut (Object out) {
        this.out = out;
    }

}

