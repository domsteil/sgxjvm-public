package com.r3.sgx.rng.client

import com.r3.sgx.core.common.SchemesSettings
import com.r3.sgx.core.common.SgxQuote
import com.r3.sgx.core.common.SgxReportBody
import com.r3.sgx.enclavelethost.client.Crypto
import picocli.CommandLine
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(
        name = "get-random",
        description = ["Retrieve random numbers"],
        mixinStandardHelpOptions = true
)
class GetRandomCommand : VerifyingCommand(), Callable<Unit> {
    @CommandLine.Parameters(
            index = "0",
            description = ["The address of the RNG enclavelet host"]
    )
    var hostAddress: String = "localhost:8080"

    override fun call() {
        RngEnclaveletHostClient.withClient(hostAddress) { client ->
            val attestation = client.getAttestation().attestation
            val quote = verifyAttestation(attestation)

            val rngResponse = client.getRandomBytes()
            val keyHash = MessageDigest.getInstance("SHA-512").digest(rngResponse.publicKey)
            val keyHashInReport = quote[SgxQuote.reportBody][SgxReportBody.reportData].read()
            if (ByteBuffer.wrap(keyHash) != keyHashInReport) {
                throw GeneralSecurityException("Key hash in attestation report doesn't match the hash of the claimed enclave key")
            }

            val signatureSchemeFactory = Crypto.getSignatureSchemeFactory(SecureRandom.getInstance("SHA1PRNG"))
            val eddsaScheme = signatureSchemeFactory.make(SchemesSettings.EDDSA_ED25519_SHA512)
            eddsaScheme.verify(
                    eddsaScheme.decodePublicKey(rngResponse.publicKey),
                    rngResponse.signature,
                    rngResponse.randomBytes
            )
            val base64RandomBytes = Base64.getEncoder().encode(rngResponse.randomBytes)
            print(String(base64RandomBytes))
        }
    }
}