package tn.poslik.caisse

import org.junit.Assert.assertEquals
import org.junit.Test
import tn.poslik.caisse.domain.Money
import tn.poslik.caisse.domain.TicketNumber

class TicketNumberTest {
    @Test
    fun format() {
        assertEquals("C03", TicketNumber.registerCode(3))
        assertEquals("C03-000124", TicketNumber.format("C03", 124))
        assertEquals("C120-000001", TicketNumber.format(TicketNumber.registerCode(120), 1))
    }

    @Test
    fun money() {
        assertEquals("5,100 DT", Money.format(5_100))
        assertEquals("0,050 DT", Money.format(50))
    }
}
