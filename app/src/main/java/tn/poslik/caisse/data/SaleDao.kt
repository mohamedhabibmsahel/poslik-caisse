package tn.poslik.caisse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import tn.poslik.caisse.domain.CartLine
import tn.poslik.caisse.domain.TicketNumber

class RegisterNotProvisionedException : IllegalStateException("La caisse n'a pas encore de code attribué")

@Dao
abstract class SaleDao {

    // --- Identité de la caisse ---------------------------------------------------------

    @Query("SELECT * FROM register_state WHERE id = 0")
    abstract suspend fun registerState(): RegisterStateEntity?

    @Query("SELECT * FROM register_state WHERE id = 0")
    abstract fun observeRegisterState(): Flow<RegisterStateEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertRegisterStateIfAbsent(state: RegisterStateEntity)

    @Query("UPDATE register_state SET registerCode = :code WHERE id = 0 AND registerCode IS NULL")
    protected abstract suspend fun setRegisterCodeIfNull(code: String): Int

    @Query("UPDATE register_state SET lastSeq = :seq WHERE id = 0")
    protected abstract suspend fun setLastSeq(seq: Long)

    /** N'écrase jamais un code déjà attribué : une caisse garde son code à vie. */
    @Transaction
    open suspend fun assignRegisterCode(code: String): Boolean {
        insertRegisterStateIfAbsent(RegisterStateEntity(registerCode = null, lastSeq = 0))
        return setRegisterCodeIfNull(code) == 1
    }

    // --- Encaissement ------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertLines(lines: List<SaleLineEntity>)

    /**
     * Attribution du numéro + écriture de la vente dans UNE transaction SQLite :
     * soit tout est écrit, soit rien. SQLite sérialise les écritures, donc deux appuis
     * simultanés ne peuvent pas lire le même lastSeq.
     */
    @Transaction
    open suspend fun recordSale(cart: List<CartLine>, now: Long): SaleEntity {
        require(cart.isNotEmpty()) { "Panier vide" }
        val state = registerState()
        val code = state?.registerCode ?: throw RegisterNotProvisionedException()
        val seq = state.lastSeq + 1
        val ticketId = TicketNumber.format(code, seq)
        val sale = SaleEntity(
            ticketId = ticketId,
            registerCode = code,
            seq = seq,
            createdAt = now,
            totalMillimes = cart.sumOf { it.totalMillimes },
            printStatus = PrintStatus.PENDING,
        )
        setLastSeq(seq)
        insertSale(sale)
        insertLines(
            cart.mapIndexed { index, line ->
                SaleLineEntity(
                    ticketId = ticketId,
                    position = index,
                    productId = line.product.id,
                    name = line.product.name,
                    unitPriceMillimes = line.product.priceMillimes,
                    quantity = line.quantity,
                )
            },
        )
        return sale
    }

    // --- Historique --------------------------------------------------------------------

    @Query("SELECT * FROM sales ORDER BY seq DESC")
    abstract fun observeHistory(): Flow<List<SaleEntity>>

    @Transaction
    @Query("SELECT * FROM sales WHERE ticketId = :ticketId")
    abstract suspend fun saleWithLines(ticketId: String): SaleWithLines?

    // --- Impression --------------------------------------------------------------------

    @Query("SELECT ticketId FROM sales WHERE printStatus IN ('PENDING', 'FAILED') ORDER BY seq")
    abstract suspend fun ticketsToPrint(): List<String>

    /** Un ticket déjà imprimé ne change plus jamais d'état. */
    @Query(
        """
        UPDATE sales
        SET printStatus = :status,
            printAttempts = printAttempts + 1,
            printedAt = CASE WHEN :status = 'PRINTED' THEN :at ELSE printedAt END,
            version = version + 1
        WHERE ticketId = :ticketId AND printStatus != 'PRINTED'
        """,
    )
    abstract suspend fun markPrintResult(ticketId: String, status: String, at: Long): Int

    // --- Synchronisation ---------------------------------------------------------------

    @Transaction
    @Query("SELECT * FROM sales WHERE version > syncedVersion ORDER BY seq LIMIT :limit")
    abstract suspend fun unsynced(limit: Int): List<SaleWithLines>

    @Query("SELECT COUNT(*) FROM sales WHERE version > syncedVersion")
    abstract fun observeUnsyncedCount(): Flow<Int>

    /**
     * Ne marque comme synchronisée que la version effectivement envoyée : si l'état d'impression
     * a changé pendant l'envoi, version reste > syncedVersion et la vente repartira.
     */
    @Query("UPDATE sales SET syncedVersion = :version WHERE ticketId = :ticketId AND syncedVersion < :version")
    abstract suspend fun markSynced(ticketId: String, version: Long)
}
