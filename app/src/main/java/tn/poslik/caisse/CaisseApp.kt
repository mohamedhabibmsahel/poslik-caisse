package tn.poslik.caisse

import android.app.Application
import kotlinx.coroutines.launch

class CaisseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.provisioner.ensureProvisioned()
        container.appScope.launch { container.printQueue.recoverAtStartup() }
        container.requestSync()
    }
}
