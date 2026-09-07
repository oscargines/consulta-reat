package com.oscar.consultareat.ui.baremo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oscar.consultareat.data.baremo.BaremoIndice
import com.oscar.consultareat.data.baremo.BaremoInfraccion
import com.oscar.consultareat.data.baremo.BaremoRepository
import com.oscar.consultareat.ui.theme.AmarilloPastel
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.FondoGris
import com.oscar.consultareat.ui.theme.RojoPastel
import com.oscar.consultareat.ui.theme.TextoSecundario
import com.oscar.consultareat.ui.theme.VerdePastel

private val gravedades = listOf(BaremoRepository.TODAS, "MUY GRAVE", "GRAVE", "LEVE")

@Composable
fun BaremoScreen(
    state: BaremoUiState,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSeverityChange: (String) -> Unit,
    onIndexChange: (BaremoIndice?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarIndices by remember { mutableStateOf(false) }
    var detalle by remember { mutableStateOf<BaremoInfraccion?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = FondoGris,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Baremo sancionador",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AzulInstitucional
                    )
                    Text("Versión 7.3", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                Icon(Icons.Filled.Gavel, contentDescription = null, tint = AzulInstitucional)
            }
        }
    ) { padding ->
        if (state.cargando) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = AzulInstitucional)
                Spacer(Modifier.height(12.dp))
                Text("Cargando infracciones...", color = TextoSecundario)
            }
        } else if (state.error != null) {
            ErrorBaremo(state.error, onRetry, Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    "Consulta de infracciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Busca por código, concepto, norma o precepto sancionador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = state.busqueda,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Buscar en el baremo") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                )

                Spacer(Modifier.height(12.dp))
                IndiceSelector(
                    selected = state.indiceSeleccionado,
                    indices = state.indices,
                    expanded = mostrarIndices,
                    onToggle = { mostrarIndices = !mostrarIndices },
                    onSelect = {
                        onIndexChange(it)
                        mostrarIndices = false
                    }
                )

                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    gravedades.forEach { gravedad ->
                        FilterChip(
                            selected = state.gravedad == gravedad,
                            onClick = { onSeverityChange(gravedad) },
                            label = { Text(if (gravedad == BaremoRepository.TODAS) "Todas" else gravedad) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AzulClaro,
                                selectedLabelColor = AzulInstitucional
                            )
                        )
                    }
                }

                Text(
                    resultadoLabel(state.resultados.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                if (state.resultados.isEmpty()) {
                    EmptyBaremo()
                } else {
                    // El repositorio ya ordena por índice, subíndice y código; aquí se
                    // conserva ese orden y se presenta cada bloque de forma agrupada.
                    val grupos = state.resultados.groupBy {
                        "${it.indiceAcronimo}|${it.subindiceAcronimo}|${it.subindiceConcepto}"
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(grupos.entries.toList(), key = { it.key }) { grupo ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                IndiceGroupHeader(grupo.value.first())
                                grupo.value.forEach { infraccion ->
                                    InfraccionCard(infraccion) { detalle = infraccion }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detalle?.let { infraccion ->
        InfraccionDetailDialog(infraccion) { detalle = null }
    }
}

@Composable
private fun IndiceSelector(
    selected: BaremoIndice?,
    indices: List<BaremoIndice>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (BaremoIndice?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Índice", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
            Text(
                selected?.etiqueta ?: "Todos los índices",
                style = MaterialTheme.typography.titleSmall,
                color = AzulInstitucional,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                IndiceListItem("Todos los índices", selected == null) { onSelect(null) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(indices, key = { it.id }) { indice ->
                        IndiceListItem(indice.etiqueta, selected?.id == indice.id) {
                            onSelect(indice)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndiceListItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) AzulClaro else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) AzulInstitucional else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IndiceGroupHeader(item: BaremoInfraccion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            "Índice ${item.indiceAcronimo}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AzulInstitucional
        )
        Text(
            "Subíndice ${item.subindiceAcronimo} · ${item.subindiceConcepto}",
            style = MaterialTheme.typography.labelMedium,
            color = TextoSecundario
        )
    }
}

@Composable
private fun InfraccionCard(item: BaremoInfraccion, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.codigo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzulInstitucional,
                    modifier = Modifier.weight(1f)
                )
                SeverityBadge(item.gravedad)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.concepto, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1C1C1E))
            Spacer(Modifier.height(8.dp))
            Text(
                "${item.indiceAcronimo}.${item.subindiceAcronimo} · ${item.subindiceConcepto}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
            item.cuantia?.let {
                Text(
                    "Cuantía: ${it} €",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SeverityBadge(gravedad: String) {
    val (background, foreground) = when (gravedad) {
        "MUY GRAVE" -> RojoPastel to Color(0xFFC62828)
        "GRAVE" -> AmarilloPastel to Color(0xFFE65100)
        else -> VerdePastel to Color(0xFF2E7D32)
    }
    Text(
        gravedad,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = foreground,
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun InfraccionDetailDialog(item: BaremoInfraccion, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(item.codigo, color = AzulInstitucional, fontWeight = FontWeight.Bold)
                SeverityBadge(item.gravedad)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailValue("Concepto", item.concepto)
                DetailValue("Índice", "${item.indiceAcronimo} - ${item.indiceDefinicion}")
                DetailValue("Subíndice", "${item.subindiceAcronimo} - ${item.subindiceConcepto}")
                DetailValue("Normas infringidas", item.normasInfringidas)
                DetailValue("Norma sancionadora", item.normaSancionadora)
                DetailValue("Precepto sancionador", item.preceptoSancionador)
                DetailValue("Cuantía", item.cuantia?.let { "$it €" })
                DetailValue("Observaciones", item.observaciones)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun DetailValue(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyBaremo() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = TextoSecundario, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text("No se encontraron infracciones", fontWeight = FontWeight.Bold)
        Text("Prueba a cambiar los filtros o el término de búsqueda.", color = TextoSecundario)
    }
}

@Composable
private fun ErrorBaremo(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No se pudo cargar el baremo", fontWeight = FontWeight.Bold)
        Text(message, color = TextoSecundario, modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

private fun resultadoLabel(count: Int): String =
    "$count ${if (count == 1) "infracción encontrada" else "infracciones encontradas"}"
