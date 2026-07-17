package com.aerocat.cloudy.ota

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Binds [com.aerocat.cloudy.ota.RootService] once and keeps the connection alive for the
 * session. Call [connect] before flashing; [disconnect] when the screen is done.
 */
class RootIpc(private val context: Context) {

    @Volatile var worker: IRootIpc? = null
        private set

    private var connection: ServiceConnection? = null

    val isConnected: Boolean get() = worker != null

    /** Suspends until the root worker is bound (or returns false if root is unavailable). */
    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        if (!RootManager.hasRoot()) { cont.resume(false); return@suspendCancellableCoroutine }
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                worker = IRootIpc.Stub.asInterface(binder)
                if (cont.isActive) cont.resume(worker != null)
            }
            override fun onServiceDisconnected(name: ComponentName?) { worker = null }
        }
        connection = conn
        val intent = Intent(context, RootService::class.java)
        RootService.bind(intent, conn)
        cont.invokeOnCancellation { disconnect() }
    }

    fun disconnect() {
        connection?.let { RootService.stop(Intent(context, RootService::class.java)) }
        connection = null
        worker = null
    }

    /** Convenience: read ro.cloudy.version (the ROM's own version stamp) via the worker. */
    fun cloudyVersionProp(): String = worker?.getProp("ro.cloudy.version").orEmpty()
}
