package com.r3.sgx.rng.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.r3.sgx.core.common.Cursor
import com.r3.sgx.core.common.SgxQuote
import com.r3.sgx.enclavelethost.client.ias.ReportResponse
import com.r3.sgx.enclavelethost.grpc.GetEpidAttestationResponse
import picocli.CommandLine
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Callable

@CommandLine.Command(
        name = "print-attestation",
        description = ["Prints the attestation data in human-readable form"],
        mixinStandardHelpOptions = true
)
class PrintAttestationCommand : Callable<Unit> {
    override fun call() {
        val base64Quote = Base64.getDecoder().wrap(System.`in`)
        val attestation = GetEpidAttestationResponse.parseFrom(base64Quote).attestation

        println("IAS Certificate:")
        println(attestation.iasCertificate)

        println("IAS Response:")
        val objectMapper = ObjectMapper()
        val iasResponse = objectMapper.readValue<ReportResponse>(attestation.iasResponse.toByteArray(), ReportResponse::class.java)
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
        val stringWriter = StringWriter()
        objectMapper.writeValue(stringWriter, iasResponse)
        println(stringWriter.toString())

        println("IAS Signature:")
        val base64Signature = Base64.getEncoder().encode(attestation.iasSignature.asReadOnlyByteBuffer())
        System.out.println(StandardCharsets.UTF_8.decode(base64Signature))

        println("Quote body:")
        val quote = Cursor(SgxQuote, ByteBuffer.wrap(iasResponse.isvEnclaveQuoteBody))
        println(quote)
    }
}

