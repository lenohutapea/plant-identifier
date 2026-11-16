package com.lenohutapea.plantidentifier

import com.lenohutapea.plantidentifier.ui.ResultText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTextTest {
    @Test
    fun percentRoundsToWholeNumber() {
        assertEquals("93%", ResultText.percent(0.9301))
        assertEquals("0%", ResultText.percent(0.0))
        assertEquals("100%", ResultText.percent(1.0))
    }

    @Test
    fun conditionTypeLabels() {
        assertEquals("Fungal disease", ResultText.conditionType("fungal"))
        assertEquals("Water mold (oomycete)", ResultText.conditionType("oomycete"))
        assertEquals("Unknown", ResultText.conditionType("unknown"))
    }

    @Test
    fun scheduleDayHandlesSeedingBeforePlanting() {
        assertEquals("25 days before planting", ResultText.scheduleDay(-25))
        assertEquals("Planting day", ResultText.scheduleDay(0))
        assertEquals("Day 21 after planting", ResultText.scheduleDay(21))
    }

    @Test
    fun everyApiStatusHasAnExplanation() {
        listOf("assessed", "uncertain", "not_supported", "unrecognized").forEach {
            assertTrue(it, ResultText.statusExplanation(it, "Tomat").isNotBlank())
        }
    }
}
