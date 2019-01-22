package com.r3.sgx.rng.client

import com.r3.sgx.core.common.Cursor
import com.r3.sgx.core.common.SgxQuote
import com.r3.sgx.enclavelethost.client.EpidAttestationVerificationBuilder
import com.r3.sgx.enclavelethost.grpc.EpidAttestation
import picocli.CommandLine
import java.nio.ByteBuffer

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
        val verification = EpidAttestationVerificationBuilder()
                .withAcceptConfigurationNeeded(acceptConfigurationNeeded)
                .withAcceptGroupOutOfDate(acceptGroupOutOfDate)
                .withAcceptDebug(acceptDebug)
                .build()
        val intelPkix = verification.loadIntelPkix()
        return verification.verify(intelPkix, attestation)
    }
}