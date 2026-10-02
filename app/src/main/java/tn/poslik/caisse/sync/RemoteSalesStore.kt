package tn.poslik.caisse.sync

import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import tn.poslik.caisse.data.SaleWithLines

interface RemoteSalesStore {
    /** Écriture idempotente : envoyer deux fois la même vente produit un seul nœud. */
    suspend fun upsert(sale: SaleWithLines)
}

/**
 * /sales/{registerCode}/{ticketId} : la clé EST le numéro de ticket, donc un renvoi
 * (retry, crash avant l'accusé, double worker) écrase le même nœud au lieu d'en créer un autre.
 */
class FirebaseRemoteSalesStore(
    private val database: FirebaseDatabase,
    private val timeoutMs: Long = 20_000,
) : RemoteSalesStore {

    override suspend fun upsert(sale: SaleWithLines) {
        val s = sale.sale
        val value = mapOf(
            "ticketId" to s.ticketId,
            "registerCode" to s.registerCode,
            "seq" to s.seq,
            "createdAt" to s.createdAt,
            "totalMillimes" to s.totalMillimes,
            "printStatus" to s.printStatus.name,
            "printAttempts" to s.printAttempts,
            "printedAt" to s.printedAt,
            "lines" to sale.lines.sortedBy { it.position }.map {
                mapOf(
                    "productId" to it.productId,
                    "name" to it.name,
                    "unitPriceMillimes" to it.unitPriceMillimes,
                    "quantity" to it.quantity,
                )
            },
            "syncedAt" to ServerValue.TIMESTAMP,
        )
        withTimeout(timeoutMs) {
            // La Task ne se termine qu'à l'accusé de réception du serveur.
            database.getReference("sales").child(s.registerCode).child(s.ticketId).setValue(value).await()
        }
    }
}
