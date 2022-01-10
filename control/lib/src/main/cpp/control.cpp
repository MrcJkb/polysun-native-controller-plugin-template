#include <stdlib.h>
#include <math.h> 
#include "control.h"
#include <stdio.h>

static int lastSimulationTime;
static int lastDay;

int* control(int simulationTime, bool status, float sensors[], bool sensorsInUse[], float controlSignals[], bool controlSignalsInUse[], float logValues[], int stage, bool preRun, int maximumTimeStepSize) {

  if (stage == 0) {
    printf("Intialise simulation...");
    lastDay = 0;
    return new int[0];
  } else if (stage == 2) {
    printf("Terminate simulation...");
    return new int[0];
  }
  // Simulation
  if (!preRun && simulationTime > lastSimulationTime) {
    lastSimulationTime = simulationTime;
  }
  int day = truncf(simulationTime / (3600 * 24));
  if (!preRun && day > lastDay) {
    printf("Simulate day: %d/n", day);
    lastDay = day;
  }
  return new int[0];
}
