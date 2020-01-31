package com.example.anapp

import com.changlinli.raptorq.utils.filter
import com.changlinli.raptorq.utils.takeWhile
import com.pholser.junit.quickcheck.Property
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun iterator_takeWhile_works_on_simple_example() {
        val result = listOf(1, 1, 1, 2, 3, 1, 2, 3)
            .iterator()
            .takeWhile { it == 1 }
            .asSequence()
            .toList()
        assertEquals(listOf(1, 1, 1), result)
    }

    @Test
    fun iterator_filter_works_on_simple_example() {
        val result = listOf(1, 1, 1, 2, 3, 1, 2, 3)
            .iterator()
            .filter { it == 1 }
            .asSequence()
            .toList()
        assertEquals(listOf(1, 1, 1, 1), result)
    }

    @Test
    fun iterator_takeWhile_halts_when_paired_with_foreach() {
        val result = mutableListOf<Int>()
        listOf(1, 1, 1, 2, 3)
            .iterator()
            .takeWhile { it == 1 }
            .forEach { result.add(it) }

        assertEquals(listOf(1, 1, 1), result)

    }

}

@RunWith(JUnitQuickcheck::class)
class ExamplePropertyTests {
    @Property
    fun iterator_takeWhile_agrees_with_list(list: LinkedList<Int>) {
        val elementToCompare = list.getOrElse(2){1}
        val result = list
            .iterator()
            .takeWhile { it == elementToCompare }
            .asSequence()
            .toList()
        assertEquals(list.takeWhile{ it == elementToCompare }, result)
    }

    @Property
    fun iterator_filter_agrees_with_list(list: LinkedList<Int>) {
        val elementToCompare = list.getOrElse(2) {1}
        val result = list
            .iterator()
            .filter { it == elementToCompare }
            .asSequence()
            .toList()
        assertEquals(list.filter{ it == elementToCompare }, result)
    }

}
