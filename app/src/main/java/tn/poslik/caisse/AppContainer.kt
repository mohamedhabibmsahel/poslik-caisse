package tn.poslik.caisse

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tn.poslik.caisse.data.CaisseDatabase
import tn.poslik.caisse.domain.Checkout
import tn.poslik.caisse.print.FakePrinter
import tn.poslik.caisse.print.PrintQueue
import tn.poslik.caisse.register.RegisterProvisioner
import tn.poslik.caisse.sync.FirebaseRemoteSalesStore
import tn.poslik.caisse.sync.SalesSyncer
import tn.poslik.caisse.sync.SyncWorker

/** Injection manuelle : quelques objets à durée de vie application, pas besoin de Hilt ici. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = CaisseDatabase.build(appContext)
    val dao = database.saleDao()
    val firebase: FirebaseDatabase = FirebaseDatabase.getInstance()

    val printer = FakePrinter()
    val requestSync: () -> Unit = { SyncWorker.request(appContext) }
    val printQueue = PrintQueue(dao, printer, appScope, onStatusChanged = requestSync)
    val checkout = Checkout(dao, printQueue, requestSync)
    val salesSyncer = SalesSyncer(dao, FirebaseRemoteSalesStore(firebase))
    val provisioner = RegisterProvisioner(dao, firebase, appScope)
}
