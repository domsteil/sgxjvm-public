package com.r3.sgx.rng.client

import com.r3.sgx.core.common.Cursor
import com.r3.sgx.core.common.SgxQuote
import com.r3.sgx.enclavelethost.client.EpidAttestationVerificationBuilder
import com.r3.sgx.enclavelethost.client.measurement.MeasurementTrust
import com.r3.sgx.enclavelethost.grpc.EpidAttestation
import picocli.CommandLine
import java.io.File
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

    @CommandLine.Option(
            names = ["-t", "--trust-file"],
            description = ["Optional yaml file containing trusted enclave measurements. By default all measurements are accepted."]
    )
    var trustedMeasurementsFile: File? = null

    private val trustedMeasurements get() =
        trustedMeasurementsFile?.let {
            MeasurementTrust.load(it).get("com.r3.sgx.rng.enclave.RngEnclave")
        } ?: MeasurementTrust.All

    fun verifyAttestation(attestation: EpidAttestation): Cursor<ByteBuffer, SgxQuote> {
        val verification = EpidAttestationVerificationBuilder(trustedMeasurements)
                .withAcceptConfigurationNeeded(acceptConfigurationNeeded)
                .withAcceptGroupOutOfDate(acceptGroupOutOfDate)
                .withAcceptDebug(acceptDebug)
                .build()
        val intelPkix = verification.loadIntelPkix()
        return verification.verify(intelPkix, attestation)
    }
}