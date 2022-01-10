plugins {
    `cpp-library`
}

library {
    baseName.set("control")
    // Set the target operating systems and architectures for this library
    targetMachines.set(listOf(machines.windows.x86, machines.windows.x86_64, machines.macOS.x86_64, machines.linux.x86_64))
    // linkage.set(listOf(Linkage.STATIC))
}

