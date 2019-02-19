package com.r3.sgx.rng.client

import com.r3.sgx.enclavelethost.grpc.GetEpidAttestationResponse
import picocli.CommandLine
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(
        name = "verify-attestation",
        description = ["Reads and verifies attestation data from the standard input"],
        mixinStandardHelpOptions = true
)
class VerifyAttestationCommand : VerifyingCommand(), Callable<Unit> {

    override fun call() {
        val base64Quote = Base64.getDecoder().wrap(System.`in`)
        val attestation = GetEpidAttestationResponse.parseFrom(base64Quote).attestation
        verifyAttestation(attestation)
    }
}
