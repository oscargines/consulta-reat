package com.oscar.consultareat.ui.inspeccion

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.ConsultaResultado
import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
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

private val puntos = listOf(
    PuntoInspeccion("autorizacion", "Art. 2", "Autorización administrativa del servicio disponible y vigente", "KA01.08"),
    PuntoInspeccion("antiguedad", "Art. 3", "Antigüedad del vehículo dentro del límite permitido", "KF01.01"),
    PuntoInspeccion("categoria", "Art. 4.1", "Vehículo homologado como categoría M", "VEH012.9"),
    PuntoInspeccion("pantalla", "Art. 4.2.1", "Pantalla de protección del puesto del conductor", "VEH012.9"),
    PuntoInspeccion("puertas", "Art. 4.2.2", "Puertas de servicio y apertura de emergencia protegida", "VEH012.9"),
    PuntoInspeccion("ventanas", "Art. 4.2.3", "Abertura de ventanas limitada al tercio superior", "VEH012.9"),
    PuntoInspeccion("asientos", "Art. 4.2.4", "Protecciones en asientos enfrentados o sin respaldo suficiente", "VEH012.9"),
    PuntoInspeccion("emergencia", "Art. 4.2.6", "Señal de emergencia luminosa operativa en las paradas", "VEH018.1"),
    PuntoInspeccion("martillos", "Art. 4.2.7", "Martillos rompecristales protegidos y disponibles", "VEH012.9"),
    PuntoInspeccion("plazas", "Art. 4.2.12", "Cada menor dispone de su propia plaza o asiento", "CIR009.5E"),
    PuntoInspeccion("tacografo", "Art. 4.2.13", "Tacógrafo instalado cuando resulte exigible", "VEH011.15"),
    PuntoInspeccion("limitador", "Art. 4.2.14", "Limitador de velocidad cuando resulte exigible", "VEH011.15"),
    PuntoInspeccion("frenos", "Art. 4.2.15", "Frenos y ABS en las condiciones exigibles", "VEH012.8"),
    PuntoInspeccion("extintor", "Art. 4.2.27", "Extintor y botiquín de primeros auxilios", "VEH019.1"),
    PuntoInspeccion("salidas", "Art. 4.2.29-31", "Puertas, trampillas y salidas de emergencia operativas y señalizadas", "VEH012.9"),
    PuntoInspeccion("v10", "Art. 5", "Distintivo de transporte escolar V-10 visible", "VEH018.1"),
    PuntoInspeccion("itv", "Art. 6", "ITV específica favorable y en vigor", "VEH010.1"),
    PuntoInspeccion("conductor", "Art. 7", "Conductor con los permisos y requisitos exigibles", "KJ01.01"),
    PuntoInspeccion("acompanante", "Art. 8", "Acompañante presente cuando sea obligatorio", "KB01.01"),
    PuntoInspeccion("velocidad", "Art. 9", "Velocidad máxima respetada", "CIR048.1"),
    PuntoInspeccion("paradas", "Art. 10", "Itinerario, paradas y acceso de menores seguros", "CIR011.1"),
    PuntoInspeccion("duracion", "Art. 11", "Duración máxima del viaje y descansos respetados", "CIR120.1"),
    PuntoInspeccion("seguro", "Art. 12", "Responsabilidad civil ilimitada cubierta", "SOA002.1"),
    PuntoInspeccion("documentacion", "Art. 13", "Entidad organizadora ha exigido la documentación", "KC03.01")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspeccionTransEscolarScreen(
    uiState: UiState,
    onConsultar: (ConsultaRequest) -> Unit,
    onResultadoHtmlObtenido: (String, TipoConsulta) -> Unit,
    onBack: () -> Unit,
    onAbrirConfiguracionImpresora: () -> Unit,
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

    LaunchedEffect(uiState) {
        avisos = when (val state = uiState) {
            is UiState.Success -> avisosVehiculo(state.resultado)
            else -> emptyList()
        }
    }

    val context = LocalContext.current
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) mostrandoScanner = true
    }

    val puedeImprimir = uiState is UiState.Success

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
                        cargando = uiState is UiState.Loading
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
                    Spacer(Modifier.height(8.dp))
                    // Botón imprimir siempre visible; habilitado solo con éxito y impresora configurada
                    val resultadoSuccess = (uiState as? UiState.Success)?.resultado
                    val hayResultado = resultadoSuccess != null
                    val storage = BluetoothPrinterStorage(context)
                    val hayImpresora = storage.hasDefaultPrinter()
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (hayResultado && hayImpresora) {
                                    val defaultPrinter = storage.getDefaultPrinter()
                                    defaultPrinter?.let { printer ->
                                        val actaData = ActaInspeccionData(
                                            matricula = resultadoSuccess.matricula ?: "",
                                            empresaTitular = resultadoSuccess.empresaTitular ?: "",
                                            numeroAutorizacion = resultadoSuccess.numeroAutorizacion ?: ""
                                        )
                                        printActaInspeccion(context, printer.mac, actaData)
                                    }
                                } else if (!hayImpresora) {
                                    mostrarAvisoImpresora = true
                                }
                            },
                            enabled = hayResultado && hayImpresora,
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
    cargando: Boolean
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
