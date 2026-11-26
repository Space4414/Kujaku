package eu.kanade.tachiyomi.extension.api

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

internal data class KeiyoushiExtension(
    val name: String,
    val packageName: String,
    val apkUrl: String,
    val iconUrl: String,
    val extensionLib: String,
    val versionCode: Long,
    val versionName: String,
    val contentWarning: Int,
    val sources: List<KeiyoushiSource>,
)

internal data class KeiyoushiSource(
    val id: Long,
    val name: String,
    val language: String,
    val homeUrl: String,
)

internal object KeiyoushiIndex {
    fun decode(bytes: ByteArray): List<KeiyoushiExtension> {
        val payload = if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        } else {
            bytes
        }
        val index = ProtoReader(payload)
        var listBytes: ByteArray? = null
        var externalList: String? = null
        while (index.hasNext()) {
            val tag = index.readVarint().toInt()
            when (tag ushr 3) {
                101 -> listBytes = index.readBytes(tag and 7)
                102 -> externalList = index.readString(tag and 7)
                else -> index.skip(tag and 7)
            }
        }
        check(externalList == null) { "This index.pb points to an external extension list" }
        val listPayload = requireNotNull(listBytes) { "index.pb has no embedded extension list" }
        val list = ProtoReader(listPayload)
        val result = mutableListOf<KeiyoushiExtension>()
        while (list.hasNext()) {
            val tag = list.readVarint().toInt()
            if (tag ushr 3 == 1) {
                result += decodeExtension(list.readBytes(tag and 7))
            } else {
                list.skip(tag and 7)
            }
        }
        return result
    }

    private fun decodeExtension(bytes: ByteArray): KeiyoushiExtension {
        val reader = ProtoReader(bytes)
        var name = ""
        var packageName = ""
        var apkUrl = ""
        var iconUrl = ""
        var extensionLib = ""
        var versionCode = 0L
        var versionName = ""
        var warning = 0
        val sources = mutableListOf<KeiyoushiSource>()
        while (reader.hasNext()) {
            val tag = reader.readVarint().toInt()
            when (tag ushr 3) {
                1 -> name = reader.readString(tag and 7)
                2 -> packageName = reader.readString(tag and 7)
                3 -> {
                    val resources = ProtoReader(reader.readBytes(tag and 7))
                    while (resources.hasNext()) {
                        val resourceTag = resources.readVarint().toInt()
                        when (resourceTag ushr 3) {
                            1 -> apkUrl = resources.readString(resourceTag and 7)
                            2 -> iconUrl = resources.readString(resourceTag and 7)
                            else -> resources.skip(resourceTag and 7)
                        }
                    }
                }
                4 -> extensionLib = reader.readString(tag and 7)
                5 -> versionCode = reader.readVarint()
                6 -> versionName = reader.readString(tag and 7)
                7 -> warning = reader.readVarint().toInt()
                8 -> sources += decodeSource(reader.readBytes(tag and 7))
                else -> reader.skip(tag and 7)
            }
        }
        require(name.isNotBlank() && packageName.isNotBlank()) { "Invalid extension entry" }
        return KeiyoushiExtension(name, packageName, apkUrl, iconUrl, extensionLib, versionCode, versionName, warning, sources)
    }

    private fun decodeSource(bytes: ByteArray): KeiyoushiSource {
        val reader = ProtoReader(bytes)
        var id = 0L
        var name = ""
        var language = ""
        var homeUrl = ""
        while (reader.hasNext()) {
            val tag = reader.readVarint().toInt()
            when (tag ushr 3) {
                1 -> id = reader.readVarint()
                2 -> name = reader.readString(tag and 7)
                3 -> language = reader.readString(tag and 7)
                4 -> homeUrl = reader.readString(tag and 7)
                else -> reader.skip(tag and 7)
            }
        }
        return KeiyoushiSource(id, name, language, homeUrl)
    }
}

private class ProtoReader(private val data: ByteArray) {
    private var position = 0
    fun hasNext(): Boolean = position < data.size

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            check(position < data.size) { "Truncated protobuf varint" }
            val value = data[position++].toInt() and 0xff
            result = result or ((value and 0x7f).toLong() shl shift)
            if (value and 0x80 == 0) return result
            shift += 7
        }
        error("Malformed protobuf varint")
    }

    fun readBytes(wireType: Int): ByteArray {
        require(wireType == 2) { "Expected protobuf length-delimited field" }
        val length = readVarint().toInt()
        require(length >= 0 && position + length <= data.size) { "Invalid protobuf field length" }
        return data.copyOfRange(position, position + length).also { position += length }
    }

    fun readString(wireType: Int): String = readBytes(wireType).toString(Charsets.UTF_8)

    fun skip(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            1 -> advance(8)
            2 -> advance(readVarint().toInt())
            5 -> advance(4)
            else -> error("Unsupported protobuf wire type: $wireType")
        }
    }

    private fun advance(count: Int) {
        require(count >= 0 && position + count <= data.size) { "Truncated protobuf field" }
        position += count
    }
}
