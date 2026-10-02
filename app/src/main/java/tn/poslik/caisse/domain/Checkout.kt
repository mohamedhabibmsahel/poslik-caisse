package tn.poslik.caisse.domain

import tn.poslik.caisse.data.SaleDao
import tn.poslik.caisse.data.SaleEntity
import tn.poslik.caisse.print.PrintQueue

/**
 * Le geste « Encaisser » : numéro + vente écrits atomiquement sur disque, puis ticket
 * envoyé à l'impression et synchro demandée — ces deux dernières étapes sont asynchrones,
 * la main est rendue dès que la transaction locale est validée (quelques ms).
 */
class Checkout(
    private val dao: SaleDao,
    private val printQueue: PrintQueue,
    private val requestSync: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(cart: List<CartLine>): SaleEntity {
        val sale = dao.recordSale(cart, clock())
        printQueue.enqueue(sale.ticketId)
        requestSync()
        return sale
    }
}
