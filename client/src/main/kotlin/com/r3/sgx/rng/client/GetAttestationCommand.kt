package com.r3.sgx.rng.client

import picocli.CommandLine
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(
        name = "get-attestation",
        description = ["Retrieves attestation data from a running enclavelet host"],
        mixinStandardHelpOptions = true
)
class GetAttestationCommand : Callable<Unit> {
    @CommandLine.Parameters(
            index = "0",
            description = ["The address of the RNG enclavelet host"]
    )
    var hostAddress: String = "localhost:8080"

    override fun call() {
        RngEnclaveletHostClient.withClient(hostAddress) { client ->
            val quote = client.getAttestation()
            val base64Quote = Base64.getEncoder().encode(quote.toByteArray())
            System.out.print(String(base64Quote))
        }
    }
}
