package com.r3.sgx.rng.host

import com.google.common.io.BaseEncoding
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.channels.Channels

object Hex {
    fun byteBufferToHex(byteBuffer: ByteBuffer): String {
        val writer = StringWriter()
        val channel = Channels.newChannel(BaseEncoding.base16().encodingStream(writer))
        channel.write(byteBuffer)
        return writer.toString()
    }
}
