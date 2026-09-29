package com.oscar.consultareat.ui.adr

import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.consultareat.R
import com.oscar.consultareat.data.adr.AdrDatabase
import com.oscar.consultareat.data.adr.AdrMercanciaCalculo
import com.oscar.consultareat.data.adr.AdrRepository
import com.oscar.consultareat.data.adr.AdrTablaAItem
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.FondoGris
import com.oscar.consultareat.ui.theme.TextoSecundario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdrCalculo1000PuntosViewModel(
    private val repository: AdrRepository
) : ViewModel() {
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<AdrTablaAItem>>(emptyList())
    var isSearching by mutableStateOf(false)
    var mercancias by mutableStateOf<List<AdrMercanciaCalculo>>(emptyList())
        private set
    var variantesOnu by mutableStateOf<List<AdrTablaAItem>>(emptyList())
        private set
    var seleccionPendiente by mutableStateOf<AdrTablaAItem?>(null)
        private set
    var error by mutableStateOf<String?>(null)

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }

    fun buscarMercancias() {
        if (searchQuery.trim().isBlank()) {
            searchResults = emptyList()
            return
        }
        isSearching = true
        error = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = repository.searchForCalculo(searchQuery.trim())
                error = if (res.isEmpty()) {
                    "No se ha encontrado ninguna materia con categoría de transporte para '${searchQuery.trim()}'."
                } else {
                    null
                }
                searchResults = res
            } catch (e: Exception) {
                error = "Error en la búsqueda: ${e.message}"
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    fun seleccionarResultado(item: AdrTablaAItem) {
        val variantes = searchResults.filter { it.numeroOnu == item.numeroOnu }
        if (variantes.size > 1) {
            variantesOnu = variantes
        } else {
            seleccionPendiente = item
        }
    }

    fun confirmarVariante(item: AdrTablaAItem) {
        variantesOnu = emptyList()
        seleccionPendiente = item
    }

    fun confirmarCantidad(cantidad: Double, unidad: String) {
        seleccionPendiente?.let {
            mercancias = mercancias + AdrMercanciaCalculo(it, cantidad, unidad)
            searchQuery = ""
            searchResults = emptyList()
        }
        seleccionPendiente = null
    }

    fun cancelarSeleccion() {
        variantesOnu = emptyList()
        seleccionPendiente = null
    }

    fun actualizarCantidad(index: Int, cantidad: Double) {
        mercancias = mercancias.mapIndexed { i, m ->
            if (i == index) m.copy(cantidad = cantidad) else m
        }
    }

    fun actualizarUnidad(index: Int, unidad: String) {
        mercancias = mercancias.mapIndexed { i, m ->
            if (i == index) m.copy(unidad = unidad) else m
        }
    }

    fun eliminarMercancia(index: Int) {
        mercancias = mercancias.filterIndexed { i, _ -> i != index }
    }

    fun limpiar() {
        mercancias = emptyList()
        searchQuery = ""
        searchResults = emptyList()
        error = null
    }

    val totalPuntos: Double
        get() = mercancias.sumOf { it.puntos }

    val dentroExencion: Boolean
        get() = tieneMercancias &&
            totalPuntos <= 1000.0 &&
            mercancias.none {
                it.tablaAItem.categoriaTransporteNumero == 0 && it.cantidad > 0
            }

    val tieneMercancias: Boolean
        get() = mercancias.isNotEmpty()

    // ADR 5.4.1: al aplicar 1.1.3.6 hay que indicar cantidad y valor calculado por categoría
    val desglosePorCategoria: List<Pair<Int, Pair<Double, Double>>>
        get() {
            val resumen = mutableMapOf<Int, Pair<Double, Double>>()
            mercancias.forEach { m ->
                val cat = m.tablaAItem.categoriaTransporteNumero ?: return@forEach
                val prev = resumen[cat] ?: (0.0 to 0.0)
                resumen[cat] = (prev.first + m.cantidad) to (prev.second + m.puntos)
            }
            return resumen.entries.sortedBy { it.key }.map { it.key to it.value }
        }

    val advertencias: List<String>
        get() {
            val warns = mutableListOf<String>()
            mercancias.forEachIndexed { index, m ->
                val cat = m.tablaAItem.categoriaTransporteNumero
                if (cat == null) {
                    warns.add("${index + 1}. ${m.nombreDisplay}: Sin categoría de transporte definida")
                }
                if (cat == 0 && m.cantidad > 0) {
                    warns.add("${index + 1}. ${m.nombreDisplay}: Categoría de transporte 0 - la exención 1.1.3.6 no permite transportarlas")
                }
                if (m.tablaAItem.clase?.startsWith("1") == true && cat != 0) {
                    warns.add("${index + 1}. ${m.nombreDisplay}: Explosivos (Clase 1) - verificar reglamento específico")
                }
                if (m.tablaAItem.clase?.startsWith("7") == true) {
                    warns.add("${index + 1}. ${m.nombreDisplay}: Material radiactivo (Clase 7) - regla 1000 puntos no aplica")
                }
                if (m.cantidad <= 0) {
                    warns.add("${index + 1}. ${m.nombreDisplay}: Cantidad debe ser mayor que 0")
                }
            }
            return warns
        }
}

