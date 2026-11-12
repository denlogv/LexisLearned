package dev.denlogv.lexislearned.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountEntryTest {
    private val cards = 1..1000
    private val fresh = 0..1000

    @Test
    fun anyWholeNumberInTheRangeIsAccepted() {
        listOf("1", "7", "30", "31", "250", "999", "1000").forEach {
            assertEquals(CountEntry.Accepted(it.toInt()), parseCount(it, cards))
        }
        assertEquals(CountEntry.Accepted(0), parseCount("0", fresh))
    }

    @Test
    fun leadingZerosAreReadAsTheNumber() {
        assertEquals(CountEntry.Accepted(7), parseCount("007", cards))
        assertEquals(CountEntry.Accepted(0), parseCount("000", fresh))
    }

    @Test
    fun numbersOutsideTheRangeAreRejectedNotClamped() {
        assertEquals(CountEntry.Rejected, parseCount("0", cards))
        assertEquals(CountEntry.Rejected, parseCount("1001", cards))
        assertEquals(CountEntry.Rejected, parseCount("99999999999999999999", cards))
        assertEquals(CountEntry.Rejected, parseCount("2147483648", cards))
    }

    @Test
    fun textThatIsNotAWholeNumberIsRejected() {
        listOf("", " ", "abc", "1 2", " 5", "5 ", "-3", "+3", "2.5", "2,5", "1e3", "٣", "５").forEach {
            assertEquals("'$it'", CountEntry.Rejected, parseCount(it, cards))
        }
    }

    @Test
    fun theSupportTextNamesTheRangeAndTheKeptNumber() {
        assertEquals("Any whole number from 1 to 1000.", countSupportText(cards, rejected = false))
        val error = countSupportText(fresh, rejected = true)
        assertTrue(error.startsWith("Enter a whole number from 0 to 1000."))
        assertTrue(error.endsWith("The last valid number stays in use."))
    }
}
