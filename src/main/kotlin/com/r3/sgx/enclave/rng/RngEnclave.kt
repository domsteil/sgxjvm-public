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
 * The host-enclave communication is set up by the respective sides each building up a tree of [Handler]s to process
 * messages flowing back and forth.
 *
 * The root of the tree processes raw ECALLs/OCALLs (calls in and out of the enclave). As the message propagates down
 * the tree it gets demultiplexed/processed and possibly passed to a downstream handler. This way we can compose
 * different pieces of functionality (like attestation) horizontally, and add intercepting functionality like
 * encryption/decryption. When a [Handler] is receiving a message it furthermore has access to a [Sender] which can
 * send messages the other way.
 *
 * As an example we may have the following [Handler] structure:
 *
 *   Host                                Enclave
 *
 * A--\                                      /--A'
 *    |                                      |
 * B--E---\                             /----E'-B'
 *    |   |                             |    |
 * C--/   F--root ~ ECALL/OCALL ~ root--F'   \--C'
 *        |                             |
 * D------/                             \-------D'
 *
 * Here we have handlers A,B,C,D,E and F on the host side, and corresponding ones on the other. When say A wants to send
 * something to the enclave it will call through the [Handler]/[Sender] tree where each component will serialize its
 * part of the message. For example we may end up with a message like this:
 *
 * +----------+
 * | F header |
 * +----------+
 * | E header |
 * +----------+
 * |  A body  |
 * |   ...    |
 * +----------+
 *
 * When the message is sent it will first be handled by F' which will deserialize its part and forward it to its
 * downstream E', then A', etc.
 *
 * In general at the root of the tree on both sides is a [RootHandler]. This [Handler] deals with exception handling and
 * provides a way to add downstream [Handler]s the messages of which it will mux automatically. Exceptions thrown are
 * propagated back and rethrown on the calling side.
 *
 * In the case of an [Enclavelet] the downstream handlers added to [RootHandler] are
 * - [ChannelHandlingHandler]: allows the host to open/close channels to the enclave. Each new channel in turn has an
 *   associated downstream handler, created by [createHandler]. For [RngEnclave] this handler is [RngHandler].
 * - [EpidAttestationEnclaveHandler]: allows the host to request the report data from the enclave, to be embedded into
 *   an EPID attestation quote.
 *
 * This is the structure that a host of an [Enclavelet] must mirror with [ChannelInitiatingHandler] and
 * [EpidAttestationHostHandler] respectively.
 *
 * Most of the above complexity is hidden behind the [Enclavelet] abstract class. The two functions to implement are:
 * - [createReportData]: the piece of data the enclave provides to be included in an attestation report/quote.
 * - [createHandler]: a factory for [Handler]s that handle incoming connections to the enclave.
 */
class RngEnclave : Enclavelet() {
    private lateinit var signatureScheme: SignatureScheme
    private lateinit var keyPair: KeyPair

    override fun createReportData(api: EnclaveApi): Cursor<ByteBuffer, SgxReportData> {
        // Generate a key pair
        signatureScheme = api.signatureSchemeFactory.make(SchemesSettings.EDDSA_ED25519_SHA512)
        keyPair = signatureScheme.generateKeyPair()
        // Hash the public key, to be included in the report
        val keyDigest = MessageDigest.getInstance("SHA-512").digest(keyPair.public.encoded)
        // Construct a typed cursor over the bytes. SgxReportData is an [Encoder] which describes a JVM view over the
        // corresponding native C++ structure.
        return Cursor(SgxReportData, ByteBuffer.wrap(keyDigest))
    }

    override fun createHandler(api: EnclaveApi): Handler<*> {
        return RngHandler(keyPair, signatureScheme, api)
    }
}
