#ifndef CONTROL_H
#define CONTROL_H

#ifdef _WIN32
#define CONTROL_EXPORT_FUNC __declspec(dllexport)
#else
#define CONTROL_EXPORT_FUNC
#endif

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
  /// <param name="preRun">0 = initialise simulation, 1 = simulate, 2 = terminate simulation</param>  
  /// <param name="maximumTimeStepSize">Flag indicating whether Polysun is in the preliminary simulation (0 = simulation, 1 = preliminary simulation)</param>  
  /// <param name="simulationTime"></param>  defined by the plugin controller. Polysun is guaranteed to simulate a time step in this interval.
  extern "C" void control(int simulationTime,
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
      int maximumTimeStepSize);
#endif
