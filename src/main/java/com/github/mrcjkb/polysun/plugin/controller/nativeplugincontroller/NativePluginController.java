package com.github.mrcjkb.polysun.plugin.controller.nativeplugincontroller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sun.jna.Platform;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;

import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Log;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;

import static java.lang.String.format;

public class NativePluginController extends AbstractPluginController {

  private static final Logger logger = Logger.getLogger(NativePluginController.class.getName());

  private static final int STAGE_INITIALISE_SIMULATION = 0;
  private static final int STAGE_DURING_SIMULATION = 1;
  private static final int STAGE_TERMINATE_SIMULATION = 2;
  private static final String ENFORCE_MAX_TIME_STEP_SIZE_PROPERTY_KEY = "Enforce maximum time step size";
  private static enum YesNoOption {
    Yes,
    No
  }
  private static final String ENFORCED_TIMESTEP_SIZE_PROPERTY_KEY = "Maximum step size / s";
  private static final int MINIMUM_ENFORCED_TIME_STEP_SIZE_S = 1;
  private static final int MAXIMUM_ENFORCED_TIME_STEP_SIZE_S = 3600;
  private static final int DEFAULT_ENFORCED_TIME_STEP_SIZE_S = 900;
  private static final int MAX_NUM_GENERIC_SENSORS = 30;
  private static final int MAX_NUM_GENERIC_CONTROL_SIGNALS = 30;
  private static final int SIMULATION_ANALYSIS_LOG_COUNT = 5;

  private Optional<NativeLibraryInterface> nativeLibraryInterfaceOptional = Optional.empty();
  // JNA does not support bool[]
  private int[] controlSignalsInUse;
  private int[] sensorsInUse; 
  private int enforcedMaximumTimeStepSize = 0; // 0 means not enforced

  @Override
  public String getName() {
    return "TODO: Your controller name here";
  }

  @Override
  public PluginControllerConfiguration getConfiguration(Map<String, Object> parameters) throws PluginControllerException {
    return new PluginControllerConfiguration.Builder()
      .setNumGenericSensors(MAX_NUM_GENERIC_SENSORS)
      .setNumGenericControlSignals(MAX_NUM_GENERIC_CONTROL_SIGNALS)
      .setLogs(IntStream.range(0, SIMULATION_ANALYSIS_LOG_COUNT)
          .mapToObj(logIndex -> new Log("LOG " + logIndex))
          .collect(Collectors.toList()))
      .setProperties(buildProperties())
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
    enforcedMaximumTimeStepSize = isEnforceMaxTimestepSize()
      ? getProperty(ENFORCED_TIMESTEP_SIZE_PROPERTY_KEY).getInt()
      : 0;
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
          enforcedMaximumTimeStepSize);
      floatArrayPointerToFloatArray(pControlSignals, controlSignals);
      floatArrayPointerToFloatArray(pLogValues, logValues);
      return null;
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

  @Override
  public List<String> getPropertiesToHide(PolysunSettings polysunSettings, Map<String, Object> parameters) {
    List<String> propertiesToHide = super.getPropertiesToHide(polysunSettings, parameters);
    if (!isEnforceMaxTimestepSize()) {
      propertiesToHide.add(ENFORCED_TIMESTEP_SIZE_PROPERTY_KEY);
    }
    return propertiesToHide;
  }

  @Override
  public int getFixedTimestep(Map<String, Object> parameters) {
    return enforcedMaximumTimeStepSize;
  }

  private static List<Property> buildProperties() {
    var enforcedTimeStepSizesProperty = new Property(ENFORCE_MAX_TIME_STEP_SIZE_PROPERTY_KEY,
        // Options
        enumToStringArray(YesNoOption.class),
        // Default option
        YesNoOption.Yes.ordinal(),
        // Tooltip
        """
        Yes: Enforce a maximum time step size (Smaller time steps are possible).
        No: Do not enforce a maximum time step size.
        """);
    var enforcedTimstepSizeProperty = new Property(ENFORCED_TIMESTEP_SIZE_PROPERTY_KEY,
        DEFAULT_ENFORCED_TIME_STEP_SIZE_S,
        MINIMUM_ENFORCED_TIME_STEP_SIZE_S,
        MAXIMUM_ENFORCED_TIME_STEP_SIZE_S,
        """
        The enforced time step size in seconds.
        Forces Polysun to limit the maximum time step size to the defined value.
        Smaller time steps than the maximum may still occur in the simulation.
        Warning: Setting a too small value may cause memory to run out during the simulation.
        """);
    return List.of(enforcedTimeStepSizesProperty,
        enforcedTimstepSizeProperty);
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
      nativeLibraryInterface.control(0, 1, null, null, 0, null, null, 0, null, 0, stage, 0, enforcedMaximumTimeStepSize);
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Failed to call native library.", t);
      throw new PluginControllerException("Failed to call native library.", t);
    }
  }

  private boolean isEnforceMaxTimestepSize() {
    return getProp(ENFORCE_MAX_TIME_STEP_SIZE_PROPERTY_KEY)
      .getInt() == YesNoOption.Yes.ordinal();
  }

  private static <E extends Enum<E>> String[] enumToStringArray(Class<E> enumClass) {
    var stringArray = EnumSet.allOf(enumClass)
      .stream()
      .map(Object::toString)
      .collect(Collectors.toList())
      .toArray(String[]::new);
    logger.fine(format("Converted enum %s to String array: %s", enumClass.getSimpleName(), Arrays.toString(stringArray)));
    return stringArray;
  }

}
