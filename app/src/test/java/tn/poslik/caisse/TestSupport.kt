package tn.poslik.caisse

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import tn.poslik.caisse.data.CaisseDatabase
import tn.poslik.caisse.domain.CartLine
import tn.poslik.caisse.domain.Catalog

fun inMemoryDb(): CaisseDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CaisseDatabase::class.java).build()

fun sampleCart(): List<CartLine> = listOf(
    CartLine(Catalog.product("P01"), 2),
    CartLine(Catalog.product("P05"), 1),
)
