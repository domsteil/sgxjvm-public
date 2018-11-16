package com.r3.sgx.rng.host

import com.r3.sgx.core.common.SgxMetadata
import com.r3.sgx.core.host.EnclaveHandle
import com.r3.sgx.core.host.EnclaveletHostHandler
import com.r3.sgx.core.host.EpidAttestationHostConfiguration
import com.r3.sgx.core.host.NativeHostApi
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
class RngEnclaveHostComponent(val attestationHostConfiguration: EpidAttestationHostConfiguration) {

    private val channelId = AtomicInteger(0)
    private var enclave = loadEnclave()

    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Synchronized
    fun generate(@Suspended async: AsyncResponse) {
        val handler = RngEnclaveHostHandler(enclave.connected, async)
        val channel = enclave.connected.channels.addDownstream(channelId.getAndIncrement(), handler)

        val message = ByteBuffer.allocate(4)
        val requestedRandomBytesSize = 1024
        message.putInt(requestedRandomBytesSize)
        message.rewind()
        try {
            channel.send(message)
        } catch (exception: Exception) {
            Response.serverError().entity(exception.message).build()
        }
    }

    @Path("/reload")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Synchronized
    fun reload(): Response {
        NativeHostApi.destroyEnclave(enclave.enclaveId)
        enclave = loadEnclave()
        return Response.ok().build()
    }

    private fun loadEnclave(): EnclaveHandle<EnclaveletHostHandler.Connected> {
        val enclaveletHandler = EnclaveletHostHandler(attestationHostConfiguration)
        return withTemporaryUnpackOfResource(Paths.get("enclave/enclave.signed.so")) {
            val metadata = NativeHostApi.readMetadata(it)
            log.info("Loading enclave...")
            log.info("Enclave's sigstruct:  ${metadata[SgxMetadata.enclaveCss]}")
            log.info("Enclave's attributes: ${metadata[SgxMetadata.attributes]}")
            NativeHostApi.createEnclave(enclaveletHandler, it, isDebug = true)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(RngEnclaveHostHandler::class.java)

        fun <A> withTemporaryUnpackOfResource(resourcePath: java.nio.file.Path, block: (File) -> A): A {
            val temporaryDirectory = File(System.getProperty("java.io.tmpdir"), "com.r3.sgx.rng.host")
            temporaryDirectory.mkdir()
            temporaryDirectory.deleteOnExit()
            try {
                val destination = File(temporaryDirectory, resourcePath.fileName.toString())
                val stream = RngEnclaveHostApplication::class.java.classLoader.getResourceAsStream(resourcePath.toString())
                if (stream == null) {
                    throw Exception("Can't find enclave at $resourcePath")
                }
                Files.copy(stream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                return block(destination)
            } finally {
                temporaryDirectory.deleteRecursively()
            }
        }
    }
}
