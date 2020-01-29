package com.changlinli.raptorq

sealed class RequestCode() {
    abstract val asByte: Byte
}

object FileDownloadRequestCode: RequestCode() {
    override val asByte: Byte = 27
}

object StopDownloadRequestCode: RequestCode() {
    override val asByte: Byte = 1
}