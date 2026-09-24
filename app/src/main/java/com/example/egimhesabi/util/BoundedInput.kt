package com.example.egimhesabi.util

import java.io.InputStream
import java.io.ByteArrayOutputStream

object BoundedInput {
    fun read(input: InputStream, limit: Int = 8 * 1024 * 1024): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= limit) { "Dosya en fazla ${limit / 1024 / 1024} MB olabilir." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
