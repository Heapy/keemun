package io.heapy.keemun

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

internal expect val KeemunSystemFileSystem: FileSystem

data class KeemunPath(private val path: Path) {
    constructor(value: String) : this(value.toPath())

    val parent: KeemunPath?
        get() = path.parent?.let(::KeemunPath)

    val fileName: String
        get() = path.name

    fun resolve(child: String): KeemunPath =
        KeemunPath(path.resolve(child))

    internal fun toOkioPath(): Path = path

    override fun toString(): String = path.toString()
}

internal object KeemunFiles {
    private val fileSystem: FileSystem = KeemunSystemFileSystem

    fun exists(path: KeemunPath): Boolean =
        fileSystem.exists(path.toOkioPath())

    fun createDirectories(path: KeemunPath) {
        fileSystem.createDirectories(path.toOkioPath())
    }

    fun readString(path: KeemunPath): String =
        fileSystem.read(path.toOkioPath()) {
            readUtf8()
        }

    fun writeString(path: KeemunPath, text: String) {
        fileSystem.write(path.toOkioPath()) {
            writeUtf8(text)
        }
    }

    fun appendString(path: KeemunPath, text: String) {
        fileSystem.appendingSink(path.toOkioPath()).buffer().use { buffer ->
            buffer.writeUtf8(text)
        }
    }

    fun metadata(path: KeemunPath): GraphCacheKey {
        val metadata = fileSystem.metadata(path.toOkioPath())
        return GraphCacheKey(
            lastModifiedNanos = (metadata.lastModifiedAtMillis ?: 0L) * 1_000_000L,
            size = metadata.size ?: 0L,
        )
    }
}
