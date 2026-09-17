package com.oscar.consultareat.ui.impresora

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oscar.consultareat.R
import com.oscar.consultareat.data.print.isAllowedZebraPrinterModel
import com.oscar.consultareat.data.print.probeZebraPrinterModelByMacSdk
import com.oscar.consultareat.data.print.supportedBluetoothPrinterModelsText
import com.oscar.consultareat.data.repository.BluetoothPrinterStorage
import com.oscar.consultareat.domain.SavedBluetoothPrinter
import com.oscar.consultareat.ui.theme.AzulInstitucional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BT_TAG = "BluetoothPrinterInspeccion"

/** Dispositivo Bluetooth detectado durante escaneo activo. */
private data class ScannedBluetoothPrinter(
    val nombre: String,
    val mac: String
)

@SuppressLint("MissingPermission")
fun hasBluetoothPermissions(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

fun requiredBluetoothPermissions(): Array<String> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        return arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}

@SuppressLint("MissingPermission")
private fun BluetoothDevice.toScannedBluetoothPrinter(ctx: Context): ScannedBluetoothPrinter {
    var name = this.name
    if (name.isNullOrBlank()) {
        name = ctx.getString(R.string.bluetooth_printer_unknown_name, this.address)
    }
    return ScannedBluetoothPrinter(
        nombre = name!!,
        mac = this.address
    )
}

