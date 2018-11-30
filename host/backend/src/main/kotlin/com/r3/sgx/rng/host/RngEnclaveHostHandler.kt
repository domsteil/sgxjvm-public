package com.r3.sgx.rng.host

import com.r3.sgx.core.common.BytesHandler
import com.r3.sgx.core.host.EnclaveletHostHandler
import com.r3.sgx.core.host.NativeHostApi
import java.nio.ByteBuffer
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.core.Response

class RngEnclaveHostHandler(private val hostConnected: EnclaveletHostHandler.Connected, private val async: AsyncResponse) : BytesHandler() {
    override fun receive(connected: Connected, input: ByteBuffer) {
        val randomBytesSize = input.int
        val randomBytes = input.readByteBuffer(randomBytesSize)

        val publicKeySize = input.int
        val publicKey = input.readByteBuffer(publicKeySize)

        val signatureSize = input.int
        val signature = input.readByteBuffer(signatureSize)

        val signedQuote = hostConnected.attestation.getQuote()

        val iasResponse = if (NativeHostApi.buildType == "Simulation") {
            getMockIasResponse()
        } else {
            // TODO add proper IAS roundtrip
            getMockIasResponse()
        }

        val response = GenerateResponse(
                iasCertificate = iasResponse.iasCertificate,
                iasSignature = iasResponse.iasSignature,
                iasResponse = iasResponse.iasResponse,
                generatedRandomBytes = Hex.byteBufferToHex(randomBytes),
                enclavePublicKey = Hex.byteBufferToHex(publicKey),
                enclaveSignature = Hex.byteBufferToHex(signature),
                signedQuote = signedQuote.toString()
        )
        async.resume(Response.ok(response).build())
    }
}

data class FormattedIasResponse(
        val iasCertificate: String,
        val iasSignature: String,
        val iasResponse: String
)

private fun getMockIasResponse(): FormattedIasResponse {
    return FormattedIasResponse(
            iasCertificate = "<Mock IAS certificate>",
            iasSignature = "<Mock IAS signature>",
            iasResponse = "<Mock IAS response>"
    )
}

private fun ByteBuffer.readByteBuffer(size: Int): ByteBuffer {
    val result = duplicate()
    result.limit(position() + size)
    position(position() + size)
    return result
}
