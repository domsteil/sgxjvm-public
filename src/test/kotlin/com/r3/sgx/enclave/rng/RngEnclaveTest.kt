package com.r3.sgx.enclave.rng

import com.r3.sgx.core.common.*
import com.r3.sgx.core.host.*
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
    private val enclaveFile = File(System.getProperty("com.r3.sgx.enclave.path"))

    private lateinit var enclave: EnclaveHandle<RootHandler.Connected>

    @Before
    fun setup() {
        enclave = NativeHostApi.createEnclave(RootHandler(), enclaveFile)
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
        val attestationConfiguration = EpidAttestationHostConfiguration(
                quoteType = SgxQuoteType32.LINKABLE,
                spid = Cursor.allocate(SgxSpid)
        )

        // Construct the host side of an Enclavelet, with channel and EPID attestation support. This mirrors
        // RngEnclave's handler tree.
        val root: RootHandler.Connected = enclave.connected
        val channels: ChannelInitiatingHandler.Connected = root.addDownstream(ChannelInitiatingHandler())
        val attestation: EpidAttestationHostHandler.Connected = root.addDownstream(EpidAttestationHostHandler(attestationConfiguration))

        // Create a quote, including the enclave's report data created by RngEnclave.createReportData and signed by the
        // Quoting Enclave. Note that this test does *not* do a full attestation roundtrip to the Intel Attestation
        // Service!
        // This is because by default we're using a Simulation enclave that doesn't produce proper quotes. To get a
        // proper quote we need to use a Debug or a Release enclave. Furthermore we need a whitelisted TLS key to talk
        // to the IAS and verify such a quote.
        // Without doing the above roundtrip the enclave is not to be trusted! This test is for demonstration purposes
        // only.
        val quote: Cursor<ByteBuffer, SgxSignedQuote> = attestation.getQuote()
        val reportBody: Cursor<ByteBuffer, SgxReportBody> = quote[quote.type.quote][SgxQuote.reportBody]

        // Get a view on the report data created by RngEnclave.createReportData
        val hashedEnclaveKey = reportBody[SgxReportBody.reportData].read()

        // Check that the measurement matches the enclave's that we wanted to load
        // First read the metadata from the enclave binary
        val metadata: Cursor<ByteBuffer, SgxMetadata> = NativeHostApi.readMetadata(enclaveFile)
        val measurementInMetadata = metadata[SgxMetadata.enclaveCss][SgxEnclaveCss.body][SgxCssBody.enclaveHash]
        // Now get the measurement from the quote we created
        val measurement = reportBody[SgxReportBody.measurement]
        assertEquals(measurementInMetadata, measurement)

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
