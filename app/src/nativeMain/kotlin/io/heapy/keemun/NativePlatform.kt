package io.heapy.keemun

import kotlinx.cinterop.toKString
import okio.FileSystem
import platform.posix.getenv

internal actual val KeemunSystemFileSystem: FileSystem = FileSystem.SYSTEM
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual val KeemunUserHome: String?
    get() = getenv("HOME")?.toKString()
