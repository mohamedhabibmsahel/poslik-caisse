package tn.poslik.caisse

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tn.poslik.caisse.data.CaisseDatabase
import tn.poslik.caisse.data.PrintStatus
import tn.poslik.caisse.data.RegisterNotProvisionedException

@RunWith(RobolectricTestRunner::class)
class CheckoutNumberingTest {
    private lateinit var db: CaisseDatabase

    @Before fun setUp() { db = inMemoryDb() }
    @After fun tearDown() { db.close() }

    @Test
    fun numbersAreSequentialAndSaleIsStoredPending() = runBlocking {
        val dao = db.saleDao()
        dao.assignRegisterCode("C01")
        val a = dao.recordSale(sampleCart(), now = 1)
        val b = dao.recordSale(sampleCart(), now = 2)
        assertEquals("C01-000001", a.ticketId)
        assertEquals("C01-000002", b.ticketId)
        assertEquals(5_100, a.totalMillimes)
        assertEquals(PrintStatus.PENDING, a.printStatus)
        assertEquals(2, dao.saleWithLines(a.ticketId)!!.lines.size)
        assertFalse(a.isSynced)
    }

    @Test
    fun concurrentCheckoutsNeverShareANumber() = runBlocking {
        val dao = db.saleDao()
        dao.assignRegisterCode("C01")
        val sales = (1..200).map { async(Dispatchers.Default) { dao.recordSale(sampleCart(), now = it.toLong()) } }.awaitAll()
        assertEquals(200, sales.map { it.ticketId }.toSet().size)
        assertEquals((1L..200L).toList(), sales.map { it.seq }.sorted())
        assertEquals(200L, dao.registerState()!!.lastSeq)
    }

    @Test
    fun checkoutRefusedUntilRegisterHasACode() = runBlocking {
        val dao = db.saleDao()
        val refused = runCatching { dao.recordSale(sampleCart(), now = 1) }.exceptionOrNull()
        assertTrue(refused is RegisterNotProvisionedException)
        assertNull(dao.saleWithLines("C01-000001"))
    }

    @Test
    fun registerCodeIsNeverReassigned() = runBlocking {
        val dao = db.saleDao()
        assertTrue(dao.assignRegisterCode("C01"))
        assertFalse(dao.assignRegisterCode("C02"))
        assertEquals("C01", dao.registerState()!!.registerCode)
    }

    @Test
    fun counterSurvivesAppRestart() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("restart.db")
        var fileDb = CaisseDatabase.build(context, "restart.db")
        fileDb.saleDao().assignRegisterCode("C07")
        repeat(3) { fileDb.saleDao().recordSale(sampleCart(), now = 1) }
        fileDb.close()

        fileDb = CaisseDatabase.build(context, "restart.db")
        val next = fileDb.saleDao().recordSale(sampleCart(), now = 2)
        assertEquals("C07-000004", next.ticketId)
        fileDb.close()
    }
}
