package com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Platform;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;

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
  private int[] controlSignalsInUse;
  private int[] sensorsInUse; // JNA does not support bool[]

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
    sensorsInUse = new int[sensorsUsed.length];
    for (int i = 0; i < sensorsUsed.length; i++) {
      sensorsInUse[i] = bool2int(sensorsUsed[i]);
    }
    controlSignalsInUse = new int[controlSignalsUsed.length];
    for (int i = 0; i < controlSignalsUsed.length; i++) {
      controlSignalsInUse[i] = bool2int(controlSignalsUsed[i]);
    }
  }

  @Override
  public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
    super.initialiseSimulation(parameters);
    passStageToNativeLibrary(STAGE_INITIALISE_SIMULATION);
  }

  @Override
  public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
      boolean preRun, Map<String, Object> parameters) throws PluginControllerException {
    if (nativeLibraryInterfaceOptional.isEmpty()) {
      throw new PluginControllerException("The native library is not loaded.");
    }
    var nativeLibraryInterface = nativeLibraryInterfaceOptional.get();
    try {
      for (int inUse : controlSignalsInUse) {
        if (inUse == 1) {
          logger.info("Control signal in use.");
        }
      }
      Pointer pControlSignals = floatArrayToPointer(controlSignals);
      Pointer pLogValues = floatArrayToPointer(logValues);
      nativeLibraryInterface.control(simulationTime,
          bool2int(status),
          sensors,
          sensorsInUse,
          sensors.length,
          pControlSignals,
          controlSignalsInUse,
          controlSignals.length,
          pLogValues,
          logValues.length,
          STAGE_DURING_SIMULATION,
          bool2int(preRun),
          0);
      floatArrayPointerToFloatArray(pControlSignals, controlSignals);
      // for (float controlSignal : controlSignals) {
      //   logger.info("Control signal set to " + controlSignal);
      // }
      floatArrayPointerToFloatArray(pLogValues, logValues);
      return null;
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
      passStageToNativeLibrary(STAGE_TERMINATE_SIMULATION);
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Could not terminate simulation", t);
      throw new RuntimeException(t);
    }
  }

  private NativeLibraryInterface getNativeLibraryInterface() throws PluginControllerException {
    if (nativeLibraryInterfaceOptional.isEmpty()) {
      throw new PluginControllerException("The native library is not loaded.");
    }
    var nativeLibraryInterface = nativeLibraryInterfaceOptional.get();
    return nativeLibraryInterface;
  }

  private void loadNativeLibrary() throws PluginControllerException {
    if (nativeLibraryInterfaceOptional.isPresent()) {
      logger.info("Native library already loaded.");
    }
    try {
      File tempLibrary = createTemporaryNativeLibrary();
      var nativeLibraryInterface = (NativeLibraryInterface) Native.load(tempLibrary.getAbsolutePath(), NativeLibraryInterface.class);
      nativeLibraryInterfaceOptional = Optional.of(nativeLibraryInterface);
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to load native library.", t);
      throw new PluginControllerException("Failed to load native library.", t);
    }
  }

  private File createTemporaryNativeLibrary() throws PluginControllerException {
    String nativeLibraryFileName = switch(Platform.getOSType()) {
      case Platform.LINUX -> "libcontrol.so";
      case Platform.WINDOWS, Platform.WINDOWSCE -> "control.dll";
      case Platform.MAC -> "libcontrol.dylib";
      default -> throw new PluginControllerException("The current operating system is not supported.");
    };
    InputStream nativeLibrarResourceStream = getClass().getClassLoader()
      .getResourceAsStream(nativeLibraryFileName);
    if (nativeLibrarResourceStream == null) {
      String osType = switch(Platform.getOSType()) {
        case Platform.LINUX -> "Linux";
        case Platform.WINDOWS, Platform.WINDOWSCE -> "Windows";
        case Platform.MAC -> "macOS";
          default -> "Unknown";
    };
      String message = "Could not find native library resource " + nativeLibraryFileName + " for OS " + osType;
      logger.warning(message);
      throw new PluginControllerException(message);
    }
    try {
      File tempDir = Files.createTempDirectory(UUID.randomUUID().toString()).toFile();
      File tempLibrary = new File(tempDir, nativeLibraryFileName);
      Files.copy(nativeLibrarResourceStream, tempLibrary.toPath());
      return tempLibrary;
    } catch (IOException e) {
      String message = "Could not copy native library resource " + nativeLibraryFileName + " to temporary directory.";
      logger.log(Level.WARNING, message, e);
      throw new PluginControllerException(message, e);
    }
  }

  private static Pointer floatArrayToPointer(float[] floatArray) {
    if (floatArray.length == 0) {
      return null;
    }
    Pointer floatArrayPointer = new Memory(floatArray.length * Native.getNativeSize(Float.TYPE));
    for (int i = 0; i < floatArray.length; i++) {
      floatArrayPointer.setFloat(i * Native.getNativeSize(Float.TYPE), floatArray[i]);
    }
    return floatArrayPointer;
  }

  private static void floatArrayPointerToFloatArray(Pointer floatArrayPointer, float[] floatArray) {
    for (int i = 0; i < floatArray.length; i++) {
      floatArray[i] = floatArrayPointer.getFloat(i * Native.getNativeSize(Float.TYPE));
    }
  }

  int bool2int(boolean tf) {
    return tf ? 1 : 0;
  }
  
  private void passStageToNativeLibrary(int stage) throws PluginControllerException {
    try {
      var nativeLibraryInterface = getNativeLibraryInterface();
      nativeLibraryInterface.control(0, 1, null, null, 0, null, null, 0, null, 0, stage, 0, 0);
      // TODO add fixed time step configuration
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to call native library.", t);
      throw new PluginControllerException("Failed to call native library.", t);
    }
  }

}
