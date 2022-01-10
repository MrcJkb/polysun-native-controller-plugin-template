package com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller;

import com.sun.jna.Library;

public interface NativeLibraryInterface extends Library {

  int[] control(int simulationTime, boolean status, float[] sensors, boolean[] sensorsInUse, float[] controlSignals, boolean[] controlSignalsInUse, float[] logValues, int stage, boolean preRun, int maximumTimeStepSize);

}
