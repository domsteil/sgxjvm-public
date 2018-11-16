package com.r3.sgx.rng.host

import com.google.common.io.BaseEncoding
import com.r3.sgx.core.common.Cursor
import com.r3.sgx.core.common.SgxMetadata
import com.r3.sgx.core.common.SgxQuoteType
import com.r3.sgx.core.common.SgxSpid
import com.r3.sgx.core.host.EnclaveletHostHandler
import com.r3.sgx.core.host.EpidAttestationHostConfiguration
import com.r3.sgx.core.host.NativeHostApi
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class RngEnclaveHostApplication : Application<RngEnclaveHostConfiguration>() {

    override fun run(configuration: RngEnclaveHostConfiguration, environment: Environment) {
        val spid = BaseEncoding.base16().decode(configuration.spid)
        val attestationHostConfiguration = EpidAttestationHostConfiguration(
                quoteType = if (configuration.isQuoteLinkable) SgxQuoteType.LINKABLE.value else SgxQuoteType.UNLINKABLE.value,
                spid = Cursor(SgxSpid, ByteBuffer.wrap(spid))
        )
        environment.jersey().apply {
            register(RngEnclaveHostComponent(attestationHostConfiguration))
        }
    }

    override fun initialize(bootstrap: Bootstrap<RngEnclaveHostConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/static", "/", "index.html"))
    }
}
