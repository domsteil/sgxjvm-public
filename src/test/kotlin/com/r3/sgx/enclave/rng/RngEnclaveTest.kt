package com.r3.sgx.enclave.rng

import com.r3.sgx.core.common.*
import com.r3.sgx.core.host.EnclaveHandle
import com.r3.sgx.core.host.EpidAttestationHostConfiguration
import com.r3.sgx.core.host.EpidAttestationHostHandler
import com.r3.sgx.core.host.NativeHostApi
import com.r3.sgx.core.host.internal.Native
import com.r3.sgx.testing.BytesRecordingHandler
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.test.assertEquals

class RngEnclaveTest {
    // The SGX gradle plugin sets this property to the path of the built+signed enclave.
    private val enclavePath = System.getProperty("com.r3.sgx.enclave.path")

    private lateinit var enclave: EnclaveHandle<RootHandler.Connected>

    @Before
    fun setup() {
        enclave = NativeHostApi.createEnclave(RootHandler(), File(enclavePath))
    }

    @After
    fun shutdown() {
        Native.destroyEnclave(enclave.enclaveId)
    }

    @Test
    fun rngEnclaveWorks() {
        // A channel handler that records the binary blobs sent by the enclave.
        val handler = BytesRecordingHandler()

        // Configure the host side of attestation. Insert your SPID/quote type if you want to test using a
        // non-Simulation enclave.
        val configuration = EpidAttestationHostConfiguration(
                quoteType = SgxQuoteType32.LINKABLE,
                spid = Cursor.allocate(SgxSpid)
        )

        // Construct the host side of an Enclavelet, with channel and EPID attestation support
        val connected = enclave.connected
        val channels = connected.addDownstream(ChannelInitiatingHandler())
        val attesting = connected.addDownstream(EpidAttestationHostHandler(configuration))

        // Create a quote, including the enclave's report data created by RngEnclave.createReportData, and signed by the
        // Quoting Enclave. Note that this test does *not* do a full attestation roundtrip to the Intel Attestation
        // Service!
        val quote = attesting.getQuote()
        val hashedEnclaveKey = quote[quote.type.quote][SgxQuote.reportBody][SgxReportBody.reportData].read()

        // Open a channel to the enclave, and send a request for a random sequence of bytes of size 256.
        val channel = channels.addDownstream(0, handler)
        val message = ByteBuffer.allocate(4)
        val requestedRandomBytesSize = 256
        message.putInt(requestedRandomBytesSize)
        message.rewind()
        channel.send(message)

        // Get the enclave's OCALL reply from the handler. Note that the channel send is synchronous, the enclave will
        // reply in the same callchain. I.e. the execution stack at the time of the reply will look something like this:
        //
        // HOST:    rngEnclaveWorks    -- this test
        // HOST:    channel.send       -- the above send
        // HOST:    ecall_host_side    -- internals of send, which translates to an ECALL
        // =========================== -- host-enclave boundary
        // ENCLAVE: ecall_enclave_side -- internals of receive in the enclave
        // ENCLAVE: RngHandler.receive -- the receive function in RngHandler, generating/signing the random numbers
        // ENCLAVE: connected.send     -- the enclave is replying here
        // ENCLAVE: ocall_enclave_side -- internals of send, which translates to an OCALL
        // =========================== -- enclave-host boundary
        // HOST:    ocall_host_side    -- internals of receive in the host
        // HOST:    handler.receive    -- the receive function in the handler we created at the beginning of the test
        //
        // Therefore when the above send() returns we will have already received the reply, recorded in handler.ocalls.
        assertEquals(1, handler.ocalls.size)
        val responseBytes = handler.ocalls.first()
        val randomBytesSize = responseBytes.getInt()
        assertEquals(requestedRandomBytesSize, randomBytesSize)
        val randomBytes = ByteArray(randomBytesSize)
        responseBytes.get(randomBytes)

        // Get the enclave's public key and check it against the hash in the report.
        val publicKeySize = responseBytes.getInt()
        val publicKey = ByteArray(publicKeySize)
        responseBytes.get(publicKey)
        val keyDigest = MessageDigest.getInstance("SHA-512").digest(publicKey)
        assertEquals(hashedEnclaveKey, ByteBuffer.wrap(keyDigest))

        // Get the enclave's signature over the random bytes and check its correctness.
        val signatureSize = responseBytes.getInt()
        val signature = ByteArray(signatureSize)
        responseBytes.get(signature)
        val signatureSchemeFactory = NativeHostApi.getSignatureSchemeFactory(SecureRandom.getInstance("SHA1PRNG"))
        val eddsaScheme = signatureSchemeFactory.make(SchemesSettings.EDDSA_ED25519_SHA512)
        eddsaScheme.verify(
                publicKey = eddsaScheme.decodePublicKey(publicKey),
                signatureData = signature,
                clearData = randomBytes
        )

        // Close the channel.
        channels.removeDownstream(0)
    }
}
