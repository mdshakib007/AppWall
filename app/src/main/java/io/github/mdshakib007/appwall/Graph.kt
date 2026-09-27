package io.github.mdshakib007.appwall

import android.content.Context
import io.github.mdshakib007.appwall.core.BlockEngine
import io.github.mdshakib007.appwall.data.BlockRepository
import io.github.mdshakib007.appwall.data.InstalledApps
import io.github.mdshakib007.appwall.data.Prefs
import io.github.mdshakib007.appwall.data.UsageProvider
import io.github.mdshakib007.appwall.data.db.AppWallDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tiny hand-rolled service locator. No DI framework = smaller APK, zero reflection. */
object Graph {
    lateinit var appContext: Context private set
    lateinit var db: AppWallDatabase private set
    lateinit var prefs: Prefs private set
    lateinit var usage: UsageProvider private set
    lateinit var installedApps: InstalledApps private set
    lateinit var repo: BlockRepository private set
    lateinit var engine: BlockEngine private set

    /** Process-wide scope for background work shared by services and UI. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        db = AppWallDatabase.build(appContext)
        prefs = Prefs(appContext)
        usage = UsageProvider(appContext)
        installedApps = InstalledApps(appContext)
        repo = BlockRepository(db, usage)
        engine = BlockEngine(repo, scope)
        initialized = true
        scope.launch {
            prefs.ensureInstalledAt(System.currentTimeMillis())
            repo.pruneOld()
        }
    }
}
