package tn.poslik.caisse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.poslik.caisse.domain.CartLine
import tn.poslik.caisse.domain.Catalog
import tn.poslik.caisse.domain.Money

@Composable
fun CaisseScreen(vm: CaisseViewModel) {
    val cart by vm.cart.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val registerCode by vm.registerCode.collectAsStateWithLifecycle()
    val provisioned = registerCode != null

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(Modifier.fillMaxSize()) {
                ProductGrid(vm, Modifier.weight(1.4f).fillMaxHeight())
                CartPanel(cart, busy, provisioned, vm, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ProductGrid(vm, Modifier.weight(1f).fillMaxWidth())
                CartPanel(cart, busy, provisioned, vm, Modifier.fillMaxWidth().heightIn(max = 340.dp))
            }
        }
    }
}

@Composable
private fun ProductGrid(vm: CaisseViewModel, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(Catalog.products, key = { it.id }) { product ->
            ElevatedCard(onClick = { vm.add(product.id) }) {
                Column(Modifier.padding(14.dp).heightIn(min = 64.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(Money.format(product.priceMillimes), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    cart: List<CartLine>,
    busy: Boolean,
    provisioned: Boolean,
    vm: CaisseViewModel,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Panier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (cart.isEmpty()) {
                Text(
                    "Touchez un produit pour l'ajouter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(cart, key = { it.product.id }) { line ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(line.product.name, style = MaterialTheme.typography.bodyLarge)
                                Text(Money.format(line.totalMillimes), style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalIconButton(onClick = { vm.remove(line.product.id) }, modifier = Modifier.size(34.dp)) { Text("−") }
                            Text("${line.quantity}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            FilledTonalIconButton(onClick = { vm.add(line.product.id) }, modifier = Modifier.size(34.dp)) { Text("+") }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    Money.format(cart.sumOf { it.totalMillimes }),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = vm::checkout,
                enabled = cart.isNotEmpty() && !busy && provisioned,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Encaisser", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (!provisioned) {
                Text(
                    "Première mise en service : connexion requise une seule fois pour attribuer un code à cette caisse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
