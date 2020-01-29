package com.changlinli.raptorq

import java.net.InetSocketAddress
import java.util.*

sealed class ClientRequest {
    abstract val requestCode: RequestCode
}

data class FileDownloadRequest(val underlyingPacket: UdpPacket): ClientRequest() {
    // This should be read-only
    private val rawBytesOfPacket: ByteArray = underlyingPacket.bytes.underlyingArray

    val address: InetSocketAddress = underlyingPacket.address

    override val requestCode: RequestCode
        get() {
            assert(
                FileDownloadRequestCode.asByte == rawBytesOfPacket[0]
            ) {"This is a programmer bug! We created a FileDownloadRequest around a " +
                    "packet whose first byte does not signal a FileDownloadRequest (${FileDownloadRequestCode.asByte}) (it " +
                    "was instead ${rawBytesOfPacket[0]})"}
            return FileDownloadRequestCode
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
}

data class StopDownloadRequest(val underlyingPacket: UdpPacket): ClientRequest() {
    // This should be read-only
    private val rawBytesOfPacket: ByteArray = underlyingPacket.bytes.underlyingArray

    override val requestCode: com.changlinli.raptorq.RequestCode
        get() {
            assert(
                FileDownloadRequestCode.asByte == rawBytesOfPacket[0]
            ) {"This is a programmer bug! We created a StopDownloadRequest around a " +
                    "packet whose first byte does not signal a StopDownloadRequest (${StopDownloadRequestCode.asByte}) (it " +
                    "was instead ${rawBytesOfPacket[0]})"}
            return StopDownloadRequestCode
        }


}