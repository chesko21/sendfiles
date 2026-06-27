package com.chesko.sendfiles.network

import java.net.InetAddress

data class Device(
    val name: String,
    val host: InetAddress,
    val port: Int
)