private fun BluetoothAdapter.cancelDiscoverySafely(ctx: Context) {
    runCatching { this.cancelDiscovery() }.onFailure { Log.w(BT_TAG, "cancelDiscovery failed", it) }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val storage = remember(context) { BluetoothPrinterStorage(context) }
    val bluetoothAdapter = remember(context) {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    var savedPrinters by remember { mutableStateOf(emptyList<SavedBluetoothPrinter>()) }
    var selectedSavedPrinterMac by rememberSaveable { mutableStateOf("") }
    var defaultPrinterName by rememberSaveable { mutableStateOf("") }
    var savedPrintersExpanded by remember { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf("") }
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var scanSecondsLeft by rememberSaveable { mutableStateOf(10) }
    var showSaveConfirmation by rememberSaveable { mutableStateOf(false) }
    var isValidatingPrinter by rememberSaveable { mutableStateOf(false) }
    var scanTrigger by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val discoveredPrinters = remember { mutableStateListOf<ScannedBluetoothPrinter>() }
    var selectedDiscoveredPrinterMac by rememberSaveable { mutableStateOf("") }

    var receiver by remember { mutableStateOf<BroadcastReceiver?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scanTrigger = true
        } else {
            statusMessage = context.getString(R.string.bluetooth_printer_permissions_required)
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scanTrigger = true
        } else {
            statusMessage = context.getString(R.string.bluetooth_printer_bluetooth_disabled)
        }
    }

    fun addDiscoveredPrinter(printer: ScannedBluetoothPrinter) {
        if (printer.mac.isBlank()) return
        if (discoveredPrinters.none { it.mac == printer.mac }) {
            discoveredPrinters += printer
        }
    }

    fun refreshSavedPrinters() {
        if (isInPreview) {
            savedPrinters = listOf(
                SavedBluetoothPrinter(1, "Zebra ZQ521", "00:11:22:33:44:55"),
                SavedBluetoothPrinter(2, "Zebra RW420", "AA:BB:CC:DD:EE:FF")
            )
            val defaultPrinter = savedPrinters.firstOrNull()
            selectedSavedPrinterMac = selectedSavedPrinterMac.ifBlank { defaultPrinter?.mac.orEmpty() }
            defaultPrinterName = defaultPrinter?.nombre.orEmpty()
        } else {
            val defaultPrinter = storage.getDefaultPrinter()
            if (defaultPrinter != null) {
                savedPrinters = listOf(defaultPrinter)
                selectedSavedPrinterMac = selectedSavedPrinterMac.ifBlank { defaultPrinter.mac }
                defaultPrinterName = defaultPrinter.nombre
            } else {
                savedPrinters = emptyList()
                selectedSavedPrinterMac = ""
                defaultPrinterName = ""
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun addBondedDevicesAsFallback() {
        if (bluetoothAdapter == null || !hasBluetoothPermissions(context)) return
        runCatching {
            bluetoothAdapter.bondedDevices?.forEach { device ->
                val printer = device.toScannedBluetoothPrinter(context)
                addDiscoveredPrinter(printer)
            }
        }.onFailure { Log.e(BT_TAG, "addBondedDevicesAsFallback error", it) }
    }

    fun refreshAfterPairing() {
        refreshSavedPrinters()
        if (!isScanning) {
            addBondedDevicesAsFallback()
            if (selectedDiscoveredPrinterMac.isNotBlank() && discoveredPrinters.none { it.mac == selectedDiscoveredPrinterMac }) {
                selectedDiscoveredPrinterMac = ""
            }
        }
    }

    // Scan effect - runs when scanTrigger changes
    LaunchedEffect(scanTrigger) {
        if (!scanTrigger) return@LaunchedEffect
        scanTrigger = false

        Log.d(BT_TAG, "startBluetoothScan() called")
        if (bluetoothAdapter == null) {
            Log.w(BT_TAG, "startBluetoothScan: adapter is null → abort")
            statusMessage = context.getString(R.string.bluetooth_printer_not_supported)
            return@LaunchedEffect
        }

        if (!hasBluetoothPermissions(context)) {
            Log.w(BT_TAG, "startBluetoothScan: missing permissions → abort")
            statusMessage = context.getString(R.string.bluetooth_printer_permissions_required)
            return@LaunchedEffect
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w(BT_TAG, "startBluetoothScan: bluetooth disabled → abort")
            statusMessage = context.getString(R.string.bluetooth_printer_bluetooth_disabled)
            return@LaunchedEffect
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationEnabled = locationManager?.let {
            it.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } ?: false
        Log.d(BT_TAG, "startBluetoothScan: locationEnabled=$locationEnabled")

        if (!locationEnabled) {
            Log.w(BT_TAG, "startBluetoothScan: location disabled → showing paired devices as fallback")
            discoveredPrinters.clear()
            selectedDiscoveredPrinterMac = ""
            addBondedDevicesAsFallback()
            statusMessage = if (discoveredPrinters.isEmpty()) {
                context.getString(R.string.bluetooth_printer_location_disabled)
            } else {
                context.getString(R.string.bluetooth_printer_showing_paired_only)
            }
            return@LaunchedEffect
        }

        Log.d(BT_TAG, "startBluetoothScan: cancelling previous discovery (if any)")
        bluetoothAdapter.cancelDiscoverySafely(context)

        discoveredPrinters.clear()
        selectedDiscoveredPrinterMac = ""

        isScanning = true
        scanSecondsLeft = 10
        statusMessage = context.getString(R.string.bluetooth_printer_scanning)

        val scanReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context?, intent: Intent) {
                val ctxNonNull = ctx ?: return
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let { addDiscoveredPrinter(it.toScannedBluetoothPrinter(ctxNonNull)) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                        scanSecondsLeft = 0
                        if (discoveredPrinters.isEmpty()) {
                            addBondedDevicesAsFallback()
                            statusMessage = ctxNonNull.getString(R.string.bluetooth_printer_no_results)
                        } else {
                            statusMessage = ctxNonNull.getString(R.string.bluetooth_printer_scan_finished)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        receiver = scanReceiver

        coroutineScope.launch {
            for (i in 10 downTo 1) {
                delay(1000)
                scanSecondsLeft = i - 1
                statusMessage = context.getString(R.string.bluetooth_printer_scanning_countdown, i - 1)
            }
        }

        val started = bluetoothAdapter.startDiscovery()
        if (!started) {
            isScanning = false
            scanSecondsLeft = 0
            discoveredPrinters.clear()
            addBondedDevicesAsFallback()
            statusMessage = context.getString(R.string.bluetooth_printer_scan_error)
        }
    }

    // Cleanup effect - unregisters receiver when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            receiver?.let { context.unregisterReceiver(it) }
        }
    }

    val canPairSelectedDiscovered = selectedDiscoveredPrinterMac.isNotBlank()
    val canUseSelectedSaved = savedPrinters.any { it.mac == selectedSavedPrinterMac }

    LaunchedEffect(Unit) {
        refreshSavedPrinters()
        if (!isScanning) {
            addBondedDevicesAsFallback()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.bluetooth_printer_title),
                    style = MaterialTheme.typography.titleMedium
                )

                if (savedPrinters.isEmpty()) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        label = { Text(stringResource(R.string.bluetooth_printer_saved_devices_label)) },
                        placeholder = { Text(stringResource(R.string.bluetooth_printer_no_saved_devices)) }
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = savedPrintersExpanded,
                        onExpandedChange = { savedPrintersExpanded = !savedPrintersExpanded }
                    ) {
                        OutlinedTextField(
                            value = defaultPrinterName,
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.bluetooth_printer_saved_devices_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = savedPrintersExpanded)
                            }
                        )

                        DropdownMenu(
                            expanded = savedPrintersExpanded,
                            onDismissRequest = { savedPrintersExpanded = false }
                        ) {
                            savedPrinters.forEach { printer ->
                                DropdownMenuItem(
                                    text = { Text(printer.nombre) },
                                    onClick = {
                                        selectedSavedPrinterMac = printer.mac
                                        savedPrintersExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.bluetooth_printer_paired_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = defaultPrinterName.ifBlank {
                                stringResource(R.string.bluetooth_printer_not_linked)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (defaultPrinterName.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (savedPrinters.any { it.mac == selectedSavedPrinterMac }) {
                                val selectedPrinter = savedPrinters.first { it.mac == selectedSavedPrinterMac }
                                storage.saveDefaultPrinter(selectedPrinter.nombre, selectedPrinter.mac)
                                refreshSavedPrinters()
                                defaultPrinterName = selectedPrinter.nombre
                                statusMessage = context.getString(
                                    R.string.bluetooth_printer_saved_status,
                                    defaultPrinterName.ifBlank { context.getString(R.string.bluetooth_printer_not_linked) }
                                )
                                showSaveConfirmation = true
                            } else {
                                statusMessage = context.getString(R.string.bluetooth_printer_select_saved_one)
                            }
                        },
                        enabled = savedPrinters.any { it.mac == selectedSavedPrinterMac },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = stringResource(R.string.bluetooth_printer_save_default_action))
                    }
                }

                Text(
                    text = statusMessage.ifBlank { stringResource(R.string.bluetooth_printer_ready) },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bluetooth_printer_discovered_devices),
                                modifier = Modifier.fillMaxWidth(),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (discoveredPrinters.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = stringResource(R.string.bluetooth_printer_no_results))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(discoveredPrinters, key = { it.mac }) { printer ->
                                    val isSelected = selectedDiscoveredPrinterMac == printer.mac
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) Color(0xFFD6EAF8)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedDiscoveredPrinterMac = printer.mac }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = printer.nombre,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Log.d(BT_TAG, "── Scan button pressed ──")
                    if (!hasBluetoothPermissions(context)) {
                        permissionLauncher.launch(requiredBluetoothPermissions())
                    } else if (bluetoothAdapter == null) {
                        statusMessage = context.getString(R.string.bluetooth_printer_not_supported)
                    } else if (!bluetoothAdapter.isEnabled) {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    } else {
                        scanTrigger = true
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.bluetooth_printer_scan_action))
            }

            Button(
                onClick = {
                    val scannedSelection = discoveredPrinters.firstOrNull { it.mac == selectedDiscoveredPrinterMac }
                    when {
                        scannedSelection != null -> {
                            coroutineScope.launch {
                                isValidatingPrinter = true
                                statusMessage = context.getString(R.string.bluetooth_printer_validating)
                                val detectedModel = withContext(Dispatchers.IO) {
                                    probeZebraPrinterModelByMacSdk(scannedSelection.mac)
                                }
                                isValidatingPrinter = false

                                if (!isAllowedZebraPrinterModel(detectedModel)) {
                                    val modelText = detectedModel ?: context.getString(R.string.bluetooth_printer_unknown_model)
                                    statusMessage = context.getString(
                                        R.string.bluetooth_printer_not_supported_model,
                                        modelText,
                                        supportedBluetoothPrinterModelsText()
                                    )
                                    return@launch
                                }

                                val saved = storage.saveDefaultPrinter(scannedSelection.nombre, scannedSelection.mac)
                                refreshSavedPrinters()
                                selectedSavedPrinterMac = saved.mac
                                defaultPrinterName = saved.nombre
                                statusMessage = context.getString(R.string.bluetooth_printer_saved_status, saved.nombre)
                                showSaveConfirmation = true
                            }
                        }

                        else -> {
                            statusMessage = context.getString(R.string.bluetooth_printer_select_one)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = canPairSelectedDiscovered && !isValidatingPrinter,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                if (isValidatingPrinter) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(R.string.bluetooth_printer_save_action))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backIcon = androidx.compose.material.icons.Icons.Filled.ArrowBack
            androidx.compose.material3.IconButton(
                onClick = onBackClick
            ) {
                androidx.compose.material3.Icon(
                    imageVector = backIcon,
                    contentDescription = "Volver",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text(text = stringResource(R.string.bluetooth_printer_saved_title)) },
            text = { Text(text = statusMessage) },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text(text = stringResource(R.string.accept_action))
                }
            }
        )
    }
}

@Preview
@Composable
private fun BluetoothPrinterScreenPreview() {
    com.oscar.consultareat.ui.theme.ConsultaREATTheme {
        BluetoothPrinterScreen()
    }
}