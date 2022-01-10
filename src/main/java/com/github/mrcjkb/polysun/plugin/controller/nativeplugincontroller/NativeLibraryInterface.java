package com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface NativeLibraryInterface extends Library {

  void control(int simulationTime, 
      int status, 
      float[] sensors,
      int[] sensorsInUse, 
      int numberOfSensors,
      Pointer controlSignals,
      int[] controlSignalsInUse,
      int numberOfControlSignals,
      Pointer logValues,
      int numberOfLogValues,
      int stage,
      int preRun,
      int maximumTimeStepSize);

}
