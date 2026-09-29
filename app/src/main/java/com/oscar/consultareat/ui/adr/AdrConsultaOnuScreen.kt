package com.oscar.consultareat.ui.adr

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.consultareat.R
import com.oscar.consultareat.data.adr.AdrDatabase
import com.oscar.consultareat.data.adr.AdrRepository
import com.oscar.consultareat.data.adr.AdrTablaAItem
import com.oscar.consultareat.data.adr.AdrTablaBItem
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.FondoGris
import com.oscar.consultareat.ui.theme.TextoSecundario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdrConsultaOnuViewModel(
    private val repository: AdrRepository
) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<AdrTablaBItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    var selectedItem by mutableStateOf<AdrTablaBItem?>(null)
    var detalleItems by mutableStateOf<List<AdrTablaAItem>>(emptyList())
    var error by mutableStateOf<String?>(null)

    fun onQueryChange(newQuery: String) {
        query = newQuery
    }

    fun buscar() {
        Log.d("AdrConsultaOnu", "buscar() called, query='${query}', isLoading=$isLoading")
        if (query.trim().isBlank()) {
            Log.d("AdrConsultaOnu", "query is blank, returning")
            results = emptyList()
            return
        }
        isLoading = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = repository.findByNombreOrOnu(query.trim())
                error = if (res.isEmpty()) {
                    "No se ha encontrado ninguna materia u objeto con el código ONU o nombre '${query.trim()}'."
                } else {
                    null
                }
                results = res
            } catch (e: Exception) {
                Log.e("AdrConsultaOnu", "search error", e)
                error = "Error en la búsqueda: ${e.message}"
                results = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    fun seleccionar(item: AdrTablaBItem) {
        selectedItem = item
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = repository.getTablaAByOnu(item.numeroOnu)
                detalleItems = res
            } catch (e: Exception) {
                error = "Error al cargar detalles: ${e.message}"
                detalleItems = emptyList()
            }
        }
    }

    fun limpiar() {
        query = ""
        results = emptyList()
        selectedItem = null
        detalleItems = emptyList()
        error = null
    }
}

