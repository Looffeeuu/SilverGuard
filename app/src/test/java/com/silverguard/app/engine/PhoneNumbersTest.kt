package com.silverguard.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun spacesDashesAndBracketsAreRemoved() {
        assertEquals("13812345678", PhoneNumbers.normalize(" 138 1234-5678 "))
        assertEquals("01062345678", PhoneNumbers.normalize("(010) 6234-5678"))
        assertEquals("+8613812345678", PhoneNumbers.normalize("+86 138 1234 5678"))
    }

    @Test
    fun implausibleInputIsRejected() {
        assertNull(PhoneNumbers.normalize(""))
        assertNull(PhoneNumbers.normalize("妈妈"))
        assertNull(PhoneNumbers.normalize("1234"))
        assertNull(PhoneNumbers.normalize("138abc45678"))
        assertNull(PhoneNumbers.normalize("1".repeat(16)))
    }

    @Test
    fun middleDigitsAreMasked() {
        assertEquals("138****5678", PhoneNumbers.mask("13812345678"))
        assertEquals("62***78", PhoneNumbers.mask("6234578"))
        assertEquals("12315", PhoneNumbers.mask("12315"))
    }
}
