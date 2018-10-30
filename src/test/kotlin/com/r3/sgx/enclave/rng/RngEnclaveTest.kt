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
        val handler = BytesRecordingHandler()

        val configuration = EpidAttestationHostConfiguration(
                quoteType = SgxQuoteType32.LINKABLE,
                spid = Cursor.allocate(SgxSpid)
        )

        val connected = enclave.connected
        val channels = connected.addDownstream(ChannelInitiatingHandler())
        val channel = channels.addDownstream(0, handler)
        val attesting = connected.addDownstream(EpidAttestationHostHandler(configuration))
        val quote = attesting.getQuote()
        val hashedKey = quote[quote.type.quote][SgxQuote.reportBody][SgxReportBody.reportData].read()

        val message = ByteBuffer.allocate(4)
        val requestedRandomBytesSize = 256
        message.putInt(requestedRandomBytesSize)
        message.rewind()
        channel.send(message)

        // Get bytes
        assertEquals(1, handler.ocalls.size)
        val responseBytes = handler.ocalls.first()
        val randomBytesSize = responseBytes.getInt()
        assertEquals(requestedRandomBytesSize, randomBytesSize)
        val randomBytes = ByteArray(randomBytesSize)
        responseBytes.get(randomBytes)

        // Get and check key
        val publicKeySize = responseBytes.getInt()
        val publicKey = ByteArray(publicKeySize)
        responseBytes.get(publicKey)
        val keyDigest = MessageDigest.getInstance("SHA-512").digest(publicKey)
        assertEquals(hashedKey, ByteBuffer.wrap(keyDigest))

        // Get and check signature
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
    }
}
