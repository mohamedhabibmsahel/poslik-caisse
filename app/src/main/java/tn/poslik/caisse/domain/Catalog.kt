package tn.poslik.caisse.domain

/** Prix en millimes (1 DT = 1000 millimes) pour éviter tout calcul en virgule flottante. */
enum class Category(val label: String) {
    HOT("Boissons chaudes"),
    COLD("Boissons fraîches"),
    BAKERY("Viennoiseries"),
    SNACK("Snacking"),
}

/** [icon] : pictogramme d'affichage ; en production, l'image viendrait du catalogue distant. */
data class Product(
    val id: String,
    val name: String,
    val priceMillimes: Long,
    val category: Category,
    val icon: String,
)

object Catalog {
    val products = listOf(
        Product("P01", "Café express", 1_800, Category.HOT, "☕"),
        Product("P02", "Cappuccino", 3_500, Category.HOT, "☕"),
        Product("P03", "Thé à la menthe", 2_000, Category.HOT, "🍵"),
        Product("P04", "Jus d'orange", 4_500, Category.COLD, "🍊"),
        Product("P05", "Croissant", 1_500, Category.BAKERY, "🥐"),
        Product("P06", "Pain au chocolat", 1_800, Category.BAKERY, "🍫"),
        Product("P07", "Sandwich thon", 6_500, Category.SNACK, "🥪"),
        Product("P08", "Eau minérale", 1_000, Category.COLD, "💧"),
    )

    private val byId = products.associateBy { it.id }

    fun product(id: String): Product = byId.getValue(id)
}

data class CartLine(val product: Product, val quantity: Int) {
    val totalMillimes: Long get() = product.priceMillimes * quantity
}

object Money {
    fun format(millimes: Long): String {
        val sign = if (millimes < 0) "-" else ""
        val abs = kotlin.math.abs(millimes)
        return "%s%d,%03d DT".format(sign, abs / 1000, abs % 1000)
    }
}
