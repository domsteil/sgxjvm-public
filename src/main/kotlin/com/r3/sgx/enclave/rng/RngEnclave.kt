package com.r3.sgx.enclave.rng

import com.r3.sgx.core.common.*
import com.r3.sgx.core.enclave.EnclaveApi
import com.r3.sgx.core.enclave.Enclavelet
import com.r3.sgx.core.enclave.EpidAttestationEnclaveHandler
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.MessageDigest

/**
 * A Random Number Generator [Enclavelet]. The enclave creates an EDDSA key-pair and puts the hash of the public key
 * into the enclave's report. It then uses [RngHandler] to communicate with clients, generating and signing random
 * numbers.
 *
 * The host-enclave communication is set up by the respective sides each building up a pipeline to process messages
 * flowing back and forth.
 *
 * Pipelines consist of a tree of [Handler]s. The root of the tree processes raw ECALLs/OCALLs (calls in and out of the
 * enclave). As the message propagates down the tree it gets demultiplexed/processed and possibly passed to a downstream
 * handler. This way we can compose different pieces of functionality (like attestation) horizontally, and add
 * intercepting functionality like encryption/decryption.
 *
 * At the bottom of the pipeline on both sides is a [RootHandler]. This [Handler] deals with exception handling and
 * provides a way to add downstream [Handler]s. Exceptions thrown are propagated back and rethrown on the calling side.
 *
 * In the case of an [Enclavelet] the downstream handlers added to [RootHandler] are
 * - [ChannelHandlingHandler]: allows the host to open/close channels to the enclave. Each new channel in turn has an
 *   associated downstream handler, created by [createHandler]. For [RngEnclave] this handler is [RngHandler].
 * - [EpidAttestationEnclaveHandler]: allows the host to request the report data from the enclave, to be embedded into
 *   an EPID attestation quote.
 *
 * @see Enclavelet
 * @see EpidAttestationEnclaveHandler
 * @see RngHandler
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
