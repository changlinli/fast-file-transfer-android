package com.changlinli.raptorq

import java.net.DatagramPacket
import java.net.InetSocketAddress

data class UdpPacket(val address: InetSocketAddress, val bytes: ImmutableByteArray) {
    fun toDatagramPacket(): DatagramPacket =
        DatagramPacket(bytes.unsafeUnderlyingArray, bytes.underlyingArrayOffset, bytes.size, address)
}