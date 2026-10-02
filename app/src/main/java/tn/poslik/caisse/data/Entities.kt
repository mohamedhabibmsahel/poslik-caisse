package tn.poslik.caisse.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class PrintStatus { PENDING, PRINTED, FAILED }

/** Ligne unique (id = 0) : identité de la caisse et dernier numéro attribué. */
@Entity(tableName = "register_state")
data class RegisterStateEntity(
    @PrimaryKey val id: Int = 0,
    val registerCode: String?,
    val lastSeq: Long,
)

/**
 * [version] est incrémentée à chaque modification locale, [syncedVersion] est la dernière
 * version confirmée par Firebase. Une vente est « à synchroniser » tant que version > syncedVersion.
 */
@Entity(
    tableName = "sales",
    indices = [Index(value = ["registerCode", "seq"], unique = true)],
)
data class SaleEntity(
    @PrimaryKey val ticketId: String,
    val registerCode: String,
    val seq: Long,
    val createdAt: Long,
    val totalMillimes: Long,
    val printStatus: PrintStatus,
    val printAttempts: Int = 0,
    val printedAt: Long? = null,
    val version: Long = 1,
    val syncedVersion: Long = 0,
) {
    val isSynced: Boolean get() = syncedVersion >= version
}

@Entity(
    tableName = "sale_lines",
    primaryKeys = ["ticketId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["ticketId"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SaleLineEntity(
    val ticketId: String,
    val position: Int,
    val productId: String,
    val name: String,
    val unitPriceMillimes: Long,
    val quantity: Int,
)

data class SaleWithLines(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "ticketId", entityColumn = "ticketId")
    val lines: List<SaleLineEntity>,
)
