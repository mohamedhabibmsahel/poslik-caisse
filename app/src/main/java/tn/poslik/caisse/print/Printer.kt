package tn.poslik.caisse.print

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import tn.poslik.caisse.data.SaleWithLines
import tn.poslik.caisse.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrinterException(message: String) : Exception(message)

/** Point d'extension : une vraie imprimante (Bluetooth, USB, réseau) implémente cette interface. */
interface Printer {
    /** Retourne normalement si le ticket est imprimé, lève une exception sinon. */
    suspend fun print(sale: SaleWithLines)
}

/** Imprimante simulée : délai réaliste, panne activable depuis l'interface. */
class FakePrinter(private val printDelayMs: Long = 800) : Printer {
    private val _outOfOrder = MutableStateFlow(false)
    val outOfOrder: StateFlow<Boolean> = _outOfOrder

    fun setOutOfOrder(value: Boolean) {
        _outOfOrder.value = value
    }

    override suspend fun print(sale: SaleWithLines) {
        delay(printDelayMs)
        if (_outOfOrder.value) throw PrinterException("Imprimante injoignable")
        Log.i("FakePrinter", "\n" + render(sale))
    }

    companion object {
        fun render(sale: SaleWithLines): String = buildString {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(sale.sale.createdAt))
            appendLine("TICKET ${sale.sale.ticketId}   $date")
            sale.lines.forEach { appendLine("${it.quantity} x ${it.name}  ${Money.format(it.unitPriceMillimes * it.quantity)}") }
            appendLine("TOTAL  ${Money.format(sale.sale.totalMillimes)}")
        }
    }
}
