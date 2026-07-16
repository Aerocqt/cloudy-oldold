package com.aerocat.cloudy.ota

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

/**
 * A libsu RootService. When Cloudy is unprivileged but rooted, binding this hosts our
 * privileged work in a separate root process (cleaner than spawning `su -c` per action).
 * The AIDL surface is intentionally omitted here for brevity — OtaInstaller uses the
 * simpler Shell.cmd path, and this service is the hook for teams that want a persistent
 * root worker.
 */
class RootService : RootService() {
    override fun onBind(intent: Intent): IBinder? = null
}
