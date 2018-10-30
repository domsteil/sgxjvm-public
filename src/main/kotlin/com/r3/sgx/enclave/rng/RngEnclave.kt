package com.r3.sgx.enclave.rng

import com.r3.sgx.core.common.*
import com.r3.sgx.core.enclave.EnclaveApi
import com.r3.sgx.core.enclave.EnclaveletEnclave
import com.r3.sgx.core.common.SchemesSettings
import com.r3.sgx.core.common.SignatureScheme
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.MessageDigest
import kotlin.math.min

class RngEnclave : EnclaveletEnclave() {
    private lateinit var signatureScheme: SignatureScheme
    private lateinit var keyPair: KeyPair

    override fun createReportData(api: EnclaveApi): Cursor<ByteBuffer, SgxReportData> {
        signatureScheme = api.signatureSchemeFactory.make(SchemesSettings.EDDSA_ED25519_SHA512)
        keyPair = signatureScheme.generateKeyPair()
        val keyDigest = MessageDigest.getInstance("SHA-512").digest(keyPair.public.encoded)
        return Cursor(SgxReportData, ByteBuffer.wrap(keyDigest))
    }

    override fun createHandler(api: EnclaveApi): Handler<*> {
        return RngHandler(keyPair, signatureScheme, api)
    }
}

class RngHandler(val keyPair: KeyPair, val signatureScheme: SignatureScheme, val api: EnclaveApi): Handler<Sender> {
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
