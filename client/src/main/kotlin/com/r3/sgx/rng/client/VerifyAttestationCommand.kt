package com.r3.sgx.rng.client

import com.r3.sgx.core.common.Cursor
import com.r3.sgx.core.common.SgxQuote
import com.r3.sgx.enclavelethost.client.EpidAttestationVerification
import com.r3.sgx.enclavelethost.grpc.EpidAttestation
import com.r3.sgx.enclavelethost.grpc.GetEpidAttestationResponse
import picocli.CommandLine
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(
        name = "verify-attestation",
        description = ["Verifies the attestation data"],
        mixinStandardHelpOptions = true
)
class VerifyAttestationCommand : VerifyingCommand(), Callable<Unit> {

    override fun call() {
        val base64Quote = Base64.getDecoder().wrap(System.`in`)
        val attestation = GetEpidAttestationResponse.parseFrom(base64Quote).attestation
        verifyAttestation(attestation)
    }
}

open class VerifyingCommand {
    @CommandLine.Option(
            names = ["--accept-debug"],
            description = ["Accept quotes from enclaves loaded in DEBUG mode"]
    )
    var acceptDebug: Boolean = false

    @CommandLine.Option(
            names = ["--accept-group-out-of-date"],
            description = ["Accept attestation service responses with status GROUP_OUT_OF_DATE"]
    )
    var acceptGroupOutOfDate: Boolean = false

    @CommandLine.Option(
            names = ["--accept-configuration-needed"],
            description = ["Accept attestation service responses with status CONFIGURATION_NEEDED"]
    )
    var acceptConfigurationNeeded: Boolean = false

    fun verifyAttestation(attestation: EpidAttestation): Cursor<ByteBuffer, SgxQuote> {
        val verification = EpidAttestationVerification()
        val intelPkix = verification.loadIntelPkix()
        verification.acceptDebug = acceptDebug
        verification.acceptGroupOutOfDate = acceptGroupOutOfDate
        verification.acceptConfigurationNeeded = acceptConfigurationNeeded
        return verification.verify(intelPkix, attestation)
    }
}

