package tn.poslik.caisse.print

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.poslik.caisse.data.PrintStatus
import tn.poslik.caisse.data.SaleDao
import java.util.concurrent.ConcurrentHashMap

/**
 * File d'impression : un seul consommateur, les tickets sortent dans l'ordre d'envoi.
 * L'état est d'abord PENDING en base (écrit par l'encaissement), puis PRINTED ou FAILED.
 * Si l'app meurt pendant l'impression, le ticket reste PENDING et repart au démarrage
 * (au pire une réimpression, jamais un ticket perdu).
 */
class PrintQueue(
    private val dao: SaleDao,
    private val printer: Printer,
    scope: CoroutineScope,
    private val onStatusChanged: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val channel = Channel<String>(Channel.UNLIMITED)
    private val queued = ConcurrentHashMap.newKeySet<String>()
    private val _pending = MutableStateFlow(0)

    /** Nombre de tickets dans la file (en attente ou en cours d'impression). */
    val pending: StateFlow<Int> = _pending

    init {
        scope.launch {
            for (ticketId in channel) {
                try {
                    printOne(ticketId)
                } finally {
                    queued.remove(ticketId)
                    _pending.update { it - 1 }
                }
            }
        }
    }

    /** Sans effet si le ticket est déjà dans la file : pas de double impression au démarrage. */
    fun enqueue(ticketId: String) {
        if (queued.add(ticketId)) {
            _pending.update { it + 1 }
            channel.trySend(ticketId)
        }
    }

    /** Au démarrage : relance les tickets PENDING et FAILED, jamais les PRINTED. */
    suspend fun recoverAtStartup(): Int {
        val ids = dao.ticketsToPrint()
        ids.forEach(::enqueue)
        return ids.size
    }

    private suspend fun printOne(ticketId: String) {
        val sale = dao.saleWithLines(ticketId) ?: return
        if (sale.sale.printStatus == PrintStatus.PRINTED) return
        val status = try {
            printer.print(sale)
            PrintStatus.PRINTED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PrintStatus.FAILED
        }
        if (dao.markPrintResult(ticketId, status.name, clock()) > 0) onStatusChanged()
    }
}
