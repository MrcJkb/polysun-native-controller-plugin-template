package com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Platform;
 import com.sun.jna.Native;

import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;


public class NativePluginController extends AbstractPluginController {

  private static final Logger logger = Logger.getLogger(NativePluginController.class.getName());

  private static final int STAGE_INITIALISE_SIMULATION = 0;
  private static final int STAGE_DURING_SIMULATION = 1;
  private static final int STAGE_TERMINATE_SIMULATION = 2;
  private Optional<NativeLibraryInterface> nativeLibraryInterfaceOptional = Optional.empty();
  private boolean[] controlSignalsInUse;
  private boolean[] sensorsInUse;

  @Override
  public String getName() {
    return "TODO: Your controller name here";
  }

  @Override
  public PluginControllerConfiguration getConfiguration(Map<String, Object> parameters) throws PluginControllerException {
    return new PluginControllerConfiguration.Builder()
      .setNumGenericSensors(30)
      .setNumGenericControlSignals(30)
      .build();
  }

  @Override
  public void build(PolysunSettings polysunSettings, Map<String, Object> parameters) throws PluginControllerException {
    super.build(polysunSettings, parameters);
    if (nativeLibraryInterfaceOptional.isEmpty()) {
      loadNativeLibrary();
    }
    sensorsInUse = new boolean[getSensors().size()];
    for (int i = 0; i < sensorsInUse.length; i++) {
      sensorsInUse[i] = getSensors()
        .get(i)
        .isUsed();
    }
    controlSignalsInUse = new boolean[getControlSignals().size()];
    for (int i = 0; i < controlSignalsInUse.length; i++) {
      controlSignalsInUse[i] = getControlSignals()
        .get(i)
        .isUsed();
    }
  }

  @Override
  public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
    super.initialiseSimulation(parameters);
    var nativeLibraryInterface = getNativeLibraryInterface();
    try {
      nativeLibraryInterface.control(0, true, new float[0], new boolean[0], new float[0], new boolean[0], new float[0], STAGE_INITIALISE_SIMULATION, false, 0);
      // TODO add fixed time step configuration
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to call native library.", t);
      throw new PluginControllerException("Failed to call native library.", t);
    }
  }

  @Override
  public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
      boolean preRun, Map<String, Object> parameters) throws PluginControllerException {
    if (nativeLibraryInterfaceOptional.isEmpty()) {
      throw new PluginControllerException("The native library is not loaded.");
    }
    var nativeLibraryInterface = nativeLibraryInterfaceOptional.get();
    try {
      return nativeLibraryInterface.control(simulationTime, status, sensors, sensorsInUse, controlSignals, controlSignalsInUse, logValues, STAGE_DURING_SIMULATION, preRun, 0);
      // TODO add fixed time step configuration
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to call native library.", t);
      throw new PluginControllerException("Failed to call native library.", t);
    }
  }

  @Override
  public void terminateSimulation(Map<String, Object> parameters) {
    super.terminateSimulation(parameters);
    try {
      var nativeLibraryInterface = getNativeLibraryInterface();
      nativeLibraryInterface.control(0, true, new float[0], new boolean[0], new float[0], new boolean[0], new float[0], STAGE_TERMINATE_SIMULATION, false, 0);
      // TODO add fixed time step configuration
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to call native library.", t);
      throw new RuntimeException("Failed to call native library.", t);
    }
  }

  private void loadNativeLibrary() throws PluginControllerException {
    try {
      String nativeLibraryFileName = switch(Platform.getOSType()) {
        case Platform.LINUX -> "libcontrol.so";
        case Platform.WINDOWS, Platform.WINDOWSCE -> "control.dll";
        case Platform.MAC -> "libcontrol.dylib";
        default -> throw new PluginControllerException("The current operating system is not supported.");
      };
      URL nativeLibraryUrl = getClass().getClassLoader()
        .getResource(nativeLibraryFileName);
      String nativeLibraryFilePath = nativeLibraryUrl.getFile();
      var nativeLibraryInterface = (NativeLibraryInterface) Native.load(nativeLibraryFilePath, NativeLibraryInterface.class);
      nativeLibraryInterfaceOptional = Optional.of(nativeLibraryInterface);
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to load native library.", t);
      throw new PluginControllerException("Failed to load native library.", t);
    }
  }

  private NativeLibraryInterface getNativeLibraryInterface() throws PluginControllerException {
    if (nativeLibraryInterfaceOptional.isEmpty()) {
      throw new PluginControllerException("The native library is not loaded.");
    }
    var nativeLibraryInterface = nativeLibraryInterfaceOptional.get();
    return nativeLibraryInterface;
  }

}
