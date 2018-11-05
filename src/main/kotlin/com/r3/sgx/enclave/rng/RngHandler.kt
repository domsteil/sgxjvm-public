package com.r3.sgx.enclave.rng

import com.r3.sgx.core.common.Handler
import com.r3.sgx.core.common.Sender
import com.r3.sgx.core.common.SignatureScheme
import com.r3.sgx.core.enclave.EnclaveApi
import java.nio.ByteBuffer
import java.security.KeyPair
import kotlin.math.min

/**
 * The [Handler] of [RngEnclave] clients.
 *
 * It receives requests for random numbers consisting of a single integer no larger than 1024. Then it generates a
 * random sequence of bytes of this size, signs the bytes, and sends back a reply including the random numbers, the
 * enclave's public key, and the signature itself.
 */
class RngHandler(
        private val keyPair: KeyPair,
        private val signatureScheme: SignatureScheme,
        private val api: EnclaveApi
): Handler<Sender> {
    override fun receive(connected: Sender, input: ByteBuffer) {
        val sizeRequested = input.getInt()
        val randomBytesSize = min(sizeRequested, 1024)
        val randomBytes = ByteArray(randomBytesSize)
        api.getRandomBytes(randomBytes, 0, randomBytesSize)
        val signature = signatureScheme.sign(keyPair.private, randomBytes)
        val publicKey = keyPair.public.encoded
        // We could use something like protobuf, but for the sake of reducing build dependencies we do custom serialization
        val size = 4 + randomBytesSize + 4 + publicKey.size + 4 + signature.size
        connected.sendTopLevel(size) {
            it.putInt(randomBytesSize)
            it.put(randomBytes)
            it.putInt(publicKey.size)
            it.put(publicKey)
            it.putInt(signature.size)
            it.put(signature)
        }
    }

    override fun connect(upstream: Sender): Sender {
        return upstream
    }
}
