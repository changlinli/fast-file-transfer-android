package com.changlinli.raptorq

sealed class MessageType {
    abstract val asByte: Byte
}

sealed class RequestType(): MessageType() {
    abstract override val asByte: Byte
}

object FileDownloadRequestType: RequestType() {
    override val asByte: Byte = 27
}

object StopDownloadRequestType: RequestType() {
    override val asByte: Byte = 100
}

sealed class ResponseType(): MessageType() {
    abstract override val asByte: Byte

    companion object {
        // Purely a dev-time canary function
        // If you see a warning about an uncovered case here, you need to add that case to addByte
        @Suppress("unused")
        fun fromByteCanary(responseType: ResponseType): Unit =
            when (responseType) {
                FileFragmentType -> Unit
                ReceivedUploadRequestType -> Unit
                FileUUIDNotFoundType -> Unit
            }

        fun fromByte(byte: Byte): ResponseType? =
            if (byte == FileFragmentType.asByte) {
                FileFragmentType
            } else if (byte == FileUUIDNotFoundType.asByte) {
                FileUUIDNotFoundType
            } else if (byte == ReceivedUploadRequestType.asByte) {
                ReceivedUploadRequestType
            } else {
                null
            }

    }
}

object ReceivedUploadRequestType: ResponseType() {
    override val asByte: Byte = 2
}

object FileUUIDNotFoundType: ResponseType() {
    override val asByte: Byte = 1
}

object FileFragmentType: ResponseType() {
    override val asByte: Byte = 0

}