#include <stdlib.h>
#include <math.h> 
#include "control.h"
#include <stdio.h>
#include <iostream>

using namespace std;
extern "C" {

  static int lastSimulationTime;
  static int lastDay;

  /// <summary>Control function, called by the Polysun plugin controller</summary>
  /// <param name="simulationTime">The simulation time in s (0 = beginning of the year)</param>  
  /// <param name="status">The status of the controller (0 = disabled, 1 = enabled)</param>  
  /// <param name="sensorsInUse">The sensor data</param>  
  /// <param name="numberOfSensors">Flags for the sensors that are in use (0 = not in use, 1 = in use)</param>  
  /// <param name="controlSignals">The number of sensors = length of the sensors and sensorsInUse arrays</param>  
  /// <param name="controlSignalsInUse">The control signal data -> Write to this array to set the controller's output signals</param>  
  /// <param name="numberOfControlSignals"></param>Flags for the control signals that are in use (0 = not in use, 1 = in use)  
  /// <param name="logValues"></param>The number of control signals = length of the controlSignals and controlSignalsInUse arrays  
  /// <param name="numberOfLogValues">Log values that can be displayed in the simulation analysis -> Write to this array to output to the simulation analysis</param>  
  /// <param name="stage">The number of log values = length of the logValues array</param>  
  /// <param name="preRun">1 = initialise simulation, 1 = simulate, 2 = terminate simulation</param>  
  /// <param name="maximumTimeStepSize">Flag indicating whether Polysun is in the preliminary simulation (0 = simulation, 1 = preliminary simulation)</param>  
  /// <param name="simulationTime"></param>  defined by the plugin controller. Polysun is guaranteed to simulate a time step in this interval.
  void control(int simulationTime,
      int status,
      float sensors[],
      int sensorsInUse[],
      int numberOfSensors,
      float controlSignals[],
      int controlSignalsInUse[],
      int numberOfControlSignals,
      float logValues[],
      int numberOfLogValues,
      int stage,
      int preRun, 
      int maximumTimeStepSize)
  { 
    if (stage == 0) {
      // This will print to stdout when running Polysun from the command line
      std::cout << "Initilise simulation..." << endl;
      lastDay = 0;
      return;
    } else if (stage == 2) {
      std::cout << "Terminate simulation..." << endl;
      return;
    }
    // Java JNA does not support bool
    bool isPreRun = preRun == 1;
    // Simulation
    if (!isPreRun && simulationTime > lastSimulationTime) {
      lastSimulationTime = simulationTime;
    }
    int day = truncf(simulationTime / (3600 * 24));
    if (!isPreRun && day > lastDay) {
      std::cout << "Simulate day: " << day << endl;
      lastDay = day;
    }

    for (int i = 0; i < numberOfControlSignals; i++) {
      // Write control signals here
      if (controlSignalsInUse[i] == 1) {
        controlSignals[i] = 1;
      }
    }
    for (int i = 0; i < numberOfLogValues; i++) {
      // Write log values here
      logValues[i] = 1;
    }
  }
}
