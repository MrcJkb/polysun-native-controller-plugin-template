package com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface NativeLibraryInterface extends Library {

  void control(int simulationTime, int status, float[] sensors, int[] sensorsInUse, Pointer controlSignals, int[] controlSignalsInUse, Pointer logValues, int stage, int preRun, int maximumTimeStepSize);

}
