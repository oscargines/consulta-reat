package com.oscar.consultareat.ui.inicio

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.FondoGris
import com.oscar.consultareat.ui.theme.TextoSecundario

@Composable
fun PantallaPrincipal(
    onConsultas: () -> Unit,
    onHistorial: () -> Unit,
    onExcepciones: () -> Unit,
    onInspeccionTransEscolar: () -> Unit,
    onBaremo: () -> Unit,
    onAcercaDe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logo = remember {
        try {
            context.assets.open("logo grande.jpg").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FondoGris)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        logo?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Logo Consulta REAT",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Text(
            text = "Consulta REAT",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Consulta pública del Registro de Empresas y\nActividades de Transporte",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        TarjetaAcceso(
            titulo = "Consultas",
            subtitulo = "Realiza una consulta pública del REAT",
            icono = Icons.Filled.Search,
            onClick = onConsultas
        )
        TarjetaAcceso(
            titulo = "Historial",
            subtitulo = "Consulta tus búsquedas anteriores",
            icono = Icons.Filled.History,
            onClick = onHistorial
        )
        TarjetaAcceso(
            titulo = "Consulta excepciones",
            subtitulo = "Excepciones a la obligación de título habilitante (Art. 33 ROTC)",
            icono = Icons.Filled.Article,
            onClick = onExcepciones
        )
        TarjetaAcceso(
            titulo = "Baremo sancionador",
            subtitulo = "Busca infracciones, gravedad, normas y cuantías",
            icono = Icons.Filled.Gavel,
            onClick = onBaremo
        )
        TarjetaAcceso(
            titulo = "Inspección Trans Escolar",
            subtitulo = "Comprueba los requisitos del Real Decreto 443/2001",
            icono = Icons.Filled.DirectionsBus,
            onClick = onInspeccionTransEscolar
        )
        TarjetaAcceso(
            titulo = "Acerca de",
            subtitulo = "Información legal y fuentes de datos",
            icono = Icons.Outlined.Info,
            onClick = onAcercaDe
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TarjetaAcceso(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
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
                    tint = AzulInstitucional
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextoSecundario
            )
        }
    }
}
