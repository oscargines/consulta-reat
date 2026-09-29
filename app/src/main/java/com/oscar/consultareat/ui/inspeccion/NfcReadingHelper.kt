package com.oscar.consultareat.ui.inspeccion

import android.nfc.Tag
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.consultareat.data.datasource.nfc.NfcDniReader
import com.oscar.consultareat.data.datasource.nfc.NfcTagRepository
import com.oscar.consultareat.domain.NfcDniPersonData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NFC_LOG_TAG = "NfcReadingHelper"

class NfcReadingHelper(
    private val showCanDialogState: MutableState<Boolean>,
    private val canCodeState: MutableState<String>,
    private val canCodeErrorState: MutableState<String>,
    private val nfcReadErrorState: MutableState<String?>,
    private val pendingNfcDataState: MutableState<NfcDniPersonData?>,
    private val showNfcScanDialogState: MutableState<Boolean>,
    private val isReadingNfcState: MutableState<Boolean>,
    private val waitingForNfcTagState: MutableState<Boolean>,
    private val pendingCanForNfcState: MutableState<String>,
    private val pendingAttemptIdState: MutableState<String>,
    private val nfcScanStartedAtMillisState: MutableState<Long>,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val canInvalidMessage: String,
    private val nfcMissingLibraryMessage: String,
    private val nfcReadErrorTitle: String,
    private val onEnableNfcReader: () -> Unit,
    private val onDisableNfcReader: () -> Unit
) {
    val showCanDialog: Boolean by showCanDialogState
    val canCode: String by canCodeState
    val canCodeError: String by canCodeErrorState
    val nfcReadError: String? by nfcReadErrorState
    val pendingNfcData: NfcDniPersonData? by pendingNfcDataState
    val showNfcScanDialog: Boolean by showNfcScanDialogState
    val isReadingNfc: Boolean by isReadingNfcState

    fun startCanDialog() {
        showCanDialogState.value = true
        canCodeState.value = ""
        canCodeErrorState.value = ""
    }

    fun confirmCan(code: String) {
        if (!Regex("^\\d{6}$").matches(code)) {
            canCodeErrorState.value = canInvalidMessage
            return
        }
        val attemptId = System.currentTimeMillis().toString()
        val tagDebug = NfcTagRepository.debugInfo()
        Log.i(NFC_LOG_TAG, "CAN válido. tagDebug: hasTag=${tagDebug.hasTag}, uid=${tagDebug.uid}, ageMs=${tagDebug.ageMs}")
        pendingCanForNfcState.value = code
        pendingAttemptIdState.value = attemptId
        nfcScanStartedAtMillisState.value = System.currentTimeMillis()
        NfcTagRepository.clear()
        onEnableNfcReader()
        waitingForNfcTagState.value = true
        showCanDialogState.value = false
        showNfcScanDialogState.value = true
    }

    fun cancelScan() {
        waitingForNfcTagState.value = false
        showNfcScanDialogState.value = false
        pendingCanForNfcState.value = ""
        pendingAttemptIdState.value = ""
        nfcScanStartedAtMillisState.value = 0L
        showCanDialogState.value = false
        canCodeState.value = ""
        canCodeErrorState.value = ""
        onDisableNfcReader()
    }

    fun clearError() {
        nfcReadErrorState.value = null
    }

    fun retryScan() {
        val can = pendingCanForNfcState.value
        if (can.isBlank()) {
            clearError()
            startCanDialog()
            return
        }
        val attemptId = System.currentTimeMillis().toString()
        NfcTagRepository.clear()
        pendingAttemptIdState.value = attemptId
        nfcScanStartedAtMillisState.value = System.currentTimeMillis()
        nfcReadErrorState.value = null
        onEnableNfcReader()
        waitingForNfcTagState.value = true
        showNfcScanDialogState.value = true
    }

    fun dismissDataDialog() {
        pendingNfcDataState.value = null
    }

    fun updateCanCode(code: String) {
        canCodeState.value = code
        canCodeErrorState.value = ""
    }

    fun startNfcRead(attemptId: String, can: String, tag: Tag) {
        val uid = tag.id?.joinToString(":") { "%02X".format(it) }
        val debugInfo = NfcTagRepository.debugInfo()
        Log.i(
            NFC_LOG_TAG,
            "[$attemptId] Inicio lectura con tag uid=${uid ?: "<null>"} techs=${tag.techList.joinToString()} " +
                "thread=${Thread.currentThread().name} capturedAt=${debugInfo.capturedAtMillis} " +
                "ageMs=${debugInfo.ageMs} hasTag=${debugInfo.hasTag}"
        )
        waitingForNfcTagState.value = false
        showNfcScanDialogState.value = false
        canCodeErrorState.value = ""
        isReadingNfcState.value = true
        scope.launch {
            Log.d(NFC_LOG_TAG, "[$attemptId] Invocando NfcDniReader.read(...) en IO")
            try {
                val result = withContext(Dispatchers.IO) { NfcDniReader.read(can, tag) }
                result.onSuccess {
                    Log.i(NFC_LOG_TAG, "[$attemptId] Lectura NFC OK. nombre='${it.firstName.take(24)}' doc='${it.documentNumber.take(12)}'")
                    pendingNfcDataState.value = it
                }.onFailure { throwable ->
                    val rootCause = generateSequence(throwable) { it.cause }.last()
                    Log.e(
                        NFC_LOG_TAG,
                        "[$attemptId] Lectura NFC fallida: type=${throwable.javaClass.name}, message=${throwable.message}, rootType=${rootCause.javaClass.name}, rootMessage=${rootCause.message}",
                        throwable
                    )
                    nfcReadErrorState.value = if (throwable is ClassNotFoundException) {
                        nfcMissingLibraryMessage
                    } else {
                        throwable.message ?: nfcReadErrorTitle
                    }
                    Log.w(NFC_LOG_TAG, "[$attemptId] Mensaje mostrado al usuario: '${nfcReadErrorState.value}'")
                }
            } finally {
                isReadingNfcState.value = false
                NfcTagRepository.clear()
                onDisableNfcReader()
            }
        }
    }
}

