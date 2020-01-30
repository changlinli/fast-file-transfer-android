package com.changlinli.raptorq

import java.net.InetSocketAddress
import java.util.*

sealed class ClientRequest {
    abstract val requestType: RequestType
}

data class FileDownloadRequest(val underlyingPacket: UdpPacket): ClientRequest() {
    // This should be read-only
    private val rawBytesOfPacket: ByteArray = underlyingPacket.bytes.unsafeUnderlyingArray

    val address: InetSocketAddress = underlyingPacket.address

    override val requestType: RequestType
        get() {
            assert(
                FileDownloadRequestType.asByte == rawBytesOfPacket[0]
            ) {"This is a programmer bug! We created a FileDownloadRequest around a " +
                    "packet whose first byte does not signal a FileDownloadRequest (${FileDownloadRequestType.asByte}) (it " +
                    "was instead ${rawBytesOfPacket[0]})"}
            return FileDownloadRequestType
        }

    fun fileUUID(): UUID {
        val byteBuffer = java.nio.ByteBuffer.wrap(rawBytesOfPacket)
        // Ignore the first byte, which just signals what kind of packet this is
        val mostSignificantBits = byteBuffer.getLong(1)
        val leastSignificantBits = byteBuffer.getLong(1 + 8)
        return UUID(mostSignificantBits, leastSignificantBits)
    }

    override fun toString(): String {
        return "FileDownloadRequest(fileUUID: ${fileUUID()}, address: $address)"
    }

    companion object {
        fun createFileRequest(remote: InetSocketAddress, fileUUID: UUID): FileDownloadRequest {
            val sizeOfArray = 1 + 16
            val underlyingArray = ByteArray(sizeOfArray)
            val byteBuffer = java.nio.ByteBuffer.wrap(underlyingArray)
            byteBuffer.put(FileDownloadRequestType.asByte)
            byteBuffer.putLong(fileUUID.mostSignificantBits)
            byteBuffer.putLong(fileUUID.leastSignificantBits)
            val packet = UdpPacket(remote, ImmutableByteArray.unsafeFromArray(underlyingArray))
            return FileDownloadRequest(packet)
        }
    }
}

data class StopDownloadRequest(val underlyingPacket: UdpPacket): ClientRequest() {
    // This should be read-only
    private val rawBytesOfPacket: ByteArray = underlyingPacket.bytes.unsafeUnderlyingArray

    override val requestType: com.changlinli.raptorq.RequestType
        get() {
            assert(
                FileDownloadRequestType.asByte == rawBytesOfPacket[0]
            ) {"This is a programmer bug! We created a StopDownloadRequest around a " +
                    "packet whose first byte does not signal a StopDownloadRequest (${StopDownloadRequestType.asByte}) (it " +
                    "was instead ${rawBytesOfPacket[0]})"}
            return StopDownloadRequestType
        }

    companion object {
        fun createStopDownloadRequest(remote: InetSocketAddress, fileUUID: UUID): StopDownloadRequest {
            val arraySize = 1 + 16
            val underlyingArray = ByteArray(arraySize)
            val byteBuffer = java.nio.ByteBuffer.wrap(underlyingArray)
            byteBuffer.put(StopDownloadRequestType.asByte)
            byteBuffer.putLong(fileUUID.mostSignificantBits)
            byteBuffer.putLong(fileUUID.leastSignificantBits)
            val packet = UdpPacket(remote, ImmutableByteArray.unsafeFromArray(underlyingArray))
            return StopDownloadRequest(packet)
        }
    }

}