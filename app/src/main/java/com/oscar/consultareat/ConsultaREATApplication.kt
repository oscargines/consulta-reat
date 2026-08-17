package com.oscar.consultareat

import android.app.Application
import android.util.Log
import com.oscar.consultareat.data.cache.HistorialCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConsultaREATApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                val cache = HistorialCache(this@ConsultaREATApplication)
                val eliminados = cache.purgarExpirados()
                if (eliminados > 0) {
                    Log.d(TAG, "Historial purgado: $eliminados documento(s) expirados")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purgando el historial", e)
            }
        }
    }

    companion object {
        private const val TAG = "ConsultaREAT.Application"
    }
}