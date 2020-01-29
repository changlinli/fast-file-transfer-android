package com.changlinli.raptorq

import java.net.InetSocketAddress

data class UdpPacket(val address: InetSocketAddress, val bytes: ImmutableByteArray)