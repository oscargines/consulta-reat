package com.oscar.consultareat.ui.resultado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.oscar.consultareat.domain.ConsultaResultado
import com.oscar.consultareat.domain.DatoItem
import com.oscar.consultareat.ui.theme.AmarilloEstado
import com.oscar.consultareat.ui.theme.AmarilloPastel
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.BordeSuave
import com.oscar.consultareat.ui.theme.RojoEstado
import com.oscar.consultareat.ui.theme.RojoPastel
import com.oscar.consultareat.ui.theme.TextoSecundario
import com.oscar.consultareat.ui.theme.VerdeEstado
import com.oscar.consultareat.ui.theme.VerdePastel
import com.oscar.consultareat.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoScreen(
    uiState: UiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultado de la consulta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                is UiState.Success -> ResultadoContent(uiState.resultado)
                is UiState.Error -> EstadoError(uiState.message, onBack)
                else -> {
                    Text(
                        "Selecciona una consulta",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ResultadoContent(resultado: ConsultaResultado) {
    val vacio = resultado.identidadValor == null &&
        resultado.autorizaciones.isEmpty() &&
        resultado.vehiculos.isEmpty() &&
        resultado.competenciaProfesional.isEmpty() &&
        resultado.consejeroSeguridad.isEmpty() &&
        resultado.capConductor.isEmpty() &&
        resultado.operadores.isEmpty() &&
        resultado.conjuntosDatos.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (resultado.identidadValor != null) {
            TarjetaIdentidad(resultado)
        }

        if (resultado.autorizaciones.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "Autorizaciones",
                icono = Icons.Filled.LocalShipping,
                items = resultado.autorizaciones
            )
        }

        if (resultado.vehiculos.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "Vehículos",
                icono = Icons.Filled.LocalShipping,
                items = resultado.vehiculos
            )
        }

        if (resultado.competenciaProfesional.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "Competencia Profesional",
                icono = Icons.Filled.WorkspacePremium,
                items = resultado.competenciaProfesional
            )
        }

        if (resultado.consejeroSeguridad.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "Consejero de Seguridad",
                icono = Icons.Filled.Badge,
                items = resultado.consejeroSeguridad
            )
        }

        if (resultado.capConductor.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "CAP del Conductor",
                icono = Icons.Filled.Badge,
                items = resultado.capConductor
            )
        }

        if (resultado.operadores.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "Operadores encontrados",
                icono = Icons.Filled.Business,
                items = resultado.operadores
            )
        }

        if (resultado.conjuntosDatos.isNotEmpty()) {
            TarjetaSeccion(
                titulo = "Conjuntos de datos asociados",
                icono = Icons.Filled.FolderOpen,
                items = resultado.conjuntosDatos
            )
        }

        if (vacio) {
            Text(
                "No se han encontrado resultados para esta consulta",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        resultado.comentario?.takeIf { it.isNotBlank() }?.let { comentario ->
            TarjetaComentario(comentario)
        }
    }
}

@Composable
private fun TarjetaComentario(comentario: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comentario de la búsqueda",
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = comentario,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TarjetaIdentidad(resultado: ConsultaResultado) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconoCircular(Icons.Filled.VerifiedUser)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = resultado.identidadLabel ?: "Empresa / Titular",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = resultado.identidadValor ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TarjetaSeccion(
    titulo: String,
    icono: ImageVector,
    items: List<DatoItem>
) {
    var expandida by remember { mutableStateOf(false) }
    val estado = estadoDe(items)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(estado.color)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expandida = !expandida }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconoCircular(icono)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titulo,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        tipoDe(items)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        numeroDe(items)?.let { (label, valor) ->
                            FilaIcono(Icons.Filled.Description, label, valor)
                        }
                        fechaDe(items)?.let { (label, valor) ->
                            FilaIcono(Icons.Filled.Event, label, valor)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (estado != EstadoTarjeta.SIN_ESTADO) {
                            BadgePill(estado)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "Ver detalle",
                            tint = TextoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (expandida) {
                HorizontalDivider(color = BordeSuave)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEach { item ->
                        DatoItem(
                            label = item.etiqueta,
                            value = item.valor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconoCircular(icono: ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(AzulClaro),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = AzulInstitucional,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun FilaIcono(icono: ImageVector, label: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = TextoSecundario,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextoSecundario
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BadgePill(estado: EstadoTarjeta) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(estado.pastel)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = estado.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = estado.color
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = estado.color,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun EstadoError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
fun DatoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private enum class EstadoTarjeta(
    val etiqueta: String,
    val color: Color,
    val pastel: Color
) {
    VIGENTE("Vigente", VerdeEstado, VerdePastel),
    EN_TRAMITE("En trámite", AmarilloEstado, AmarilloPastel),
    CADUCADA("Caducada", RojoEstado, RojoPastel),
    SIN_ESTADO("", TextoSecundario, BordeSuave)
}

private val regexCaducada = Regex(
    "caduc|revoc|vencid|suspend|denegad|baja|anulad|extinguid",
    RegexOption.IGNORE_CASE
)
private val regexTramite = Regex(
    "en tr[aá]mite|pendiente|solicitad|tr[aá]mit|renovaci",
    RegexOption.IGNORE_CASE
)

private fun estadoDe(items: List<DatoItem>): EstadoTarjeta {
    val texto = items.joinToString(" ") { "${it.etiqueta} ${it.valor}" }
    if (regexCaducada.containsMatchIn(texto)) return EstadoTarjeta.CADUCADA
    if (regexTramite.containsMatchIn(texto)) return EstadoTarjeta.EN_TRAMITE
    for (item in items) {
        val etiqueta = item.etiqueta.lowercase()
        if (etiqueta.contains("validez") || etiqueta.contains("caducidad")) {
            val fecha = parseFecha(item.valor) ?: continue
            if (fecha.before(Calendar.getInstance().time)) return EstadoTarjeta.CADUCADA
        }
    }
    return EstadoTarjeta.VIGENTE
}

private fun parseFecha(s: String): Date? {
    val limpio = s.trim().replace("""\s+""".toRegex(), " ")
    for (formato in listOf("dd-MM-yyyy", "dd/MM/yyyy")) {
        try {
            return SimpleDateFormat(formato, Locale.getDefault()).parse(limpio)
        } catch (_: Exception) {
        }
    }
    return null
}

private fun numeroDe(items: List<DatoItem>): Pair<String, String>? {
    val preferida = items.firstOrNull {
        val e = it.etiqueta.lowercase()
        e.contains("matrícula") || e.contains("matricula") || e.contains("número") ||
            e.contains("numero") || e.contains("nº") || e.contains("expediente")
    }
    if (preferida != null) return preferida.etiqueta to preferida.valor
    return items.firstOrNull {
        val e = it.etiqueta.lowercase()
        e.contains("autorización") || e.contains("autorizacion")
    }?.let { it.etiqueta to it.valor }
}

private fun fechaDe(items: List<DatoItem>): Pair<String, String>? =
    items.firstOrNull {
        val e = it.etiqueta.lowercase()
        e.contains("validez") || e.contains("matriculaci") || e.contains("aprobaci") ||
            e.contains("adscripci") || e.contains("expedici")
    }?.let { it.etiqueta to it.valor }

private fun tipoDe(items: List<DatoItem>): String? =
    items.firstOrNull { it.etiqueta.lowercase().contains("tipo") }?.valor