package io.github.kkoshin.muse.repo

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Simple test to verify test setup is working
 */
class SimpleMuseTest {

    @Test
    fun `test setup is working`() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `test MAX_TEXT_LENGTH constant exists`() {
        assertEquals(25_000, MAX_TEXT_LENGTH)
    }
}