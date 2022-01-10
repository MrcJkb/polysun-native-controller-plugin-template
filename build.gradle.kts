import java.text.SimpleDateFormat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.*

plugins {
    eclipse
    idea
    `java-library`
}

description = "Your description here" // TODO
group = "your.group.name.here" // TODO e.g. "com.velasolaris.polysun"

defaultTasks = mutableListOf("jar")

repositories {
    mavenCentral()
}

java {
    toolchain {
        // Do not use a newer Java version than Polysun uses.
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.BELLSOFT)
    }
}

// The polysun-plugin-if version. Keep this up to date for the latest Polysun version.
// The latest version can be found at: https://search.maven.org/artifact/com.velasolaris.polysun/polysun-plugin-if
val pluginIfVersion = "1.1.0"
dependencies {
    api("com.velasolaris.polysun:polysun-plugin-if:$pluginIfVersion")
    implementation("net.java.dev.jna:jna:5.9.0")
    // Hint: When adding third-party dependencies, check the "lib" subdirectory of the Polysun installation for potential conficts.
    // If the library is used by Polysun, it is a good idea to use the same version.

    // For unit testing, please refer to the Gradle user manual: https://docs.gradle.org/current/userguide/java_testing.html
}

val javaVersion = System.getProperty("java.version")
val javaVendor = System.getProperty("java.vendor")
val javaVmVersion = System.getProperty("java.vm.version")
val osName = System.getProperty("os.name")
val osArchitecture = System.getProperty("os.arch")
val osVersion = System.getProperty("os.version")

tasks {
    val buildCpp by registering(Exec::class) {
        description = "Builds the 'control' subproject."
        workingDir = File("control")
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        val gradleCmd = if (os.isWindows) "gradlew.bat"
            else "./gradlew"
        commandLine(gradleCmd, "clean", "assembleRelease")
    }
    val copyNativeLibraries by registering(Copy::class) {
        dependsOn(buildCpp)
        from("control/lib/build/lib/main/release/linux/x86-64/") {
            include("*.so")
        }
        from("control/lib/build/lib/main/release/windows/x86-64/") {
            // Not tested
            include("*.dll")
        }
        from("control/lib/build/lib/main/release/macos/x86-64/") {
            // Not tested
            include("*.dylib")
        }
        into("src/main/resources")
    }
    compileJava {
        dependsOn(copyNativeLibraries)
    }
    jar {
        from(configurations.runtimeClasspath.get()
        .onEach { println("${it.name} dependency will be added to the plugin Jar.") }
        .map { if (it.isDirectory) it else zipTree(it) })
        archiveFileName.set("${rootProject.name}.jar")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            // The following fields are optional
            attributes["Library"] = rootProject.name
            attributes["Version"] = archiveVersion
            attributes["Company"] = "Your company name" // TODO (optional)
            attributes["Website"] = "Your website's URL" // TODO (optional)
            attributes["Built-By"] = System.getProperty("user.name")
            attributes["Build-Timestamp"] = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())
            attributes["Created-by"] = "Gradle ${gradle.gradleVersion}"
            attributes["Build-OS"] = "$osName $osArchitecture $osVersion"
            attributes["Build-Jdk"] = "$javaVersion ($javaVendor $javaVmVersion)"
            attributes["Build-OS"] = "$osName $osArchitecture $osVersion"
        }
    }
}
