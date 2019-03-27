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
 * Helper class for experiments.
 */
public class ExperimentHelper {

    public static void busySleep(long duration) {
        long startTime = System.nanoTime();
        do {
            // Nothing
        } while ((System.nanoTime() - startTime) < duration);
    }

}

