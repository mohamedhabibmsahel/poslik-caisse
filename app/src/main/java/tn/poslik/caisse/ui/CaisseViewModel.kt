package tn.poslik.caisse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.poslik.caisse.AppContainer
import tn.poslik.caisse.data.SaleEntity
import tn.poslik.caisse.domain.CartLine
import tn.poslik.caisse.domain.Catalog
import tn.poslik.caisse.domain.Money
import tn.poslik.caisse.sync.connectedFlow

class CaisseViewModel(private val container: AppContainer) : ViewModel() {

    private val quantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _busy = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)

    val cart: StateFlow<List<CartLine>> = quantities
        .map { q -> Catalog.products.mapNotNull { p -> q[p.id]?.takeIf { it > 0 }?.let { CartLine(p, it) } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val busy: StateFlow<Boolean> = _busy
    val messages: Flow<String> = _messages.receiveAsFlow()

    val registerCode: StateFlow<String?> = container.dao.observeRegisterState()
        .map { it?.registerCode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<SaleEntity>> = container.dao.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unsyncedCount: StateFlow<Int> = container.dao.observeUnsyncedCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val online: StateFlow<Boolean> = container.firebase.connectedFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val printerOutOfOrder: StateFlow<Boolean> = container.printer.outOfOrder

    fun add(productId: String) = quantities.update { it + (productId to (it[productId] ?: 0) + 1) }

    fun remove(productId: String) = quantities.update {
        val q = (it[productId] ?: 0) - 1
        if (q <= 0) it - productId else it + (productId to q)
    }

    fun clearCart() {
        quantities.value = emptyMap()
    }

    fun checkout() {
        val lines = cart.value
        if (lines.isEmpty() || _busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val sale = container.checkout(lines)
                // Le panier n'est vidé qu'une fois la vente validée sur disque.
                quantities.value = emptyMap()
                _messages.trySend("Ticket ${sale.ticketId} encaissé — ${Money.format(sale.totalMillimes)}")
            } catch (e: Exception) {
                _messages.trySend("Encaissement impossible : ${e.message}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun reprint(ticketId: String) = container.printQueue.enqueue(ticketId)

    fun setPrinterOutOfOrder(value: Boolean) = container.printer.setOutOfOrder(value)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CaisseViewModel(container) }
        }
    }
}
