package tn.poslik.caisse.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.poslik.caisse.domain.CartLine
import tn.poslik.caisse.domain.Catalog
import tn.poslik.caisse.domain.Money
import tn.poslik.caisse.domain.Product

@Composable
fun CaisseScreen(vm: CaisseViewModel) {
    val cart by vm.cart.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val registerCode by vm.registerCode.collectAsStateWithLifecycle()
    val quantities = cart.associate { it.product.id to it.quantity }
    val cartState = CartState(cart, busy, provisioned = registerCode != null)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            // Tablette : catalogue à gauche, ticket en cours à droite, toujours visible.
            Row(Modifier.fillMaxSize()) {
                ProductGrid(quantities, vm::add, Modifier.weight(1.5f).fillMaxHeight(), compact = false)
                CartPanel(
                    cartState, vm,
                    Modifier.weight(1f).fillMaxHeight().padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                    expanded = true,
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ProductGrid(quantities, vm::add, Modifier.weight(1f).fillMaxWidth(), compact = true)
                CartPanel(cartState, vm, Modifier.fillMaxWidth(), expanded = false)
            }
        }
    }
}

private class CartState(val lines: List<CartLine>, val busy: Boolean, val provisioned: Boolean) {
    val itemCount = lines.sumOf { it.quantity }
    val total = lines.sumOf { it.totalMillimes }
}

/**
 * Tablette : grandes tuiles. Téléphone : tuiles horizontales compactes.
 * Dans les deux cas, les huit produits tiennent à l'écran sans défiler.
 */
@Composable
private fun ProductGrid(quantities: Map<String, Int>, onAdd: (String) -> Unit, modifier: Modifier, compact: Boolean) {
    LazyVerticalGrid(
        columns = if (compact) GridCells.Fixed(2) else GridCells.Adaptive(170.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = if (compact) 8.dp else 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Pas de titres de section : la teinte de l'icône porte la catégorie, et les huit produits restent visibles.
        items(Catalog.products.sortedBy { it.category }, key = { it.id }) { product ->
            ProductTile(product, quantities[product.id] ?: 0, compact = compact, onClick = { onAdd(product.id) })
        }
    }
}

@Composable
private fun ProductTile(product: Product, quantity: Int, compact: Boolean, onClick: () -> Unit) {
    val selected = quantity > 0
    val tint = product.category.tint()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(if (compact) 16.dp else 18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(Modifier.fillMaxWidth()) {
            val icon = @Composable { size: androidx.compose.ui.unit.Dp ->
                Box(
                    Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(product.icon, fontSize = (size.value * 0.52f).sp)
                }
            }
            val label = @Composable {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    Money.format(product.priceMillimes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (compact) {
                Row(Modifier.padding(10.dp).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    icon(44.dp)
                    Column(Modifier.padding(start = 10.dp, end = if (selected) 22.dp else 0.dp)) { label() }
                }
            } else {
                Column(Modifier.padding(14.dp)) {
                    icon(56.dp)
                    Spacer(Modifier.height(12.dp))
                    label()
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(if (compact) 6.dp else 10.dp),
            ) {
                QuantityBadge(quantity, if (compact) 22.dp else 28.dp)
            }
        }
    }
}

@Composable
private fun QuantityBadge(quantity: Int, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$quantity",
            color = MaterialTheme.colorScheme.onPrimary,
            style = if (size < 28.dp) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CartPanel(state: CartState, vm: CaisseViewModel, modifier: Modifier, expanded: Boolean) {
    var showDetail by rememberSaveable { mutableStateOf(false) }
    if (state.lines.isEmpty()) showDetail = false
    Surface(
        modifier = modifier,
        shape = if (expanded) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = if (expanded) 1.dp else 12.dp,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = if (expanded) 14.dp else 4.dp, bottom = if (expanded) 16.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ticket en cours", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state.lines.isNotEmpty()) {
                    Text(
                        "${state.itemCount} art.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!expanded) {
                        TextButton(onClick = { showDetail = !showDetail }) { Text(if (showDetail) "Masquer" else "Détail") }
                    }
                    TextButton(onClick = vm::clearCart, enabled = !state.busy) { Text("Vider") }
                }
            }

            if (state.lines.isEmpty()) {
                Text(
                    "Touchez un produit pour l'ajouter au ticket.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = if (expanded) 24.dp else 6.dp),
                )
                if (expanded) Spacer(Modifier.weight(1f))
            } else if (expanded) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(state.lines, key = { it.product.id }) { line ->
                        CartLineRow(line, onAdd = { vm.add(line.product.id) }, onRemove = { vm.remove(line.product.id) })
                    }
                }
            } else {
                // Téléphone : résumé sur une ligne, détail (avec +/−) à la demande, pour ne pas masquer le catalogue.
                if (!showDetail) Text(
                    state.lines.joinToString(" · ") { "${it.quantity}× ${it.product.name}" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
                androidx.compose.animation.AnimatedVisibility(visible = showDetail) {
                    LazyColumn(Modifier.heightIn(max = 220.dp).padding(top = 6.dp)) {
                        items(state.lines, key = { it.product.id }) { line ->
                            CartLineRow(line, onAdd = { vm.add(line.product.id) }, onRemove = { vm.remove(line.product.id) })
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = if (expanded) 10.dp else 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Money.format(state.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(if (expanded) 12.dp else 8.dp))
            Button(
                onClick = vm::checkout,
                enabled = state.lines.isNotEmpty() && !state.busy && state.provisioned,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(if (expanded) 60.dp else 54.dp),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Encaisser", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (!state.provisioned) {
                Text(
                    "Mise en service : cette caisse doit être connectée une seule fois pour recevoir son code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(line.product.icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(line.product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Money.format(line.product.priceMillimes)} × ${line.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Stepper(line.quantity, onAdd, onRemove)
        Text(
            Money.format(line.totalMillimes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(88.dp),
        )
    }
}

@Composable
private fun Stepper(quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton("−", onRemove)
            Text(
                "$quantity",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp),
            )
            StepperButton("+", onAdd)
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
