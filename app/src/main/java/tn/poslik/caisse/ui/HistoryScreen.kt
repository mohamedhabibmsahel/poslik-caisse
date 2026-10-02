package tn.poslik.caisse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.poslik.caisse.data.PrintStatus
import tn.poslik.caisse.data.SaleEntity
import tn.poslik.caisse.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: CaisseViewModel) {
    val sales by vm.history.collectAsStateWithLifecycle()
    if (sales.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune vente pour le moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE)
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sales, key = { it.ticketId }) { sale ->
            SaleRow(sale, time.format(Date(sale.createdAt)), onReprint = { vm.reprint(sale.ticketId) })
        }
    }
}

@Composable
private fun SaleRow(sale: SaleEntity, time: String, onReprint: () -> Unit) {
    Card {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sale.ticketId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$time · " + if (sale.isSynced) "synchronisé" else "à synchroniser",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(sale.totalMillimes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                PrintChip(sale.printStatus)
                if (sale.printStatus == PrintStatus.FAILED) {
                    TextButton(onClick = onReprint, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Réimprimer")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintChip(status: PrintStatus) {
    val (label, color) = when (status) {
        PrintStatus.PENDING -> "En attente" to StatusColors.pending
        PrintStatus.PRINTED -> "Imprimé" to StatusColors.printed
        PrintStatus.FAILED -> "Échec" to StatusColors.failed
    }
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = RoundedCornerShape(50)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}
