package tn.poslik.caisse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.poslik.caisse.data.PrintStatus
import tn.poslik.caisse.data.SaleEntity
import tn.poslik.caisse.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StatusStyle(val label: String, val color: Color, val icon: ImageVector)

private fun PrintStatus.style() = when (this) {
    PrintStatus.PENDING -> StatusStyle("En attente", StatusColors.pending, Icons.Filled.Refresh)
    PrintStatus.PRINTED -> StatusStyle("Imprimé", StatusColors.printed, Icons.Filled.CheckCircle)
    PrintStatus.FAILED -> StatusStyle("Échec", StatusColors.failed, Icons.Filled.Warning)
}

private fun PrintStatus.summary(count: Int): String {
    val s = if (count > 1) "s" else ""
    return when (this) {
        PrintStatus.PENDING -> "$count en attente"
        PrintStatus.PRINTED -> "$count imprimé$s"
        PrintStatus.FAILED -> "$count échec$s"
    }
}

@Composable
fun HistoryScreen(vm: CaisseViewModel) {
    val sales by vm.history.collectAsStateWithLifecycle()
    if (sales.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🧾", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(8.dp))
                Text("Aucune vente pour le moment", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Les tickets encaissés apparaîtront ici.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    val time = SimpleDateFormat("dd/MM · HH:mm", Locale.FRANCE)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "summary") { Summary(sales) }
            items(sales, key = { it.ticketId }) { sale ->
                SaleRow(sale, time.format(Date(sale.createdAt)), onReprint = { vm.reprint(sale.ticketId) })
            }
        }
    }
}

@Composable
private fun Summary(sales: List<SaleEntity>) {
    val byStatus = sales.groupingBy { it.printStatus }.eachCount()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "${sales.size} vente${if (sales.size > 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                Money.format(sales.sumOf { it.totalMillimes }),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrintStatus.entries.forEach { status ->
                    val count = byStatus[status] ?: 0
                    if (count > 0 || status == PrintStatus.PRINTED) StatusChip(status, status.summary(count))
                }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: SaleEntity, time: String, onReprint: () -> Unit) {
    val style = sale.printStatus.style()
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(style.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = style.label, tint = style.color, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(sale.ticketId, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("  ·  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SyncLabel(sale.isSynced)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Money.format(sale.totalMillimes), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    StatusChip(sale.printStatus, style.label)
                }
            }
            if (sale.printStatus == PrintStatus.FAILED) {
                OutlinedButton(
                    onClick = onReprint,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    Text("Réimprimer le ticket")
                }
            }
        }
    }
}

@Composable
private fun SyncLabel(synced: Boolean) {
    Box(
        Modifier.size(6.dp).clip(CircleShape)
            .background(if (synced) StatusColors.printed else MaterialTheme.colorScheme.outline),
    )
    Text(
        if (synced) " Synchronisé" else " À synchroniser",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusChip(status: PrintStatus, label: String) {
    val color = status.style().color
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = RoundedCornerShape(50)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
