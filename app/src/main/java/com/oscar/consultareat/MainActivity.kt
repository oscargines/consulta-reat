package com.oscar.consultareat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.FileProvider
import com.oscar.consultareat.data.datasource.nfc.NfcTagRepository
import com.oscar.consultareat.ui.AppNavigation
import com.oscar.consultareat.ui.theme.ConsultaREATTheme
import java.io.File

/**
 * Activity principal de ConsultaREAT - Single-Activity Architecture.
 *
 * Responsabilidades:
 * 1. **Configuración NFC**: ReaderMode para lectura de DNI electrónico.
 * 2. **Entry point de UI**: `setContent` con `AppNavigation` de Navigation Compose.
 * 3. **Efectos Android**: abrir/compartir PDF, mostrar Toasts.
 *
 * @see NfcTagRepository Para lectura de DNI
 */
class MainActivity : ComponentActivity(), NfcReaderController {
    private companion object {
        const val NFC_LOG_TAG = "MainActivityNfc"
    }

    /**
     * Adaptador NFC del dispositivo.
     */
    private var nfcAdapter: NfcAdapter? = null

    /**
     * Callback que se ejecuta cuando se detecta una etiqueta NFC en ReaderMode.
     */
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        if (tag == null) {
            Log.w(NFC_LOG_TAG, "ReaderMode callback con tag=null")
            return@ReaderCallback
        }
        val uid = tag.id?.joinToString(":") { "%02X".format(it) }.orEmpty()
        Log.i(
            NFC_LOG_TAG,
            "ReaderMode tag detectado uid=$uid techs=${tag.techList.joinToString()} " +
                "thread=${Thread.currentThread().name} nfcEnabled=${nfcAdapter?.isEnabled} " +
                "capturedAt=${System.currentTimeMillis()}"
        )
        NfcTagRepository.update(tag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        logNfcAdapterState("onCreate")
        processNfcIntent(intent)
        enableEdgeToEdge()
        setContent {
            ConsultaREATTheme {
                val nfcController: NfcReaderController = this@MainActivity
                CompositionLocalProvider(LocalNfcReaderController provides nfcController) {
                    AppNavigation(
                        onOpenPdf = { pdfFile ->
                            val opened = openGeneratedPdf(pdfFile)
                            if (!opened) {
                                Toast.makeText(
                                    applicationContext,
                                    "No se pudo abrir el PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onSharePdf = { pdfFile ->
                            val shared = shareGeneratedPdf(pdfFile)
                            if (!shared) {
                                Toast.makeText(
                                    applicationContext,
                                    "No se pudo compartir el PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onNewIntent
    // ─────────────────────────────────────────────────────────────────────────

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(NFC_LOG_TAG, "onNewIntent() action=${intent.action}")
        setIntent(intent)
        processNfcIntent(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onResume
    // ─────────────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        logNfcAdapterState("onResume")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA - onPause
    // ─────────────────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROCESAMIENTO DE INTENTS NFC
    // ─────────────────────────────────────────────────────────────────────────

    private fun processNfcIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(NFC_LOG_TAG, "processNfcIntent intent=null")
            return
        }
        val action = intent.action
        if (action.isNullOrBlank()) {
            Log.d(NFC_LOG_TAG, "processNfcIntent sin action")
            return
        }
        Log.d(NFC_LOG_TAG, "processNfcIntent action=$action")
        if (
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val uid = tag?.id?.joinToString(":") { "%02X".format(it) }
            Log.i(NFC_LOG_TAG, "Tag detectado uid=${uid ?: "<null>"} techs=${tag?.techList?.joinToString() ?: "<none>"}")
            NfcTagRepository.update(tag)
        } else {
            Log.d(NFC_LOG_TAG, "Intent no NFC recibido: $action")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESTADO Y CONFIGURACIÓN NFC
    // ─────────────────────────────────────────────────────────────────────────

    private fun logNfcAdapterState(origin: String) {
        val adapter = nfcAdapter ?: NfcAdapter.getDefaultAdapter(this)
        nfcAdapter = adapter
        val enabled = adapter?.isEnabled == true
        Log.d(NFC_LOG_TAG, "$origin nfcAdapterPresent=${adapter != null} nfcEnabled=$enabled currentIntentAction=${intent?.action}")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROL NFC BAJO DEMANDA (para lectura de DNI)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Activa el modo lector NFC para lectura de DNI electrónico.
     * Debe llamarse solo cuando el usuario inicia explícitamente la lectura.
     */
    override fun enableNfcReaderModeForDniRead() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) {
            Log.w(NFC_LOG_TAG, "enableNfcReaderModeForDniRead omitido: NFC desactivado")
            return
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 300)
        }
        Log.d(
            NFC_LOG_TAG,
            "enableReaderModeForDniRead flags=$flags presenceDelayMs=300 " +
                "thread=${Thread.currentThread().name} adapterEnabled=${adapter.isEnabled}"
        )
        runCatching { adapter.enableReaderMode(this, readerCallback, flags, options) }
            .onSuccess { Log.i(NFC_LOG_TAG, "ReaderMode habilitado para lectura DNI") }
            .onFailure { Log.e(NFC_LOG_TAG, "No se pudo habilitar ReaderMode para lectura DNI", it) }
    }

    /**
     * Desactiva el modo lector NFC tras completar/cancelar lectura de DNI.
     */
    override fun disableNfcReaderModeForDniRead() {
        val adapter = nfcAdapter ?: return
        Log.d(NFC_LOG_TAG, "disableReaderModeForDniRead thread=${Thread.currentThread().name}")
        runCatching { adapter.disableReaderMode(this) }
            .onSuccess { Log.i(NFC_LOG_TAG, "ReaderMode deshabilitado tras lectura DNI") }
            .onFailure { Log.e(NFC_LOG_TAG, "No se pudo deshabilitar ReaderMode tras lectura DNI", it) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANEJO DE DOCUMENTOS GENERADOS
    // ─────────────────────────────────────────────────────────────────────────

    private fun openGeneratedPdf(pdfFile: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
            true
        }.recoverCatching {
            if (it is ActivityNotFoundException) {
                false
            } else {
                throw it
            }
        }.getOrDefault(false)
    }

    private fun shareGeneratedPdf(pdfFile: File): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Compartir PDF").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(chooser)
            true
        }.recoverCatching {
            if (it is ActivityNotFoundException) {
                false
            } else {
                throw it
            }
        }.getOrDefault(false)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// INTERFAZ PARA CONTROL NFC DESDE COMPOSE
// ─────────────────────────────────────────────────────────────────────────

interface NfcReaderController {
    fun enableNfcReaderModeForDniRead()
    fun disableNfcReaderModeForDniRead()
}

val LocalNfcReaderController = androidx.compose.runtime.staticCompositionLocalOf<NfcReaderController> {
    error("LocalNfcReaderController not provided")
}