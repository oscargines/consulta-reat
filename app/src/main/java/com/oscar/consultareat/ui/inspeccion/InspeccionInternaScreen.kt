package com.oscar.consultareat.ui.inspeccion

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscar.consultareat.data.pdf.generateInspeccionInternaPdf
import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.domain.InspeccionInternaData
import com.oscar.consultareat.domain.ConsultaResultado
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspeccionInternaScreen(
    resultado: ConsultaResultado?,
    onBack: () -> Unit,
    onLeerConductor: () -> Unit,
    onSharePdf: (InspeccionInternaData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val ahora = remember { LocalDateTime.now() }
    var lugar by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var hora by remember { mutableStateOf(ahora.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var destacamento by remember { mutableStateOf("") }
    var marcaModelo by remember { mutableStateOf("") }
    var titular by remember { mutableStateOf(resultado?.empresaTitular.orEmpty()) }
    var nifCif by remember { mutableStateOf("") }
    var domicilio by remember { mutableStateOf("") }
    var tarjeta by remember { mutableStateOf("") }
    var clase by remember { mutableStateOf("") }
    var plazas by remember { mutableStateOf("") }
    var conductor by remember { mutableStateOf("") }
    var dniNie by remember { mutableStateOf("") }
    var entidad by remember { mutableStateOf("") }
    var entidadCif by remember { mutableStateOf("") }
    var entidadDireccion by remember { mutableStateOf("") }
    var numeroAutorizacion by remember { mutableStateOf(resultado?.numeroAutorizacion.orEmpty()) }
    var kmOrigen by remember { mutableStateOf("") }
    var origen by remember { mutableStateOf("") }
    var alumnos by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var centro by remember { mutableStateOf("") }
    var faltaResultado by remember { mutableStateOf(false) }
    var datosGuardados by remember { mutableStateOf(false) }
    val matricula = resultado?.matricula
        ?: datoConsulta(resultado?.vehiculos.orEmpty(), "matrícula", "matricula")
        ?: ""
    val preferencias = remember(context) {
        context.getSharedPreferences("hoja_datos_basicos", Context.MODE_PRIVATE)
    }
    val preferenciasActa = remember(context) {
        context.getSharedPreferences("acta_inspeccion_preferences", Context.MODE_PRIVATE)
    }

    LaunchedEffect(resultado, matricula) {
        val datosVehiculo = resultado?.vehiculos.orEmpty()
        val datosAutorizacion = resultado?.autorizaciones.orEmpty()
        val todosLosDatos = datosVehiculo + datosAutorizacion

        marcaModelo = datoMarcaModelo(datosVehiculo).orEmpty()
        titular = resultado?.empresaTitular
            ?: resultado?.identidadValor
            ?: datoConsulta(todosLosDatos, "empresa titular", "titular", "empresa", "nombre").orEmpty()
        nifCif = datoConsulta(todosLosDatos, "nif", "cif").orEmpty()
        domicilio = datoConsulta(todosLosDatos, "domicilio", "dirección", "direccion").orEmpty()
        tarjeta = resultado?.numeroAutorizacion
            ?: datoConsulta(datosAutorizacion, "número de autorización", "numero de autorizacion", "número", "numero")
            ?: datoConsulta(todosLosDatos, "tarjeta de transporte", "tarjeta transportes", "tarjeta").orEmpty()
        clase = datoConsulta(datosVehiculo, "clase", "categoría", "categoria", "tipo de vehículo", "tipo de vehiculo").orEmpty()
        plazas = datoConsulta(datosVehiculo, "plazas", "número de plazas", "numero de plazas", "asientos").orEmpty()
        numeroAutorizacion = resultado?.numeroAutorizacion
            ?: datoConsulta(datosAutorizacion, "número de autorización", "numero de autorizacion", "número", "numero").orEmpty()

        lugar = preferenciasActa.getString("lugar", lugar).orEmpty()
        destacamento = preferenciasActa.getString("unidad", destacamento).orEmpty()
        val guardado = preferencias.getString("datos_$matricula", null)?.let(::JSONObject)
        guardado?.let {
            lugar = it.optString("lugar", lugar)
            fecha = it.optString("fecha", fecha)
            hora = it.optString("hora", hora)
            destacamento = it.optString("destacamento", destacamento)
            marcaModelo = it.optString("marcaModelo", marcaModelo)
            titular = it.optString("titular", titular)
            nifCif = it.optString("nifCif", nifCif)
            domicilio = it.optString("domicilio", domicilio)
            tarjeta = it.optString("tarjeta", tarjeta)
            clase = it.optString("clase", clase)
            plazas = it.optString("plazas", plazas)
            conductor = it.optString("conductor", conductor)
            dniNie = it.optString("dniNie", dniNie)
            entidad = it.optString("entidad", entidad)
            entidadCif = it.optString("entidadCif", entidadCif)
            entidadDireccion = it.optString("entidadDireccion", entidadDireccion)
            numeroAutorizacion = it.optString("numeroAutorizacion", numeroAutorizacion)
            kmOrigen = it.optString("kmOrigen", kmOrigen)
            origen = it.optString("origen", origen)
            alumnos = it.optString("alumnos", alumnos)
            destino = it.optString("destino", destino)
            centro = it.optString("centro", centro)
        }
        tarjeta = resultado?.numeroAutorizacion ?: numeroAutorizacion
    }

    // NFC Reading Helper
    val nfcHelper = rememberNfcReadingHelper(
        onDataRead = { data ->
            conductor = data.fullName
            dniNie = data.documentNumber
        },
        onEnableNfcReader = { onLeerConductor() },
        onDisableNfcReader = { }
    )

    fun guardarDatos() {
        preferenciasActa.edit()
            .putString("lugar", lugar.trim())
            .putString("unidad", destacamento.trim())
            .apply()
        preferencias.edit().putString(
            "datos_$matricula",
            JSONObject().apply {
                put("lugar", lugar)
                put("fecha", fecha)
                put("hora", hora)
                put("destacamento", destacamento)
                put("marcaModelo", marcaModelo)
                put("titular", titular)
                put("nifCif", nifCif)
                put("domicilio", domicilio)
                put("tarjeta", tarjeta)
                put("clase", clase)
                put("plazas", plazas)
                put("conductor", conductor)
                put("dniNie", dniNie)
                put("entidad", entidad)
                put("entidadCif", entidadCif)
                put("entidadDireccion", entidadDireccion)
                put("numeroAutorizacion", numeroAutorizacion)
                put("kmOrigen", kmOrigen)
                put("origen", origen)
                put("alumnos", alumnos)
                put("destino", destino)
                put("centro", centro)
            }.toString()
        ).apply()
        datosGuardados = true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Hoja de Datos Básicos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SeccionInterna("Datos generales") {
                Campo("Lugar de la inspección", lugar) { lugar = it }
                Campo("Fecha", fecha) { fecha = it }
                Campo("Hora", hora) { hora = it }
                Campo("Destacamento", destacamento) { destacamento = it }
            }
            SeccionInterna("Vehículo y titular") {
                Campo("Matrícula", matricula, enabled = false)
                Campo("Marca y modelo", marcaModelo) { marcaModelo = it }
                Campo("Titular", titular) { titular = it }
                Campo("NIF/CIF", nifCif) { nifCif = it }
                Campo("Domicilio", domicilio) { domicilio = it }
                Campo("Nº tarjeta transportes", tarjeta) { tarjeta = it }
                Campo("Clase", clase) { clase = it }
                Campo("Nº plazas", plazas) { plazas = it }
            }
            SeccionInterna("Conductor") {
                Campo("Nombre y apellidos", conductor) { conductor = it }
                Campo("DNI/NIE", dniNie) { dniNie = it }
                Button(onClick = { nfcHelper.startCanDialog() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Leer conductor por NFC")
                }
            }
            SeccionInterna("Servicio") {
                Campo("Entidad organizadora / contratante", entidad) { entidad = it }
                Campo("CIF", entidadCif) { entidadCif = it }
                Campo("Dirección", entidadDireccion) { entidadDireccion = it }
                Campo("Número de autorización específica", numeroAutorizacion) { numeroAutorizacion = it }
                Campo("KM origen", kmOrigen) { kmOrigen = it }
                Campo("Origen", origen) { origen = it }
                Campo("Nº alumnos", alumnos) { alumnos = it }
                Campo("Destino", destino) { destino = it }
                Campo("Centro educativo", centro) { centro = it }
            }
            Button(
                onClick = { guardarDatos() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (datosGuardados) "Datos guardados" else "Guardar") }
            Button(
                onClick = {
                    if (lugar.isBlank()) faltaResultado = true
                    else {
                        val data = InspeccionInternaData(
                            lugar = lugar, fecha = fecha, hora = hora, destacamento = destacamento,
                            matricula = matricula, marcaModelo = marcaModelo,
                            titular = titular, nifCif = nifCif, domicilio = domicilio,
                            tarjetaTransportes = tarjeta, clase = clase, plazas = plazas,
                            conductor = conductor, dniNie = dniNie,
                            entidadOrganizadora = entidad, entidadCif = entidadCif, entidadDireccion = entidadDireccion,
                            numeroAutorizacion = numeroAutorizacion, kmOrigen = kmOrigen, origen = origen,
                            alumnos = alumnos, destino = destino, centroEducativo = centro
                        )
                        generateAndSharePdf(context, data)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generar y compartir PDF") }
            Spacer(Modifier.height(24.dp))
        }
    }

    // NFC Dialogs
    nfcHelper.NfcDialogs()

    if (faltaResultado) {
        AlertDialog(
            onDismissRequest = { faltaResultado = false },
            title = { Text("Dato pendiente") },
            text = { Text("El lugar de la inspección es necesario para generar el PDF.") },
            confirmButton = { TextButton(onClick = { faltaResultado = false }) { Text("Aceptar") } }
        )
    }
}

private fun datoConsulta(items: List<DatoItem>, vararg claves: String): String? {
    val normalizadas = claves.map(::normalizarEtiqueta)
    return items.firstOrNull { item ->
        val etiqueta = normalizarEtiqueta(item.etiqueta)
        normalizadas.any { clave -> etiqueta.contains(clave) }
    }?.valor?.takeIf { it.isNotBlank() && it != "-" }
}

private fun datoMarcaModelo(items: List<DatoItem>): String? {
    datoConsulta(items, "marca y modelo", "marca/modelo", "marca modelo")?.let { return it }
    val marca = datoConsulta(items, "marca")
    val modelo = datoConsulta(items, "modelo")
    return listOfNotNull(marca, modelo).joinToString(" ").takeIf { it.isNotBlank() }
}

private fun normalizarEtiqueta(valor: String): String =
    java.text.Normalizer.normalize(valor.lowercase(), java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .trim()

private fun generateAndSharePdf(context: Context, data: InspeccionInternaData) {
    val file = generateInspeccionInternaPdf(context, data)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir PDF de inspección"))
}

@Composable
private fun SeccionInterna(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Campo(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit = {}) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)
    )
}
