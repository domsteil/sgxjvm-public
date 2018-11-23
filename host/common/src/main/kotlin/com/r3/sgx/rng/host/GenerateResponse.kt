package com.r3.sgx.rng.host

data class EncoderTree(
        val typeName: String,
        val size: Int,
        val children: List<EncoderTree>
)

data class GenerateResponse(
        val iasCertificate: String,
        val iasSignature: String,
        val iasResponse: String,
        val signedQuote: String,
        val enclavePublicKey: String,
        val enclaveSignature: String,
        val generatedRandomBytes: String
)
