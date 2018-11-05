package com.r3.sgx.enclave.rng

import com.r3.sgx.core.common.*
import com.r3.sgx.core.enclave.EnclaveApi
import com.r3.sgx.core.enclave.Enclavelet
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.MessageDigest

/**
 * A Random Number Generator [Enclavelet]. It creates an EDDSA key-pair and puts the hash of the public key into the
 * enclave's report. Clients are then handled by [RngHandler]
 */
class RngEnclave : Enclavelet() {
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

