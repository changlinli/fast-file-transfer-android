package com.changlinli.raptorq.utils

fun <T, S> Iterator<T>.map(f : (T) -> S): Iterator<S> {
    val originalIterator = this

    return object : Iterator<S> {
        override fun hasNext(): Boolean = originalIterator.hasNext()

        override fun next(): S = f(originalIterator.next())

    }
}

fun <T> Iterator<T>.zipWithIndex(): Iterator<Pair<T, Int>> =
    zip(generateSequence(0) { it + 1 }.iterator())

fun <T, S> Iterator<T>.zip(other: Iterator<S>): Iterator<Pair<T, S>> {
    val originalIterator = this
    return object : Iterator<Pair<T, S>> {
        override fun hasNext(): Boolean = originalIterator.hasNext() && other.hasNext()

        override fun next(): Pair<T ,S> = originalIterator.next() to other.next()

    }
}

fun <T> Iterator<T>.tap(f: (T) -> Unit): Iterator<T> {
    val originalIterator = this

    return object : Iterator<T> {
        override fun hasNext(): Boolean = originalIterator.hasNext()

        override fun next(): T {
            val result = originalIterator.next()
            f(result)
            return result
        }

    }
}

fun <T> Iterator<T>.filter(f : (T) -> Boolean): Iterator<T> {
    val originalIterator = this

    return object : Iterator<T> {
        private var nextElement: T? = null
        // We need this boolean check as well because our Iterator might
        // have nulls in it itself, which means nextElement being null
        // is ambiguous as to whether our iterator actually stopped
        private var stoppedBecauseFilterReturnedTrue = false

        override fun hasNext(): Boolean {
            if (stoppedBecauseFilterReturnedTrue) {
                return true
            } else {
                var shouldStop = false

                while(!shouldStop) {
                    if (!originalIterator.hasNext()) {
                        shouldStop = true
                    } else {
                        val nextIteratedElement = originalIterator.next()
                        val filterResult = f(nextIteratedElement)
                        shouldStop = filterResult
                        if (filterResult) {
                            nextElement = nextIteratedElement
                            stoppedBecauseFilterReturnedTrue = filterResult
                        }
                    }
                }

                return stoppedBecauseFilterReturnedTrue
            }
        }

        override fun next(): T = if (hasNext()) {
            // We know that if hasNext() returns true, nextElement must be
            // non-null, because hasNext puts elements there
            val result = nextElement!!
            nextElement = null
            stoppedBecauseFilterReturnedTrue = false
            result
        } else {
            // TODO: Fix message
            throw Exception("This iterator has exhausted all its elements!")
        }

    }
}

fun <T> Iterator<T>.nextNull(): T? = if (this.hasNext()) this.next() else null

fun <T> Iterator<T>.takeWhile(f : (T) -> Boolean): Iterator<T> {
    val originalIterator = this

    // Very similar to filter, we just don't reset our variables after calling next
    return object : Iterator<T> {

        private var nextElement: T? = null
        // In case our iterator itself has nulls we need to record whether
        // it was written to since nextElement being null is ambiguous
        private var nextElementWritten = false

        override fun hasNext(): Boolean =
            if (nextElementWritten) {
                true
            } else {
                if (originalIterator.hasNext()) {
                    nextElement = originalIterator.next()
                    nextElementWritten = true
                    // We know it must have been written to
                    f(nextElement!!)
                } else {
                    false
                }
            }

        override fun next(): T = if (hasNext()) {
            // We know that if hasNext() returns true, nextElement must be
            // non-null, because hasNext puts elements there
            val result = nextElement!!
            nextElement = null
            nextElementWritten = false
            result
        } else {
            // TODO: Fix message
            throw Exception("This iterator has exhausted all its elements!")
        }

    }
}

fun <T, S> Iterator<T>.mapFilterNull(f : (T) -> S?): Iterator<S> {
    val originalIterator = this

    return originalIterator
        .map(f)
        .filter{ it != null }
        .map{ it!! }
}