class AdrConsultaOnuViewModelFactory(
    private val repository: AdrRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AdrConsultaOnuViewModel(repository) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdrConsultaOnuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AdrRepository(AdrDatabase(context)) }
    val viewModel: AdrConsultaOnuViewModel = viewModel(factory = AdrConsultaOnuViewModelFactory(repository))

    Scaffold(
        modifier = modifier,
        containerColor = FondoGris,
        topBar = {
            TopAppBar(
                title = { Text("Consulta cód. ONU") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CabeceraAdr()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Buscar por número ONU o nombre",
                            style = MaterialTheme.typography.titleMedium,
                            color = AzulInstitucional,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = viewModel.query,
                            onValueChange = { viewModel.onQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Número ONU o nombre (ej. 1001, gasolina, amoníaco)") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                            trailingIcon = {
                                if (viewModel.query.isNotBlank()) {
                                    IconButton(onClick = { viewModel.limpiar() }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        androidx.compose.material3.Button(
                            onClick = { 
                                Log.d("AdrConsultaOnu", "Button clicked, query='${viewModel.query}', isLoading=${viewModel.isLoading}")
                                viewModel.buscar() 
                            },
                            enabled = viewModel.query.isNotBlank() && !viewModel.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.isLoading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Buscar")
                            }
                        }
                    }
                }

                if (viewModel.error != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (viewModel.results.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${viewModel.results.size} resultado(s) encontrado(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundario
                            )
                            viewModel.results.forEach { item ->
                                ResultadoItem(
                                    item = item,
                                    onClick = { viewModel.seleccionar(item) }
                                )
                            }
                        }
                    }
                }

                viewModel.selectedItem?.let { selected ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${selected.numeroOnu} - ${selected.nombre}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AzulInstitucional,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selected.clase?.isNotBlank() == true) {
                                    Text(
                                        "Clase ${selected.clase}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                            selected.nota?.let { nota ->
                                if (nota.isNotBlank()) {
                                    Text(
                                        "Nota: $nota",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (viewModel.detalleItems.isNotEmpty()) {
                                Text(
                                    "Detalle (Tabla A):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AzulInstitucional
                                )
                                viewModel.detalleItems.forEach { detalle ->
                                    DetalleTablaAItem(item = detalle)
                                }
                            } else {
                                Text(
                                    "Sin datos en Tabla A para este número ONU",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoSecundario
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CabeceraAdr() {
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
            Icon(
                Icons.Filled.LocalFireDepartment,
                null,
                tint = AzulInstitucional,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text("Consulta cód. ONU - Tabla B ADR", fontWeight = FontWeight.Bold, color = AzulInstitucional)
                Text(
                    "Busca sustancias y artículos por número ONU o nombre",
                    color = AzulInstitucional,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ResultadoItem(
    item: AdrTablaBItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AzulClaro),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = AzulInstitucional,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.numeroOnu} - ${item.nombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.clase?.isNotBlank() == true) {
                    Text(
                        text = "Clase ${item.clase}${item.nota?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = TextoSecundario
            )
        }
    }
}

@Composable
private fun DetalleTablaAItem(item: AdrTablaAItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(
                    "ONU ${item.numeroOnu} | ${item.codigoClasificacion?.let { "Código: $it" } ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextoSecundario
                )
            }
            
            // Placa naranja ADR
            PlacaNaranjaAdr(
                numeroPeligro = item.numeroPeligro,
                numeroOnu = item.numeroOnu
            )
            
            // Etiquetas de peligro ADR
            EtiquetasPeligroAdr(item.etiquetas)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DatoItem("Clase", item.clase)
                DatoItem("Grupo emb.", item.grupoEmbalaje)
                DatoItem("Etiquetas", item.etiquetas)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DatoItem("Cat. transporte", item.categoriaTransporte)
                DatoItem("Código túnel", item.codigoTunel)
                DatoItem("Nº peligro", item.numeroPeligro)
            }
            item.cantidadesLimitadas?.let { if (it.isNotBlank()) DatoItem("Cant. limitadas", it) }
            item.cantidadesExceptuadas?.let { if (it.isNotBlank()) DatoItem("Cant. exceptuadas", it) }
            item.instruccionesEmbalaje?.let { if (it.isNotBlank()) DatoItem("Instr. embalaje", it) }
            item.disposicionesEspeciales?.let { if (it.isNotBlank()) DatoItem("Disp. especiales", it) }
            item.disposicionesTransporteBultos?.let { if (it.isNotBlank()) DatoItem("Transporte bultos", it) }
            item.disposicionesTransporteGranel?.let { if (it.isNotBlank()) DatoItem("Transporte granel", it) }
            item.disposicionesCargaDescarga?.let { if (it.isNotBlank()) DatoItem("Carga/descarga", it) }
            item.disposicionesExplotacion?.let { if (it.isNotBlank()) DatoItem("Explotación", it) }
        }
    }
}

@Composable
private fun PlacaNaranjaAdr(
    numeroPeligro: String?,
    numeroOnu: String?
) {
    val peligro = numeroPeligro?.takeIf { it.isNotBlank() } ?: "—"
    val onu = numeroOnu?.takeIf { it.isNotBlank() } ?: "—"
    val naranjaAdr = Color(0xFFFF9800)

    // ADR 5.3.2: panel rectangular de 40 x 30 cm, reborde negro de 15 mm
    // y dígitos negros de 100 mm de altura (escala proporcional 1:4 del ancho)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .aspectRatio(4f / 3f)
            .background(Color.Black)
    ) {
        val marco = maxWidth * 0.0375f // 15 mm sobre 40 cm de ancho
        val alturaDigito = with(LocalDensity.current) { (maxWidth * 0.25f).toSp() } // 100 mm sobre 40 cm

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(marco)
        ) {
            // Mitad superior: número de identificación del peligro
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(naranjaAdr),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peligro,
                    fontSize = alturaDigito,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            // Banda divisoria central negra
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(marco)
                    .background(Color.Black)
            )
            // Mitad inferior: número ONU
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(naranjaAdr),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = onu,
                    fontSize = alturaDigito,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EtiquetasPeligroAdr(etiquetas: String?) {
    etiquetas?.takeIf { it.isNotBlank() }?.let { etiquetasStr ->
        val labels = parseEtiquetas(etiquetasStr)
        if (labels.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Text(
                    "Etiquetas de peligro:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextoSecundario
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    labels.forEach { label ->
                        EtiquetaDiamanteAdr(label)
                    }
                }
            }
        }
    }
}

private fun parseEtiquetas(etiquetas: String): List<EtiquetaAdr> {
    val result = mutableListOf<EtiquetaAdr>()
    // Formato: "1", "1.4", "1 +8", "1 +6.1 +8", etc.
    val parts = etiquetas.split(" +").map { it.trim() }
    parts.forEachIndexed { index, part ->
        val classNumber = part.takeWhile { it.isDigit() || it == '.' }
        val subclass = if (part.contains('.')) part.substringAfter('.') else null
        val numericClass = classNumber.toDoubleOrNull()?.toInt() ?: 0
        val isSubsidiary = index > 0
        result.add(EtiquetaAdr(
            clase = numericClass,
            subclase = subclass,
            esRiesgoSubsidiario = isSubsidiary
        ))
    }
    return result
}

data class EtiquetaAdr(
    val clase: Int,
    val subclase: String?,
    val esRiesgoSubsidiario: Boolean = false
)

private fun etiquetaFileName(clase: Int, subclase: String?): String? = when (clase) {
    1 -> when (subclase) {
        "3" -> "etiqueta-clase-1.3.png"
        "4" -> "etiqueta-clase-1.4.png"
        "5" -> "etiqueta-clase-1.5.png"
        "6" -> "etiqueta-clase-1.6.png"
        else -> "etiqueta-clase-1.png" // Clase 1 genérica para divisiones 1.1 y 1.2
    }
    2 -> when (subclase) {
        "1" -> "etiqueta-clase-2.1.png"
        "2" -> "etiqueta-clase-2.2.png"
        "3" -> "etiqueta-clase-2.3.png"
        else -> null
    }
    3 -> "etiqueta-clase-3.png"
    4 -> when (subclase) {
        "1" -> "etiqueta-clase-4.1.png"
        "2" -> "etiqueta-clase-4.2.png"
        "3" -> "etiqueta-clase-4.3.png"
        else -> null
    }
    5 -> when (subclase) {
        "1" -> "etiqueta-clase-5.1.png"
        "2" -> "etiqueta-clase-5.2.png"
        else -> null
    }
    6 -> when (subclase) {
        "1" -> "etiqueta-clase-6.1.png"
        "2" -> "etiqueta-clase-6.2.png"
        else -> null
    }
    7 -> "etiqueta-clase-7-generica.webp"
    8 -> "etiqueta-clase-8.png"
    9 -> "etiqueta-clase-9.png"
    else -> null
}

private fun loadLabelImage(context: Context, fileName: String): ImageBitmap? = try {
    context.assets.open("PlacasEtiquetas/$fileName").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

@Composable
private fun EtiquetaDiamanteAdr(etiqueta: EtiquetaAdr) {
    val context = LocalContext.current
    val fileName = etiquetaFileName(etiqueta.clase, etiqueta.subclase)
    val imageBitmap = remember(fileName) {
        fileName?.let { loadLabelImage(context, it) }
    }
    val labelSize = 72.dp
    val denominacion = "${etiqueta.clase}${etiqueta.subclase?.let { ".$it" } ?: ""}"

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Etiqueta ADR clase $denominacion",
            modifier = Modifier.size(labelSize)
        )
    } else {
        // Aviso cuando no se dispone de la imagen de la etiqueta
        Box(
            modifier = Modifier
                .size(labelSize)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFECECEC))
                .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(8.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Clase $denominacion\nEtiqueta no disponible",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DatoItem(label: String, value: String?) {
    value?.let { v ->
        if (v.isNotBlank()) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                Text(v, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}