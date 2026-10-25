package com.example.cunipro

import com.example.cunipro.util.BreedingDateCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BreedingDateCalculatorTest {

    @Test
    fun palpationDate_isTenDaysAfterMating() {
        val matingDate = "2026-10-01"
        val palpationDate = BreedingDateCalculator.calculatePalpationDate(matingDate, 10)
        assertEquals("2026-10-11", palpationDate)
    }

    @Test
    fun kindlingDate_isThirtyOneDaysAfterMating() {
        val matingDate = "2026-10-01"
        val kindlingDate = BreedingDateCalculator.calculateExpectedKindlingDate(matingDate, 31)
        assertEquals("2026-11-01", kindlingDate)
    }

    @Test
    fun weaningDate_isThirtyFiveDaysAfterKindling() {
        val kindlingDate = "2026-11-01"
        val weaningDate = BreedingDateCalculator.calculateExpectedWeaningDate(kindlingDate, 35)
        assertEquals("2026-12-06", weaningDate)
    }
}
