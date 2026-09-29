package com.oscar.consultareat.ui.inspeccion

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import com.oscar.consultareat.data.print.PrintStatus
import com.oscar.consultareat.data.print.printActaInspeccion
import com.oscar.consultareat.data.repository.BluetoothPrinterStorage
import com.oscar.consultareat.domain.ActaInspeccionData
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oscar.consultareat.R
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.ConsultaResultado
import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.data.cache.HistorialItem
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
import com.oscar.consultareat.domain.conceptosInspeccionInterna
import com.oscar.consultareat.ui.consulta.CapturaMatriculaScreen
import com.oscar.consultareat.ui.consulta.CaptchaWebView
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.FondoGris
import com.oscar.consultareat.ui.theme.TextoSecundario
import com.oscar.consultareat.ui.viewmodel.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private data class PuntoInspeccion(
    val id: String,
    val articulo: String,
    val texto: String,
    val codigoDgt: String
)

private data class IncidenciaDgt(
    val codigo: String,
    val articulo: String,
    val calificacion: String,
    val hecho: String,
    val multa: String,
    val responsable: String,
    val normaInfringida: String,
    val preceptoSancionador: String,
    val observaciones: String
)

private val puntos = conceptosInspeccionInterna.map { concepto ->
    val codigoDgt = when (concepto.id) {
        "aut_vd" -> "KA01.01"
        "aut_vpc" -> "PA01.01"
        "aut_escolar" -> "KA01.08"
        "colaboracion" -> "KA01.04"
        "libro_ruta" -> "KC03.01"
        "permiso_itv" -> "VEH010.1"
        "seguro" -> "SOA002.1"
        "aptitud_escolar" -> "VEH012.9"
        "antiguedad" -> "KF01.01"
        "permiso_conduccion" -> "KJ01.01"
        "cap" -> "KJ01.01"
        "acompanante" -> "KB01.01"
        "plaza_menor" -> "CIR009.5E"
        "duracion" -> "CIR120.1"
        "v10" -> "VEH018.1"
        "tacografo" -> "VEH011.15"
        "contratacion" -> "KC03.01"
        "pantalla" -> "VEH012.9"
        "puertas" -> "VEH012.9"
        "protecciones_asientos" -> "VEH012.9"
        "cinturones" -> "VEH012.9"
        "emergencia_luminosa" -> "VEH018.1"
        "uso_emergencia" -> "VEH018.1"
        "martillos" -> "VEH012.9"
        "piso" -> "VEH012.9"
        "asideros" -> "VEH012.9"
        "ayudas_motrices" -> "VEH012.9"
        "bordes_escalones" -> "VEH012.9"
        "opticos" -> "VEH012.9"
        "extintor" -> "VEH019.1"
        "mando_seguridad" -> "VEH012.9"
        "botiquin" -> "VEH019.1"
        "salidas" -> "VEH012.9"
        "fluorescentes" -> "VEH012.9"
        "alarma_marcha_atras" -> "VEH012.9"
        "espejos" -> "VEH012.9"
        "homologacion" -> "VEH012.9"
        "estabilizacion" -> "VEH012.9"
        "m1_extintor" -> "VEH019.1"
        "m1_plaza_conductor" -> "VEH012.9"
        "m1_cinturon" -> "VEH012.9"
        else -> "VEH012.9"
    }
    PuntoInspeccion(concepto.id, concepto.referencia ?: "Art. gen.", concepto.texto, codigoDgt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspeccionTransEscolarScreen(
    uiState: UiState,
    onConsultar: (ConsultaRequest) -> Unit,
    onResultadoHtmlObtenido: (String, TipoConsulta) -> Unit,
    onBack: () -> Unit,
    onAbrirConfiguracionImpresora: () -> Unit,
    onAbrirInspeccionInterna: () -> Unit,
    historial: List<HistorialItem>,
    onImportarHistorial: (HistorialItem) -> Unit,
    onImprimirActa: (ConsultaResultado) -> Unit,
    modifier: Modifier = Modifier
) {
    var matricula by remember { mutableStateOf("") }
    var checks by remember { mutableStateOf(emptySet<String>()) }
    var mostrandoScanner by remember { mutableStateOf(false) }
    var avisos by remember { mutableStateOf(emptyList<String>()) }
    var incidencias by remember { mutableStateOf<List<IncidenciaDgt>>(emptyList()) }
    var incidenciaActual by remember { mutableStateOf(0) }
    var mostrarAvisoImpresora by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<PrintStatus?>(null) }
    var mostrarDialogoImpresion by remember { mutableStateOf(false) }
    var mostrarDatosActa by remember { mutableStateOf(false) }
    var mostrarHistorial by remember { mutableStateOf(false) }
    var tipActa by remember { mutableStateOf("") }
    var unidadActa by remember { mutableStateOf("") }
    var lugarActa by remember { mutableStateOf("") }
    var errorDatosActa by remember { mutableStateOf<String?>(null) }
    var opcionTransporte by remember { mutableStateOf<Char?>(null) }
    var datosOcupacion by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var aplicaRd443 by remember { mutableStateOf(false) }
    var mostrarModalPlazas by remember { mutableStateOf(false) }
    var mostrarAlertaEscolar by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val preferenciasActa = remember(context) {
        context.getSharedPreferences("acta_inspeccion_preferences", android.content.Context.MODE_PRIVATE)
    }

    LaunchedEffect(uiState) {
        avisos = when (val state = uiState) {
            is UiState.Success -> avisosVehiculo(state.resultado)
            else -> emptyList()
        }
    }

    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) mostrandoScanner = true
    }

    val puedeImprimir = uiState is UiState.Success
    val resultadoSuccess = (uiState as? UiState.Success)?.resultado
    val hayResultado = resultadoSuccess != null
    val storage = remember(context) { BluetoothPrinterStorage(context) }
    val hayImpresora = storage.hasDefaultPrinter()

    Scaffold(
        modifier = modifier,
        containerColor = FondoGris,
        topBar = {
            TopAppBar(
                title = { Text("Inspección de Transporte Escolar") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = onAbrirConfiguracionImpresora
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Configurar impresora", tint = AzulInstitucional)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AzulInstitucional
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                is UiState.CaptchaRequerido -> CaptchaWebView(
                    request = uiState.request,
                    onResultadoHtmlObtenido = onResultadoHtmlObtenido,
                    onVolver = { },
                    modifier = Modifier.fillMaxSize()
                )
                else -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CabeceraInspeccion()
                    TarjetaTipoTransporte(
                        opcionSeleccionada = opcionTransporte,
                        aplicaRd443 = aplicaRd443,
                        datosOcupacion = datosOcupacion,
                        onOpcionSeleccionada = { opcion ->
                            opcionTransporte = opcion
                            when (opcion) {
                                'A' -> {
                                    aplicaRd443 = true
                                    datosOcupacion = null
                                }
                                else -> mostrarModalPlazas = true
                            }
                        }
                    )
                    ConsultaMatricula(
                        matricula = matricula,
                        onMatriculaChange = { matricula = it.uppercase() },
                        onConsultar = {
                            runCatching {
                                ConsultaRequest.Builder()
                                    .tipoConsulta(TipoConsulta.VEHICULO)
                                    .tipoIdentificacion(TipoIdentificacion.MATRICULA)
                                    .valor(matricula)
                                    .build()
                            }.onSuccess(onConsultar)
                        },
                        onScan = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                mostrandoScanner = true
                            } else permisoCamara.launch(Manifest.permission.CAMERA)
                        },
                        cargando = uiState is UiState.Loading,
                        onImportarHistorial = { mostrarHistorial = true }
                    )
                    if (uiState is UiState.Success) DatosAutobus(uiState.resultado)
                    Text(
                        "La comprobación se centra en la antigüedad del vehículo y su autorización de transporte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                    Text(
                        "Marca las comprobaciones que cumple el vehículo y pulsa Comprobar para revisar los incumplimientos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                    puntos.groupBy { it.articulo }.forEach { (articulo, grupo) ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(articulo, fontWeight = FontWeight.Bold, color = AzulInstitucional)
                                grupo.forEach { punto ->
                                    PuntoCheck(punto, punto.id in checks) {
                                        checks = if (punto.id in checks) checks - punto.id else checks + punto.id
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            incidencias = puntos.filter { it.id !in checks }.map(::incidenciaPara)
                            incidenciaActual = 0
                        },
                        enabled = uiState !is UiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Comprobar") }
                    OutlinedButton(
                        onClick = onAbrirInspeccionInterna,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Abrir hoja interna de inspección") }
                    Spacer(Modifier.height(8.dp))
                    // Botón imprimir siempre visible; habilitado solo con éxito y impresora configurada
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (hayResultado && hayImpresora) {
                                    tipActa = preferenciasActa.getString("tip", "").orEmpty()
                                    unidadActa = preferenciasActa.getString(
                                        "unidad",
                                        preferenciasActa.getString("destacamento", "")
                                    ).orEmpty()
                                    lugarActa = preferenciasActa.getString(
                                        "lugar",
                                        "Estacionamiento Colegio Público de Llanes"
                                    ).orEmpty()
                                    errorDatosActa = null
                                    mostrarDatosActa = true
                                } else if (!hayImpresora) {
                                    mostrarAvisoImpresora = true
                                }
                            },
                            enabled = hayResultado && printStatus !is PrintStatus.Connecting && printStatus !is PrintStatus.Sending,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (hayResultado && hayImpresora) AzulInstitucional else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (hayResultado && hayImpresora) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = "", tint = if (hayResultado && hayImpresora) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (hayResultado && hayImpresora) "Imprimir Acta de Inspección"
                                    else if (!hayImpresora) "Configurar impresora primero"
                                    else "Realiza una consulta primero",
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (mostrandoScanner) {
                CapturaMatriculaScreen(
                    onMatricula = { matricula = it; mostrandoScanner = false },
                    onCancelar = { mostrandoScanner = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (avisos.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { avisos = emptyList() },
            title = { Text("Avisos de inspección") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    avisos.forEach { aviso -> Text(aviso) }
                }

            },
            confirmButton = { TextButton(onClick = { avisos = emptyList() }) { Text("Aceptar") } }
        )
    }

    if (mostrarModalPlazas && opcionTransporte in listOf('B', 'C', 'D')) {
        DialogDatosOcupacion(
            opcion = opcionTransporte!!,
            onConfirmar = { plazas, menores ->
                datosOcupacion = plazas to menores
                aplicaRd443 = when (opcionTransporte) {
                    'B' -> menores * 4 >= plazas * 2
                    'C' -> menores * 4 >= plazas * 3
                    'D' -> menores * 3 >= plazas
                    else -> false
                }
                mostrarModalPlazas = false
                if (aplicaRd443) mostrarAlertaEscolar = true
            },
            onCancelar = { mostrarModalPlazas = false }
        )
    }

    if (mostrarAlertaEscolar) {
        AlertDialog(
            onDismissRequest = { mostrarAlertaEscolar = false },
            title = {
                Text(
                    "TRANSPORTE ESCOLAR Y DE MENORES",
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    when (opcionTransporte) {
                        'B' -> "La mitad o más de las plazas del vehículo están reservadas para viajeros " +
                            "menores de 16 años: la expedición se considera transporte escolar y de menores " +
                            "a efectos del Real Decreto 443/2001 (art. 1.b)."
                        'C' -> "Tres cuartas partes o más de los viajeros son menores de 16 años: el servicio " +
                            "se considera transporte escolar y de menores a efectos del Real Decreto 443/2001 " +
                            "(art. 1.c)."
                        'D' -> "Al menos un tercio de los viajeros son menores de 16 años: el transporte se " +
                            "considera escolar y de menores a efectos del Real Decreto 443/2001 (art. 1.d)."
                        else -> "Servicio de transporte escolar y de menores a efectos del Real Decreto 443/2001."
                    } + " Deben cumplirse sus condiciones de seguridad: distintivo V-10, ITV específica, " +
                        "acompañante obligatorio en su caso, cinturones, salidas de emergencia y " +
                        "limitaciones de velocidad y duración del viaje.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { mostrarAlertaEscolar = false }) { Text("Aceptar") }
            }
        )
    }

    if (mostrarHistorial) {
        AlertDialog(
            onDismissRequest = { mostrarHistorial = false },
            title = { Text("Importar desde historial") },
            text = {
                if (historial.isEmpty()) {
                    Text("No hay consultas guardadas en el historial.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        historial.forEach { item ->
                            OutlinedButton(
                                onClick = {
                                    onImportarHistorial(item)
                                    matricula = item.resultado.matricula
                                        ?: buscarDato(item.resultado.vehiculos, "matrícula", "matricula")
                                        ?: item.valor
                                    mostrarHistorial = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    item.resultado.matricula
                                        ?: buscarDato(item.resultado.vehiculos, "matrícula", "matricula")
                                        ?: item.valor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarHistorial = false }) { Text("Cancelar") }
            }
        )
    }

    if (incidenciaActual < incidencias.size) {
        val incidencia = incidencias[incidenciaActual]
        AlertDialog(
            onDismissRequest = { incidenciaActual++ },
            title = { Text("Posible infracción ${incidenciaActual + 1} de ${incidencias.size}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(incidencia.codigo, fontWeight = FontWeight.Bold, color = AzulInstitucional)
                    Text("Artículo ${incidencia.articulo} · ${incidencia.calificacion}")
                    Text(incidencia.hecho)
                    Text("Multa: ${incidencia.multa}")
                    Text("Responsable: ${incidencia.responsable}")
                    Text("Norma infringida: ${incidencia.normaInfringida}")
                    Text("Precepto sancionador: ${incidencia.preceptoSancionador}")
                    Text("Observaciones: ${incidencia.observaciones}", color = TextoSecundario)
                }
            },
            confirmButton = {
                TextButton(onClick = { incidenciaActual++ }) {
                    Text(if (incidenciaActual == incidencias.lastIndex) "Finalizar" else "Siguiente")
                }
            }
        )
    }

    if (mostrarAvisoImpresora) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoImpresora = false },
            title = { Text("Impresora no configurada") },
            text = { Text("No hay ninguna impresora Zebra configurada. Debes configurar una impresora ZQ521 o RW420 antes de imprimir.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarAvisoImpresora = false
                    onAbrirConfiguracionImpresora()
                }) { Text("Configurar impresora") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAvisoImpresora = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDatosActa) {
        AlertDialog(
            onDismissRequest = { mostrarDatosActa = false },
            title = { Text("Datos del acta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tipActa,
                        onValueChange = {
                            tipActa = it.uppercase().filter { char -> char.isLetterOrDigit() }.take(7)
                            errorDatosActa = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("TIP") },
                        placeholder = { Text("A12345B") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        )
                    )
                    OutlinedTextField(
                        value = unidadActa,
                        onValueChange = {
                            unidadActa = it
                            errorDatosActa = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Unidad") }
                    )
                    OutlinedTextField(
                        value = lugarActa,
                        onValueChange = {
                            lugarActa = it
                            errorDatosActa = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        label = { Text("Lugar") }
                    )
                    errorDatosActa?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatosActa = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val tipValido = Regex("^[A-Z][0-9]{5}[A-Z]$").matches(tipActa)
                    when {
                        !tipValido -> errorDatosActa = "El TIP debe tener el formato A12345B."
                        unidadActa.isBlank() -> errorDatosActa = "La Unidad es obligatoria."
                        lugarActa.isBlank() -> errorDatosActa = "El Lugar es obligatorio."
                        else -> {
                            preferenciasActa.edit()
                                .putString("tip", tipActa)
                                .putString("unidad", unidadActa.trim())
                                .putString("lugar", lugarActa.trim())
                                .apply()

                            val resultado = resultadoSuccess
                            val defaultPrinter = storage.getDefaultPrinter()
                            if (resultado != null && defaultPrinter != null) {
                                val actaData = ActaInspeccionData(
                                    matricula = resultado.matricula.orEmpty(),
                                    empresaTitular = resultado.empresaTitular.orEmpty(),
                                    numeroAutorizacion = resultado.numeroAutorizacion.orEmpty(),
                                    tip = tipActa,
                                    unidad = unidadActa.trim(),
                                    lugar = lugarActa.trim()
                                )
                                mostrarDatosActa = false
                                mostrarDialogoImpresion = true
                                printActaInspeccion(context, defaultPrinter.mac, actaData) { status ->
                                    printStatus = status
                                }
                            }
                        }
                    }
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (mostrarDialogoImpresion) {
        val status = printStatus
        val terminado = status is PrintStatus.Completed || status is PrintStatus.Failed
        AlertDialog(
            onDismissRequest = { if (terminado) { mostrarDialogoImpresion = false; printStatus = null } },
            title = { Text(stringResource(R.string.printing_acta_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!terminado) CircularProgressIndicator(color = AzulInstitucional)
                    Text(
                        when (status) {
                            PrintStatus.Connecting -> stringResource(R.string.printing_acta_connecting)
                            PrintStatus.Sending -> stringResource(R.string.printing_acta_sending)
                            PrintStatus.Completed -> stringResource(R.string.printing_acta_completed)
                            is PrintStatus.Failed -> stringResource(R.string.printing_acta_failed, status.message)
                            null -> stringResource(R.string.printing_acta_connecting)
                        },
                        color = if (status is PrintStatus.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                if (terminado) {
                    TextButton(onClick = { mostrarDialogoImpresion = false; printStatus = null }) {
                        Text("Aceptar")
                    }
                }
            }
        )
    }
}

@Composable
private fun CabeceraInspeccion() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AzulClaro),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.DirectionsBus, null, tint = AzulInstitucional, modifier = Modifier.size(36.dp))
            Column {
                Text("Guía rápida RD 443/2001", fontWeight = FontWeight.Bold, color = AzulInstitucional)
                Text("Consulta el autobús y revisa sus condiciones de seguridad.", color = AzulInstitucional, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ConsultaMatricula(
    matricula: String,
    onMatriculaChange: (String) -> Unit,
    onConsultar: () -> Unit,
    onScan: () -> Unit,
    cargando: Boolean,
    onImportarHistorial: () -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Consulta del autobús", fontWeight = FontWeight.Bold, color = AzulInstitucional)
            OutlinedTextField(
                value = matricula,
                onValueChange = onMatriculaChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Matrícula") },
                trailingIcon = { IconButton(onClick = onScan) { Icon(Icons.Filled.PhotoCamera, "Leer matrícula") } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
                shape = RoundedCornerShape(12.dp)
            )
            Button(onClick = onConsultar, enabled = matricula.isNotBlank() && !cargando, modifier = Modifier.fillMaxWidth()) {
                if (cargando) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Consultar matrícula")
            }
            OutlinedButton(
                onClick = onImportarHistorial,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Importar datos desde el historial")
            }
        }
    }
}

@Composable
private fun DatosAutobus(resultado: ConsultaResultado) {
    val todos = resultado.vehiculos + resultado.autorizaciones
    val antiguedad = antiguedadAlInicioCurso(todos)
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Datos importados", fontWeight = FontWeight.Bold, color = AzulInstitucional)
            DatoImportado("Matrícula", resultado.matricula ?: buscarDato(todos, "matrícula", "matricula"))
            DatoImportado("Fecha de matriculación", buscarDato(todos, "matriculaci"))
            DatoImportado("Antigüedad al 1 de septiembre", antiguedad?.let { "$it años" })
            DatoImportado("Tipo de autorización", buscarDato(todos, "tipo de autoriz"))
            DatoImportado("Número de autorización", resultado.numeroAutorizacion ?: buscarDato(todos, "número", "numero", "autorización", "autorizacion"))
            DatoImportado("Validez", buscarDato(todos, "validez", "caducidad"))
            DatoImportado("Empresa", resultado.empresaTitular ?: buscarDato(todos, "empresa", "titular", "nombre"))
            DatoImportado("Domicilio", buscarDato(todos, "domicilio", "dirección", "direccion"))
        }
    }
}

@Composable
private fun DatoImportado(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun buscarDato(items: List<DatoItem>, vararg claves: String): String? =
    items.firstOrNull { item ->
        val etiqueta = item.etiqueta.lowercase()
        claves.any { etiqueta.contains(it) }
    }?.valor

private fun avisosVehiculo(resultado: ConsultaResultado): List<String> {
    val datos = resultado.vehiculos + resultado.autorizaciones
    val avisos = mutableListOf<String>()
    val antiguedad = antiguedadAlInicioCurso(datos)

    when {
        antiguedad == null -> avisos +=
            "No se ha podido calcular la antigüedad: el resultado no contiene una fecha de matriculación reconocible."
        antiguedad > 16 -> avisos +=
            "El autobús tiene $antiguedad años a fecha 1 de septiembre. El artículo 3 del RD 443/2001 impide utilizar vehículos de más de 16 años para estos servicios."
        antiguedad > 10 -> avisos +=
            "El autobús tiene $antiguedad años a fecha 1 de septiembre. Solo puede utilizarse excepcionalmente hasta 16 años si se acredita que ya se dedicaba a esta clase de transporte o se presenta certificado de desguace de otro vehículo dedicado al transporte escolar en el curso actual o anterior."
    }

    if (resultado.autorizaciones.isEmpty()) {
        avisos += "La matrícula consultada no muestra una autorización de transporte asociada. Comprueba la autorización específica exigible antes de prestar el servicio."
    }
    return avisos
}

private fun antiguedadAlInicioCurso(items: List<DatoItem>): Int? {
    val fechaTexto = buscarDato(items, "matriculaci") ?: return null
    val matriculacion = parseFecha(fechaTexto) ?: return null
    val hoy = LocalDate.now()
    val inicioCurso = LocalDate.of(
        if (hoy.monthValue >= 9) hoy.year else hoy.year - 1,
        9,
        1
    )
    if (matriculacion.isAfter(inicioCurso)) return 0
    return ChronoUnit.YEARS.between(matriculacion, inicioCurso).toInt()
}

private fun parseFecha(valor: String): LocalDate? {
    val limpio = valor.trim().substringBefore(" ")
    val formatos = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )
    return formatos.firstNotNullOfOrNull { formato ->
        runCatching { LocalDate.parse(limpio, formato) }.getOrNull()
    }
}

@Composable
private fun PuntoCheck(punto: PuntoInspeccion, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = if (checked) "Cumple" else "No marcado",
            tint = if (checked) AzulInstitucional else TextoSecundario,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(punto.texto, style = MaterialTheme.typography.bodyMedium)
            Text(punto.articulo, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
        }
    }
}

private fun incidenciaPara(punto: PuntoInspeccion): IncidenciaDgt = when (punto.codigoDgt) {
    "KA01.08" -> IncidenciaDgt("KA01.08", "Art. 1 RD 443/2001", "Muy grave", "Realizar transporte público de viajeros de uso especial-escolares careciendo de autorización específica.", "4001 €", "Titular", "R.D. 443/01 Art. 89 LOTT", "Art. 140.1 LOTT; Art. 197.1 ROTT", "Pérdida de honorabilidad e inmovilización.")
    "KB01.01" -> IncidenciaDgt("KB01.01", "Art. 8 RD 443/2001", "Muy grave", "Ausencia de acompañante cuando resulta obligatorio.", "1001 €", "Titular / transportista", "Art. 8 R.D. 443/01", "Art. 140.29 LOTT; Art. 197.34 ROTT", "Concretar por qué era obligatoria la presencia.")
    "KC03.01" -> IncidenciaDgt("KC03.01", "Art. 13 RD 443/2001", "Leve", "No exigir la documentación que corresponde a la entidad organizadora.", "201 €", "Entidad organizadora", "Art. 13 R.D. 443/01", "Art. 142.12 LOTT; Art. 199.13 ROTT", "Indicar el documento no exigido.")
    "KF01.01" -> IncidenciaDgt("KF01.01", "Art. 3 RD 443/2001", "Muy grave", "Vehículo de transporte escolar con antigüedad superior a la exigible.", "1001 €", "Titular / transportista", "Art. 3 R.D. 443/01", "Art. 140.28 LOTT; Art. 197.33 ROTT", "Indicar fecha de primera matriculación y antigüedad.")
    "CIR009.5E" -> IncidenciaDgt("CIR 009 1 5E", "Art. 9.1 LSV", "Leve", "Transportar personas por encima de las plazas autorizadas.", "100 € / 50 €", "Conductor", "Art. 9.1 RGCir.; RD 443/2001", "Art. 75.c LSV", "En transporte escolar se aplica la normativa específica del RD 443/2001.")
    "VEH010.1" -> IncidenciaDgt("VEH 010 1 5A", "Art. 10 RGVeh.", "Grave", "No someter el vehículo a la ITV periódica establecida.", "200 € / 100 €", "Titular", "Art. 67.2 LSV; Art. 10 RD 920/2017", "Art. 76.o LSV", "Comprobar tarjeta ITV y vigencia.")
    "VEH012.9" -> IncidenciaDgt("VEH 012 9 5A", "Art. 12.9 RGVeh.", "Grave", "Incumplir las condiciones técnicas del transporte escolar.", "200 € / 100 €", "Titular", "Art. 66.1 LSV; Art. 12.9 RGVeh.", "Art. 76.o LSV", "Especificar la condición incumplida.")
    "VEH018.1" -> IncidenciaDgt("VEH 018 1 5A", "Art. 18.1 RGVeh.", "Leve", "No llevar instalada la señal reglamentaria.", "80 € / 40 €", "Titular / conductor", "Art. 10.3 LSV; Art. 18.1 RGVeh.", "Art. 75.c LSV", "Indicar la señal omitida.")
    else -> IncidenciaDgt(
        punto.codigoDgt,
        punto.articulo,
        "Consultar codificado",
        "Incumplimiento de la condición: ${punto.texto}.",
        "Según codificado aplicable",
        "Según el hecho observado",
        "Normativa específica del transporte escolar",
        "Consultar precepto sancionador DGT",
        "Concretar los hechos observados."
    )
}

private val OPCIONES_TRANSPORTE = listOf(
    'A' to "Trans. P. Regular de uso especial escolar",
    'B' to "Trans. P. Regular de uso general",
    'C' to "Trans. P. Discrecional",
    'D' to "Trans. Privado complementario viajeros"
)

@Composable
private fun TarjetaTipoTransporte(
    opcionSeleccionada: Char?,
    aplicaRd443: Boolean,
    datosOcupacion: Pair<Int, Int>?,
    onOpcionSeleccionada: (Char) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Tipo de transporte",
                fontWeight = FontWeight.Bold,
                color = AzulInstitucional
            )
            OPCIONES_TRANSPORTE.forEach { (letra, etiqueta) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpcionSeleccionada(letra) }
                        .padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = opcionSeleccionada == letra,
                        onClick = { onOpcionSeleccionada(letra) }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$etiqueta (Op. $letra)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            datosOcupacion?.let { (plazas, menores) ->
                Text(
                    "Plazas: $plazas · Menores de 16 años: $menores",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
            if (aplicaRd443) {
                TextoTransporteEscolarAnimado()
            }
        }
    }
}

@Composable
private fun TextoTransporteEscolarAnimado() {
    val transicion = rememberInfiniteTransition(label = "transporteEscolar")
    val fraccion by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "parpadeoTransporteEscolar"
    )
    val colorAnimado = lerp(Color(0xFFB71C1C), Color(0xFFFF1744), fraccion)
    Text(
        "TRANSPORTE ESCOLAR",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = colorAnimado,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    )
}

@Composable
private fun DialogDatosOcupacion(
    opcion: Char,
    onConfirmar: (Int, Int) -> Unit,
    onCancelar: () -> Unit
) {
    var plazas by remember { mutableStateOf("") }
    var menores by remember { mutableStateOf("") }
    val plazasNum = plazas.toIntOrNull()
    val menoresNum = menores.toIntOrNull()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Ocupación del vehículo (Op. $opcion)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    when (opcion) {
                        'B' -> "Aplica el RD 443/2001 si la mitad o más de las plazas del vehículo están " +
                            "reservadas para viajeros menores de 16 años (art. 1.b)."
                        'C' -> "Aplica el RD 443/2001 si tres cuartas partes o más de los viajeros son " +
                            "menores de 16 años (art. 1.c)."
                        else -> "Aplica el RD 443/2001 si un tercio o más de los viajeros son menores de " +
                            "16 años (art. 1.d)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
                OutlinedTextField(
                    value = plazas,
                    onValueChange = { plazas = it.filter { c -> c.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("N.º de plazas del vehículo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = menores,
                    onValueChange = { menores = it.filter { c -> c.isDigit() }.take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Pasajeros menores de 16 años") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(plazasNum ?: 0, menoresNum ?: 0) },
                enabled = (plazasNum ?: 0) > 0 && menoresNum != null
            ) { Text("Comprobar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
