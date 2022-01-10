#ifndef CONTROL_H
#define CONTROL_H

#ifdef _WIN32
#define CONTROL_EXPORT_FUNC __declspec(dllexport)
#else
#define CONTROL_EXPORT_FUNC
#endif

int* control(int simulationTime, bool status, float sensors[], bool sensorsInUse[], float controlSignals[], bool controlSignalsInUse[], float logValues[], int stage, bool preRun, int maximumTimeStepSize); 

#endif
