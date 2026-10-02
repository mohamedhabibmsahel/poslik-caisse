package tn.poslik.caisse.sync

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** État réel de la connexion au serveur Firebase (pas seulement « Wi-Fi activé »). */
fun FirebaseDatabase.connectedFlow(): Flow<Boolean> = callbackFlow {
    val ref = getReference(".info/connected")
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot.getValue(Boolean::class.java) ?: false)
        }

        override fun onCancelled(error: DatabaseError) {
            trySend(false)
        }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
}
