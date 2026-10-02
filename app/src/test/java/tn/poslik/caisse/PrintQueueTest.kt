package tn.poslik.caisse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tn.poslik.caisse.data.CaisseDatabase
import tn.poslik.caisse.data.PrintStatus
import tn.poslik.caisse.data.SaleWithLines
import tn.poslik.caisse.domain.Checkout
import tn.poslik.caisse.print.PrintQueue
import tn.poslik.caisse.print.Printer
import tn.poslik.caisse.print.PrinterException
import java.util.Collections

private class RecordingPrinter : Printer {
    val printed: MutableList<String> = Collections.synchronizedList(mutableListOf())
    @Volatile var failing = false

    override suspend fun print(sale: SaleWithLines) {
        if (failing) throw PrinterException("panne")
        printed += sale.sale.ticketId
    }
}

@RunWith(RobolectricTestRunner::class)
class PrintQueueTest {
    private lateinit var db: CaisseDatabase
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before fun setUp() { db = inMemoryDb() }
    @After fun tearDown() { scope.cancel(); db.close() }

    private suspend fun PrintQueue.awaitIdle() = withTimeout(5_000) { pending.first { it == 0 } }

    @Test
    fun checkoutPrintsAndMarksPrinted() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        val printer = RecordingPrinter()
        var syncRequests = 0
        val queue = PrintQueue(dao, printer, scope, onStatusChanged = { syncRequests++ })
        val checkout = Checkout(dao, queue, requestSync = { syncRequests++ })

        val sale = checkout(sampleCart())
        queue.awaitIdle()

        assertEquals(listOf(sale.ticketId), printer.printed)
        assertEquals(PrintStatus.PRINTED, dao.saleWithLines(sale.ticketId)!!.sale.printStatus)
        assertEquals(2, syncRequests) // après l'encaissement, puis après l'impression
    }

    @Test
    fun printerFailureMarksFailed() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        val printer = RecordingPrinter().apply { failing = true }
        val queue = PrintQueue(dao, printer, scope, onStatusChanged = {})
        val sale = Checkout(dao, queue, requestSync = {})(sampleCart())
        queue.awaitIdle()
        assertEquals(PrintStatus.FAILED, dao.saleWithLines(sale.ticketId)!!.sale.printStatus)
    }

    @Test
    fun startupReprintsPendingAndFailedButNeverPrinted() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        val printed = dao.recordSale(sampleCart(), 1).ticketId
        val failed = dao.recordSale(sampleCart(), 2).ticketId
        val pending = dao.recordSale(sampleCart(), 3).ticketId // app tuée avant l'impression
        dao.markPrintResult(printed, PrintStatus.PRINTED.name, 10)
        dao.markPrintResult(failed, PrintStatus.FAILED.name, 10)

        // « Redémarrage » : nouvelle file, nouvelle imprimante.
        val printer = RecordingPrinter()
        val queue = PrintQueue(dao, printer, scope, onStatusChanged = {})
        assertEquals(2, queue.recoverAtStartup())
        queue.awaitIdle()

        assertEquals(listOf(failed, pending), printer.printed)
        listOf(printed, failed, pending).forEach {
            assertEquals(PrintStatus.PRINTED, dao.saleWithLines(it)!!.sale.printStatus)
        }
    }

    @Test
    fun printedTicketNeverGoesBackToFailed() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        val id = dao.recordSale(sampleCart(), 1).ticketId
        assertEquals(1, dao.markPrintResult(id, PrintStatus.PRINTED.name, 10))
        assertEquals(0, dao.markPrintResult(id, PrintStatus.FAILED.name, 11))
        assertEquals(PrintStatus.PRINTED, dao.saleWithLines(id)!!.sale.printStatus)
    }
}
