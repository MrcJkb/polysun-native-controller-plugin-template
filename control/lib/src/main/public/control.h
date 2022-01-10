#ifndef CONTROL_H
#define CONTROL_H

#ifdef _WIN32
#define CONTROL_EXPORT_FUNC __declspec(dllexport)
#else
#define CONTROL_EXPORT_FUNC
#endif

extern "C" void control(int simulationTime, int status, float sensors[], int sensorsInUse[], float controlSignals[], int controlSignalsInUse[], float logValues[], int stage, int preRun, int maximumTimeStepSize);

#endif
