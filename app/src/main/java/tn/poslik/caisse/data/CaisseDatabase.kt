package tn.poslik.caisse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RegisterStateEntity::class, SaleEntity::class, SaleLineEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CaisseDatabase : RoomDatabase() {
    abstract fun saleDao(): SaleDao

    companion object {
        fun build(context: Context, name: String = "caisse.db"): CaisseDatabase =
            Room.databaseBuilder(context, CaisseDatabase::class.java, name)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // En WAL, le défaut (NORMAL) peut perdre la dernière transaction sur coupure
                        // de courant. Pour une caisse, une vente validée doit survivre : FULL.
                        db.query("PRAGMA synchronous = FULL").close()
                    }
                })
                .build()
    }
}