@Composable
fun rememberNfcReadingHelper(
    onDataRead: (NfcDniPersonData) -> Unit,
    onEnableNfcReader: () -> Unit,
    onDisableNfcReader: () -> Unit
): NfcReadingHelper {
    val showCanDialogState = rememberSaveable { mutableStateOf(false) }
    val canCodeState = rememberSaveable { mutableStateOf("") }
    val canCodeErrorState = rememberSaveable { mutableStateOf("") }
    val nfcReadErrorState = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingNfcDataState = remember { mutableStateOf<NfcDniPersonData?>(null) }
    val showNfcScanDialogState = rememberSaveable { mutableStateOf(false) }
    val isReadingNfcState = rememberSaveable { mutableStateOf(false) }
    val waitingForNfcTagState = rememberSaveable { mutableStateOf(false) }
    val pendingCanForNfcState = rememberSaveable { mutableStateOf("") }
    val pendingAttemptIdState = rememberSaveable { mutableStateOf("") }
    val nfcScanStartedAtMillisState = rememberSaveable { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    val canInvalidMessage = "El CAN debe ser de 6 dígitos"
    val nfcMissingLibraryMessage = "Faltan librerías NFC. Reinstala la aplicación."
    val nfcReadErrorTitle = "Error de lectura NFC"

    val helper = remember {
        NfcReadingHelper(
            showCanDialogState = showCanDialogState,
            canCodeState = canCodeState,
            canCodeErrorState = canCodeErrorState,
            nfcReadErrorState = nfcReadErrorState,
            pendingNfcDataState = pendingNfcDataState,
            showNfcScanDialogState = showNfcScanDialogState,
            isReadingNfcState = isReadingNfcState,
            waitingForNfcTagState = waitingForNfcTagState,
            pendingCanForNfcState = pendingCanForNfcState,
            pendingAttemptIdState = pendingAttemptIdState,
            nfcScanStartedAtMillisState = nfcScanStartedAtMillisState,
            scope = scope,
            canInvalidMessage = canInvalidMessage,
            nfcMissingLibraryMessage = nfcMissingLibraryMessage,
            nfcReadErrorTitle = nfcReadErrorTitle,
            onEnableNfcReader = onEnableNfcReader,
            onDisableNfcReader = onDisableNfcReader
        )
    }

    LaunchedEffect(waitingForNfcTagState.value, pendingCanForNfcState.value, pendingAttemptIdState.value) {
        if (!waitingForNfcTagState.value) return@LaunchedEffect
        Log.i(NFC_LOG_TAG, "[${pendingAttemptIdState.value}] Esperando tag NFC nuevo para CAN válido desde ts=${nfcScanStartedAtMillisState.value}...")
        while (waitingForNfcTagState.value) {
            val debugInfo = NfcTagRepository.debugInfo()
            val candidateTag = NfcTagRepository.getLatest()
            if (candidateTag != null && debugInfo.capturedAtMillis >= nfcScanStartedAtMillisState.value) {
                Log.i(NFC_LOG_TAG, "[${pendingAttemptIdState.value}] Tag fresco detectado uid=${debugInfo.uid} capturedAt=${debugInfo.capturedAtMillis}")
                helper.startNfcRead(pendingAttemptIdState.value, pendingCanForNfcState.value, candidateTag)
                break
            }
            delay(300)
        }
    }

    LaunchedEffect(pendingNfcDataState.value) {
        pendingNfcDataState.value?.let { data ->
            onDataRead(data)
        }
    }

    return helper
}

@Composable
fun NfcReadingHelper.NfcDialogs() {
    val nfcScanDialogTitle = "Leer conductor por NFC"
    val nfcWaitingTagMessage = "Acerca el DNI al dispositivo..."
    val nfcReadingProgressTitle = "Leyendo datos..."
    val nfcReadingProgressMessage = "Mantén el DNI cerca del dispositivo"
    val canDialogTitle = "Introduce el CAN"
    val canDialogMessage = "Introduce los 6 dígitos del CAN que aparecen en la esquina inferior derecha del DNI"
    val canCodeLabel = "CAN"
    val canDialogConfirm = "Continuar"
    val cancelAction = "Cancelar"
    val acceptAction = "Aceptar"
    val nfcReadErrorTitle = "Error de lectura NFC"
    val retryAction = "Reintentar"

    if (showCanDialog) {
        AlertDialog(
            onDismissRequest = { cancelScan() },
            title = { Text(canDialogTitle) },
            text = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(canDialogMessage)
                    OutlinedTextField(
                        value = canCode,
                        onValueChange = { updateCanCode(it) },
                        label = { Text(canCodeLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        visualTransformation = VisualTransformation.None
                    )
                    if (canCodeError.isNotBlank()) {
                        Text(canCodeError, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { confirmCan(canCode) }) {
                    Text(canDialogConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelScan() }) {
                    Text(cancelAction)
                }
            }
        )
    }

    if (showNfcScanDialog) {
        if (isReadingNfc) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(nfcReadingProgressTitle) },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(nfcReadingProgressMessage)
                    }
                },
                confirmButton = { }
            )
        } else {
            AlertDialog(
                onDismissRequest = { cancelScan() },
                title = { Text(nfcScanDialogTitle) },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(nfcWaitingTagMessage)
                    }
                },
                confirmButton = { },
                dismissButton = {
                    TextButton(onClick = { cancelScan() }) {
                        Text(cancelAction)
                    }
                }
            )
        }
    }

    if (nfcReadError != null) {
        AlertDialog(
            onDismissRequest = { clearError() },
            title = { Text(nfcReadErrorTitle) },
            text = { Text(nfcReadError!!) },
            confirmButton = {
                Button(onClick = { retryScan() }) {
                    Text(retryAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelScan(); clearError() }) {
                    Text(cancelAction)
                }
            }
        )
    }

    if (pendingNfcData != null) {
        val data = pendingNfcData!!
        AlertDialog(
            onDismissRequest = { dismissDataDialog() },
            title = { Text("Datos leídos del DNI") },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Nombre: ${data.fullName}")
                    Text("DNI/NIE: ${data.documentNumber}")
                }
            },
            confirmButton = {
                Button(onClick = { dismissDataDialog() }) {
                    Text(acceptAction)
                }
            }
        )
    }
}