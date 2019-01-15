package com.r3.sgx.rng.client

import com.google.protobuf.ByteString
import com.r3.sgx.enclavelethost.grpc.*
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class RngEnclaveletHostClient(private val stub: EnclaveletHostGrpc.EnclaveletHostStub) {
    companion object {
        fun <A> withClient(hostAddress: String, block: (RngEnclaveletHostClient) -> A): A {
            val channel = ManagedChannelBuilder.forTarget(hostAddress).usePlaintext().build()
            try {
                val stub = EnclaveletHostGrpc.newStub(channel)
                return block(RngEnclaveletHostClient(stub.withWaitForReady().withCompression("gzip")))
            } finally {
                channel.shutdownNow()
            }
        }
    }

    fun getAttestation(): GetEpidAttestationResponse {
        val blocking = BlockingObserver<GetEpidAttestationResponse>()
        stub.getEpidAttestation(GetEpidAttestationRequest.getDefaultInstance(), blocking)
        return blocking.getNext()
    }

    fun getRandomBytes(): RngResponse {
        val message = ByteBuffer.allocate(4)
        message.putInt(1024)
        message.rewind()

        val blocking = BlockingObserver<ServerMessage>()
        val observer = stub.openSession(blocking)
        val clientMessage = ClientMessage.newBuilder()
                .setBlob(ByteString.copyFrom(message))
                .build()
        observer.onNext(clientMessage)
        val rawResponse = blocking.getNext().blob.asReadOnlyByteBuffer()
        return RngResponse.fromRawResponse(rawResponse)
    }
}

class RngResponse(
        val randomBytes: ByteArray,
        val publicKey: ByteArray,
        val signature: ByteArray
) {
    companion object {
        fun fromRawResponse(rawResponse: ByteBuffer): RngResponse {
            val bytesSize = rawResponse.getInt()
            val bytes = ByteArray(bytesSize)
            rawResponse.get(bytes)

            val keySize = rawResponse.getInt()
            val key = ByteArray(keySize)
            rawResponse.get(key)

            val signatureSize = rawResponse.getInt()
            val signature = ByteArray(signatureSize)
            rawResponse.get(signature)

            return RngResponse(bytes, key, signature)
        }
    }
}

class BlockingObserver<A> : StreamObserver<A> {
    private sealed class Try<A> {
        class Error<A>(val throwable: Throwable) : Try<A>()
        class Value<A>(val value: A): Try<A>()
    }

    fun getNext(): A {
        val element = queue.take()
        when (element) {
            is Try.Error -> throw element.throwable
            is Try.Value -> return element.value
        }
    }

    private val queue = LinkedBlockingQueue<Try<A>>()

    override fun onNext(value: A) {
        queue.add(Try.Value(value))
    }

    override fun onError(throwable: Throwable) {
        queue.add(Try.Error(throwable))
    }

    override fun onCompleted() {
        queue.add(Try.Error(IllegalArgumentException("Stream was closed")))
    }
}