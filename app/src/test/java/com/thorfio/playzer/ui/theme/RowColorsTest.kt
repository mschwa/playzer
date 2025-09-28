package com.thorfio.playzer.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class RowColorsTest {
    @Test
    fun alternatingRowColor_evenOdd() {
        assertEquals(Charcoal, alternatingRowColor(0))
        assertEquals(DarkGrey, alternatingRowColor(1))
        assertEquals(Charcoal, alternatingRowColor(2))
        assertEquals(DarkGrey, alternatingRowColor(3))
    }
}

