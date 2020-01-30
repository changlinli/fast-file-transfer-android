package com.changlinli.raptorq

import net.fec.openrq.EncodingPacket
import net.fec.openrq.OpenRQ
import net.fec.openrq.decoder.DataDecoder
import net.fec.openrq.parameters.FECParameters
import java.net.InetSocketAddress

sealed class ServerResponse {
    companion object {
        fun decode(udpPacket: UdpPacket): ServerResponse? {
            val responseType = udpPacket.bytes.get(0)?.let {
                ResponseType.fromByte(it)
            }
            return when (responseType) {
                FileFragmentType -> FileFragment(udpPacket)
                ReceivedUploadRequestType -> TODO()
                FileUUIDNotFoundType -> TODO()
                null -> null
            }
        }
    }
}

data class FileFragment(val underlyingPacket: UdpPacket): ServerResponse() {
    fun toEncodingPacketWithDecoder(dataDecoder: DataDecoder): EncodingPacket =
        EncodingPacket.parsePacket(dataDecoder, underlyingPacket.bytes.unsafeToByteBuffer(), false).value()

    fun toEncodingPacket(fecParameters: FECParameters): EncodingPacket =
        toEncodingPacketWithDecoder(OpenRQ.newDecoder(fecParameters, 5))

    companion object {
        fun encode(inetSocketAddress: InetSocketAddress, encodingPacket: EncodingPacket): FileFragment =
            FileFragment(UdpPacket(inetSocketAddress, ImmutableByteArray.unsafeFromArray(encodingPacket.asArray())))
    }
}