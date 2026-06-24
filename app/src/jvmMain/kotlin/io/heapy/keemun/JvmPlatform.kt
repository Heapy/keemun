package io.heapy.keemun

import okio.FileSystem

internal actual val KeemunSystemFileSystem: FileSystem = FileSystem.SYSTEM
internal actual val KeemunUserHome: String?
    get() = System.getProperty("user.home")