class AdrCalculo1000PuntosViewModelFactory(
    private val repository: AdrRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AdrCalculo1000PuntosViewModel(repository) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdrCalculo1000PuntosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AdrRepository(AdrDatabase(context)) }
    val viewModel: AdrCalculo1000PuntosViewModel = viewModel(factory = AdrCalculo1000PuntosViewModelFactory(repository))

    Scaffold(
        modifier = modifier,
        containerColor = FondoGris,
        topBar = {
            TopAppBar(
                title = { Text("Cálculo regla de los 1000 puntos") },
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
                CabeceraCalculo()

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

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Añadir mercancía",
                            style = MaterialTheme.typography.titleMedium,
                            color = AzulInstitucional,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Buscar por ONU o nombre (ej. 1203, gasolina, propano)") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                            trailingIcon = {
                                if (viewModel.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.searchQuery = ""; viewModel.searchResults = emptyList() }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.buscarMercancias() }),
                            shape = RoundedCornerShape(12.dp)
                        )

                        androidx.compose.material3.Button(
                            onClick = { viewModel.buscarMercancias() },
                            enabled = viewModel.searchQuery.isNotBlank() && !viewModel.isSearching,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (viewModel.isSearching) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Buscar")
                            }
                        }

                        if (viewModel.isSearching) {
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (viewModel.searchResults.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                viewModel.searchResults.take(10).forEach { item ->
                                    ResultadoBusquedaCalculo(item = item) { viewModel.seleccionarResultado(item) }
                                }
                            }
                        }
                    }
                }

                if (viewModel.tieneMercancias) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Mercancías añadidas (${viewModel.mercancias.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = AzulInstitucional,
                                fontWeight = FontWeight.Bold
                            )

                            viewModel.mercancias.forEachIndexed { index, mercancia ->
                                MercanciaCalculoItem(
                                    index = index,
                                    mercancia = mercancia,
                                    onCantidadChange = { viewModel.actualizarCantidad(index, it) },
                                    onUnidadChange = { viewModel.actualizarUnidad(index, it) },
                                    onEliminar = { viewModel.eliminarMercancia(index) }
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            TotalPuntosCard(
                                total = viewModel.totalPuntos,
                                dentroExencion = viewModel.dentroExencion,
                                desglose = viewModel.desglosePorCategoria
                            )

                            if (viewModel.advertencias.isNotEmpty()) {
                                AdvertenciasCard(advertencias = viewModel.advertencias)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.limpiar() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Limpiar todo")
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Filled.Calculate, contentDescription = "", tint = TextoSecundario, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No hay mercancías añadidas",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextoSecundario
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Busca y añade mercancías para calcular los puntos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundario,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Diálogo: desambiguación de grupo de embalaje para un mismo ONU
            viewModel.variantesOnu.takeIf { it.isNotEmpty() }?.let { variantes ->
                DialogSeleccionGrupoAdr(
                    variantes = variantes,
                    onSeleccion = viewModel::confirmarVariante,
                    onCancelar = viewModel::cancelarSeleccion
                )
            }

            // Diálogo: solicitud de cantidad total (litros o kilogramos)
            viewModel.seleccionPendiente?.let { item ->
                DialogCantidadAdr(
                    item = item,
                    onConfirmar = viewModel::confirmarCantidad,
                    onCancelar = viewModel::cancelarSeleccion
                )
            }
        }
    }
}

@Composable
private fun CabeceraCalculo() {
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
            Icon(Icons.Filled.LocalFireDepartment, null, tint = AzulInstitucional, modifier = Modifier.size(36.dp))
            Column {
                Text("Regla de los 1000 puntos - ADR 1.1.3.6", fontWeight = FontWeight.Bold, color = AzulInstitucional)
                Text(
                    "Calcula si tu carga está exenta según la regla de los 1000 puntos",
                    color = AzulInstitucional,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun factorCategoriaCalculo(catNum: Int?): Double = when (catNum) {
    1 -> 50.0
    2 -> 3.0
    3 -> 1.0
    else -> 0.0 // Cat. 0 y 4 no computan
}

@Composable
private fun ResultadoBusquedaCalculo(
    item: AdrTablaAItem,
    onClick: () -> Unit
) {
    val catNum = item.categoriaTransporteNumero
    val factor = factorCategoriaCalculo(catNum)

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
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = AzulInstitucional, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${item.numeroOnu} - ${item.nombre}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Clase ${item.clase ?: "-"}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    Text(
                        if (factor > 0) "Cat. ${catNum ?: "—"} (×$factor)" else "Cat. ${catNum ?: "—"} (no computa)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                    if (item.grupoEmbalaje?.isNotBlank() == true) {
                        Text("GE ${item.grupoEmbalaje}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                }
            }
            Icon(Icons.Filled.Add, contentDescription = null, tint = AzulInstitucional)
        }
    }
}

@Composable
private fun MercanciaCalculoItem(
    index: Int,
    mercancia: AdrMercanciaCalculo,
    onCantidadChange: (Double) -> Unit,
    onUnidadChange: (String) -> Unit,
    onEliminar: () -> Unit
) {
    val item = mercancia.tablaAItem
    val catNum = item.categoriaTransporteNumero
    val factor = mercancia.factorCategoria

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${index + 1}.", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                Text("${item.numeroOnu} - ${item.nombre}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = TextoSecundario)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = mercancia.cantidad.toString(),
                    onValueChange = { onCantidadChange(it.toDoubleOrNull() ?: 0.0) },
                    modifier = Modifier
                        .weight(1f)
                        .width(100.dp),
                    singleLine = true,
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = mercancia.unidad,
                    onValueChange = onUnidadChange,
                    modifier = Modifier
                        .weight(1f)
                        .width(80.dp),
                    singleLine = true,
                    label = { Text("Unidad") },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DatoPequeno("Clase", item.clase ?: "—")
                DatoPequeno("Cat. transporte", catNum?.toString() ?: "—")
                DatoPequeno("Factor", if (factor > 0) factor.toString() else "No computa")
                DatoPequeno("Puntos", "%.1f".format(mercancia.puntos))
            }

            if (item.disposicionesEspeciales != null && item.disposicionesEspeciales.isNotBlank()) {
                Text("Disp. especiales: ${item.disposicionesEspeciales}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DatoPequeno(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TotalPuntosCard(
    total: Double,
    dentroExencion: Boolean,
    desglose: List<Pair<Int, Pair<Double, Double>>>
) {
    val colorOk = Color(0xFF2E7D32)
    val colorKo = Color(0xFFC62828)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dentroExencion) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "Total puntos: %.1f".format(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (dentroExencion) colorOk else colorKo
            )
            Spacer(Modifier.height(8.dp))
            if (dentroExencion) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "", tint = colorOk, modifier = Modifier.size(24.dp))
                    Text(
                        "DENTRO DE LA EXENCIÓN (≤ 1000 puntos)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorOk
                    )
                }
                Text(
                    "La carga se beneficia de las exenciones del 1.1.3.6 ADR",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorOk,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "", tint = colorKo, modifier = Modifier.size(24.dp))
                    Text(
                        "FUERA DE LA EXENCIÓN (> 1000 puntos)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorKo
                    )
                }
                Text(
                    "La carga NO se beneficia de las exenciones del 1.1.3.6 ADR. Se aplican todas las disposiciones del ADR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorKo,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (desglose.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Desglose por categoría de transporte",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (dentroExencion) colorOk else colorKo
                )
                Spacer(Modifier.height(4.dp))
                desglose.forEach { (cat, datos) ->
                    val (cantidad, puntos) = datos
                    Text(
                        "Cat. $cat: cantidad ${"%.1f".format(cantidad)} · puntos ${"%.1f".format(puntos)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dentroExencion) colorOk else colorKo
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "ADR 5.4.1: indique en la carta de porte la cantidad total y el valor calculado por categoría de transporte.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSecundario,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AdvertenciasCard(advertencias: List<String>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "", tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                Text("Avisos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
            }
            advertencias.forEach { adv ->
                Text("• $adv", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF57F17))
            }
        }
    }
}

