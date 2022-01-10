# polysun-native-controller-plugin-template
A project template for building a Polysun plugin controller that uses native libraries, from code written in C++, C and other languages. 

## Getting started ##

* Clone this repository.
* Rename the cloned directory to your plugin's name.
* Navigate to the cloned directory.
* Delete the `.git` directory.
* Modify the [`control C++ function`](./control/lib/src/main/cpp/control.cpp). 
* Search this project's file contents for `TODO` tags and add your custom information (controller name, description, author, etc.).
    * [`build.gradle.kts`](./build.gradle.kts)
    * [`settings.gradle.kts`](./settings.gradle.kts)
    * [`NativeControllerPlugin.java`](./src/main/java/com/github/mrcjkb/polysun/plugin/controller/NativeControllerPlugin.java)
    * [`NativePluginController.java`](./src/main/java/com/github/mrcjkb/polysun/plugin/controller/nativeplugincontroller/NativePluginController.java)

Please refer to the Polysun user manual and the [`polysun-plugin-if/README.md`](https://bitbucket.org/velasolaris/polysun-plugin-if/src/master/README.md) for plugin development documentation.

## Building your plugin ##

* In Linux or macOS, run `./gradlew` from a terminal, or double-click the `gradlew` file.
* In Windows, double-click the `gradlew.bat` file to build your plugin (Windows may hide the .bat file extension) or run `gradlew.bat` from a PowerShell session.
* The resulting Jar file can be found in the `build/libs` subdirectory of this project.

>:warning: The native library must be compiled on the operating system it is intended to be used on.
> If you would like your plugin to be cross-compatible with Linux, Windows and macOS,
> you must build it manually on each platform.
>
> I have not tested this project on Windows or macOS, and I do not have any plans to at the moment.
> The `copyNativeLibraries` task in [`build.gradle.kts`](./build.gradle.kts) assumes output paths for Windows and macOS.
> If they are incorrect, please submit an issue or a pull request.


### Manually building the native library ###

* Linux/MacOS:
```bash
cd control
./gradlew assembleRelease
```
* Windows:
```powershell
cd control
gradlew.bat assembleRelease
```
* The library is written to `control/lib/build/lib/main/release/<platform>/<arch>/<lib>control{.so,.dylib,.dll}`


## Using your plugin in Polysun ##

To use your plugin, copy it to the `plugins` subdirectory of the Polysun data directory.
If Polysun is running, it will be available after restarting Polysun.

## Debugging ##

You can use `std::cout` to print to the console from C++ code.
If running Polysun from a console (terminal or PowerShell), any calls to `std::cout` will be printed.
They do not appear in the Polysun logs.
Additionally, the `logValues[]` parameter of the `control` function can be used to display values in Polysun's simulation analysis.

