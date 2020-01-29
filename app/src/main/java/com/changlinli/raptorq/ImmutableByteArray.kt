package com.changlinli.raptorq

class ImmutableByteArray(val underlyingArray: ByteArray, val offset: Int, val size: Int) {
    companion object {
        fun fromArray(array: ByteArray): ImmutableByteArray = fromArray(array, 0, array.size)

        fun fromArray(array: ByteArray, offset: Int, length: Int): ImmutableByteArray {
            val newArray = ByteArray(length)
            System.arraycopy(array, offset, newArray, 0, length)
            return ImmutableByteArray(newArray)
        }

        fun unsafeFromArray(array: ByteArray): ImmutableByteArray =
            ImmutableByteArray(array, 0, array.size)

        fun unsafeFromArray(array: ByteArray, offset: Int, size: Int) =
            ImmutableByteArray(array, offset, size)

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImmutableByteArray

        if (!underlyingArray.contentEquals(other.underlyingArray)) return false
        if (offset != other.offset) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = underlyingArray.contentHashCode()
        result = 31 * result + offset
        result = 31 * result + size
        return result
    }

}