@Composable
private fun DialogSeleccionGrupoAdr(
    variantes: List<AdrTablaAItem>,
    onSeleccion: (AdrTablaAItem) -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Seleccione grupo de embalaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Para el N.º ONU ${variantes.firstOrNull()?.numeroOnu ?: ""} existen varias entradas. Seleccione la particularidad correspondiente:",
                    style = MaterialTheme.typography.bodySmall
                )
                variantes.forEach { v ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeleccion(v) }
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Clase ${v.clase ?: "—"} · Grupo embalaje ${v.grupoEmbalaje ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Cód. clasificación: ${v.codigoClasificacion ?: "—"} · Cat. transporte: ${v.categoriaTransporteNumero ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoSecundario
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogCantidadAdr(
    item: AdrTablaAItem,
    onConfirmar: (Double, String) -> Unit,
    onCancelar: () -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("kg") }
    val cantidadDouble = cantidad.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cantidad transportada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${item.numeroOnu} - ${item.nombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Clase ${item.clase ?: "—"} · Cat. transporte ${item.categoriaTransporteNumero ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Cantidad total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = unidad == "l",
                        onClick = { unidad = "l" },
                        label = { Text("Litros (l)") }
                    )
                    FilterChip(
                        selected = unidad == "kg",
                        onClick = { unidad = "kg" },
                        label = { Text("Kilogramos (kg)") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(cantidadDouble ?: 0.0, unidad) },
                enabled = (cantidadDouble ?: 0.0) > 0.0
            ) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}