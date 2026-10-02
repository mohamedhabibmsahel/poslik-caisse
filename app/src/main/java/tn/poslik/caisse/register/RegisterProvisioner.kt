package tn.poslik.caisse.register

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import tn.poslik.caisse.data.SaleDao
import tn.poslik.caisse.domain.TicketNumber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Premier lancement : obtient un numéro de caisse unique via une transaction Firebase
 * sur /meta/registerCounter. C'est la SEULE opération qui exige le réseau, et une seule fois
 * par installation. Ensuite la caisse numérote ses tickets seule, en ligne comme hors ligne.
 */
class RegisterProvisioner(
    private val dao: SaleDao,
    private val database: FirebaseDatabase,
    private val scope: CoroutineScope,
) {
    fun ensureProvisioned() {
        scope.launch {
            var backoffMs = 2_000L
            while (dao.registerState()?.registerCode == null) {
                try {
                    val number = allocateRegisterNumber()
                    val code = TicketNumber.registerCode(number)
                    database.getReference("registers").child(code).setValue(
                        mapOf(
                            "code" to code,
                            "createdAt" to ServerValue.TIMESTAMP,
                            "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                        ),
                    ).await()
                    dao.assignRegisterCode(code)
                    Log.i(TAG, "Caisse enregistrée : $code")
                } catch (e: Exception) {
                    Log.w(TAG, "Enregistrement de la caisse impossible, nouvel essai dans ${backoffMs}ms", e)
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(60_000)
                }
            }
        }
    }

    private suspend fun allocateRegisterNumber(): Long = suspendCancellableCoroutine { cont ->
        database.getReference("meta/registerCounter").runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                val next = (current.getValue(Long::class.java) ?: 0L) + 1
                current.value = next
                return Transaction.success(current)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                val value = snapshot?.getValue(Long::class.java)
                when {
                    error != null -> cont.resumeWithException(error.toException())
                    !committed || value == null -> cont.resumeWithException(IllegalStateException("Transaction non validée"))
                    else -> cont.resume(value)
                }
            }
        }, false)
    }

    private companion object {
        const val TAG = "RegisterProvisioner"
    }
}
