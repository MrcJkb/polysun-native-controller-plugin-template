#include <stdlib.h>
#include <math.h> 
#include "control.h"
#include <stdio.h>
#include <iostream>

using namespace std;
extern "C" {

  static int lastSimulationTime;
  static int lastDay;

  void control(int simulationTime, int status, float sensors[], int sensorsInUse[], float controlSignals[], int controlSignalsInUse[], float logValues[], int stage, int preRun, int maximumTimeStepSize) {

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
      std::cout << "Simulate day:" << day << endl;
      lastDay = day;
    }
  }
}
