package tn.poslik.caisse.sync

import kotlinx.coroutines.CancellationException
import tn.poslik.caisse.data.SaleDao

/**
 * Pousse vers Firebase toutes les ventes dont la version locale n'a pas été confirmée.
 * Une vente n'est marquée synchronisée qu'APRÈS l'accusé du serveur : si l'envoi échoue
 * ou si l'app meurt, elle repart au prochain passage (au moins une fois + idempotent = exactement une fois).
 */
class SalesSyncer(
    private val dao: SaleDao,
    private val remote: RemoteSalesStore,
    private val batchSize: Int = 50,
) {
    sealed interface Result {
        data class Success(val pushed: Int) : Result
        data class Failure(val pushed: Int, val cause: Exception) : Result
    }

    suspend fun syncAll(): Result {
        var pushed = 0
        // Borne de sécurité : une vente modifiée pendant l'envoi est reprise au tour suivant.
        repeat(MAX_ROUNDS) {
            val batch = dao.unsynced(batchSize)
            if (batch.isEmpty()) return Result.Success(pushed)
            for (sale in batch) {
                try {
                    remote.upsert(sale)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return Result.Failure(pushed, e)
                }
                dao.markSynced(sale.sale.ticketId, sale.sale.version)
                pushed++
            }
        }
        return Result.Success(pushed)
    }

    private companion object {
        const val MAX_ROUNDS = 100
    }
}
