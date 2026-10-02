package tn.poslik.caisse

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tn.poslik.caisse.data.CaisseDatabase
import tn.poslik.caisse.data.PrintStatus
import tn.poslik.caisse.data.SaleWithLines
import tn.poslik.caisse.sync.RemoteSalesStore
import tn.poslik.caisse.sync.SalesSyncer
import java.io.IOException

/** Simule Firebase : un nœud par clé, comme /sales/{register}/{ticket}. */
private class FakeRemote : RemoteSalesStore {
    val nodes = linkedMapOf<String, SaleWithLines>()
    var writes = 0
    var failAfter = Int.MAX_VALUE
    var beforeWrite: suspend (SaleWithLines) -> Unit = {}

    override suspend fun upsert(sale: SaleWithLines) {
        if (writes >= failAfter) throw IOException("réseau coupé")
        beforeWrite(sale)
        writes++
        nodes["${sale.sale.registerCode}/${sale.sale.ticketId}"] = sale
    }
}

@RunWith(RobolectricTestRunner::class)
class SalesSyncerTest {
    private lateinit var db: CaisseDatabase

    @Before fun setUp() { db = inMemoryDb() }
    @After fun tearDown() { db.close() }

    @Test
    fun offlineSalesAreAllPushedOnReconnect() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        repeat(120) { dao.recordSale(sampleCart(), it.toLong()) } // 120 ventes hors ligne
        assertEquals(120, dao.observeUnsyncedCount().first())

        val remote = FakeRemote()
        val result = SalesSyncer(dao, remote, batchSize = 50).syncAll()

        assertEquals(SalesSyncer.Result.Success(120), result)
        assertEquals(120, remote.nodes.size)
        assertEquals(0, dao.observeUnsyncedCount().first())
    }

    @Test
    fun interruptedSyncLosesNothingAndCreatesNoDuplicate() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        repeat(10) { dao.recordSale(sampleCart(), it.toLong()) }

        val remote = FakeRemote().apply { failAfter = 4 } // coupure au milieu
        assertTrue(SalesSyncer(dao, remote).syncAll() is SalesSyncer.Result.Failure)
        assertEquals(6, dao.observeUnsyncedCount().first())

        remote.failAfter = Int.MAX_VALUE // retour du réseau
        SalesSyncer(dao, remote).syncAll()
        SalesSyncer(dao, remote).syncAll() // passage en double : rien à renvoyer

        assertEquals(10, remote.nodes.size)
        assertEquals(10, remote.writes)
        assertEquals(0, dao.observeUnsyncedCount().first())
    }

    @Test
    fun printStatusChangedDuringUploadIsPushedAgain() = runBlocking {
        val dao = db.saleDao().apply { assignRegisterCode("C01") }
        val id = dao.recordSale(sampleCart(), 1).ticketId
        val remote = FakeRemote()
        var once = true
        remote.beforeWrite = {
            if (once) { once = false; dao.markPrintResult(id, PrintStatus.PRINTED.name, 5) }
        }

        SalesSyncer(dao, remote).syncAll()

        assertEquals(PrintStatus.PRINTED, remote.nodes["C01/$id"]!!.sale.printStatus)
        assertEquals(1, remote.nodes.size)
        assertEquals(0, dao.observeUnsyncedCount().first())
    }
}
