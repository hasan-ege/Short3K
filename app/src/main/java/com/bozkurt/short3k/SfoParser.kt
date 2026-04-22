package com.bozkurt.short3k

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object SfoParser {
    fun parseSfo(inputStream: InputStream): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            val bytes = inputStream.readBytes()
            if (bytes.size < 20) return metadata
            
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.getInt()
            if (magic != 0x46535000) return metadata // "\0PSF"
            
            buffer.getInt() // version
            val keyTableOffset = buffer.getInt()
            val dataTableOffset = buffer.getInt()
            val entriesCount = buffer.getInt()
            
            for (i in 0 until entriesCount) {
                buffer.position(20 + i * 16)
                val keyOffset = buffer.getShort().toInt() and 0xFFFF
                val dataFormat = buffer.getShort().toInt() and 0xFFFF
                val dataLength = buffer.getInt()
                val dataMaxLength = buffer.getInt()
                val dataOffset = buffer.getInt()
                
                // Read key string
                var keyStr = ""
                var kPos = keyTableOffset + keyOffset
                while (kPos < bytes.size && bytes[kPos] != 0.toByte()) {
                    keyStr += bytes[kPos].toInt().toChar()
                    kPos++
                }
                
                // Read data string if format is string (0x0204 or 0x0004 for utf8)
                if (keyStr == "TITLE" || keyStr == "APP_VER" || keyStr == "TITLE_ID") {
                    val dPos = dataTableOffset + dataOffset
                    if (dPos + dataLength <= bytes.size) {
                        val value = String(bytes, dPos, dataLength, Charsets.UTF_8).trimEnd({ it == '\u0000' })
                        metadata[keyStr] = value
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return metadata
    }
}
