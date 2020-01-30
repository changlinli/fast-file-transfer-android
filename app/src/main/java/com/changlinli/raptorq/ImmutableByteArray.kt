package com.changlinli.raptorq

import java.nio.ByteBuffer

class ImmutableByteArray(val unsafeUnderlyingArray: ByteArray, val underlyingArrayOffset: Int, val size: Int) {
    companion object {
        fun fromArray(array: ByteArray): ImmutableByteArray = fromArray(array, 0, array.size)

        fun fromArray(array: ByteArray, offset: Int, length: Int): ImmutableByteArray {
            val newArray = ByteArray(length)
            System.arraycopy(array, offset, newArray, 0, length)
            return ImmutableByteArray(newArray, offset, length)
        }

        fun unsafeFromArray(array: ByteArray): ImmutableByteArray =
            ImmutableByteArray(array, 0, array.size)

        fun unsafeFromArray(array: ByteArray, offset: Int, size: Int) =
            ImmutableByteArray(array, offset, size)

        fun unsafeFromByteBuffer(byteBuffer: ByteBuffer): ImmutableByteArray =
            ImmutableByteArray(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining())

    }

    fun get(idx: Int): Byte? = if (idx < unsafeUnderlyingArray.size) {
        unsafeUnderlyingArray[idx]
    } else {
        null
    }

    fun unsafeToByteBuffer(): ByteBuffer = ByteBuffer.wrap(unsafeUnderlyingArray, underlyingArrayOffset, size)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImmutableByteArray

        if (!unsafeUnderlyingArray.contentEquals(other.unsafeUnderlyingArray)) return false
        if (underlyingArrayOffset != other.underlyingArrayOffset) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unsafeUnderlyingArray.contentHashCode()
        result = 31 * result + underlyingArrayOffset
        result = 31 * result + size
        return result
    }

}