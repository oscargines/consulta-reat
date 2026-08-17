package com.oscar.consultareat.ui.acercade

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.oscar.consultareat.BuildConfig
import com.oscar.consultareat.R
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.BordeSuave
import com.oscar.consultareat.ui.theme.TextoSecundario

private val FUENTES = listOf(
    Triple(
        "Consulta pública del REAT",
        "apps.fomento.gob.es/crgt",
        "https://apps.fomento.gob.es/crgt/servlet/ServletController?modulo=datosconsulta&accion=inicio&lang=es&estilo=default"
    ),
    Triple(
        "NAP - Plataforma de Datos Abiertos del Transporte",
        "nap.transportes.gob.es",
        "https://nap.transportes.gob.es"
    ),
    Triple(
        "datos.gob.es - Catálogo de datos abiertos",
        "datos.gob.es",
        "https://datos.gob.es"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcercaDeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "Icono de la aplicación",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Consulta REAT",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Versión ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextoSecundario
                    )
                }
            }

            Text(
                text = "Consulta REAT es una aplicación no oficial que facilita la consulta del " +
                    "Registro de Empresas y Actividades de Transporte (REAT).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzulClaro, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Toda la información obtenida procede de fuentes abiertas y públicas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AzulInstitucional
                )
                Text(
                    text = "La aplicación no crea ni altera los datos: solo los muestra tal y como " +
                        "los publican las fuentes oficiales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AzulInstitucional
                )
            }

            HorizontalDivider(color = BordeSuave)

            Text(
                text = "Fuentes de información",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            FUENTES.forEach { (nombre, dominio, url) ->
                FilaFuente(
                    nombre = nombre,
                    dominio = dominio,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    }
                )
            }

            HorizontalDivider(color = BordeSuave)

            Text(
                text = "Aviso legal: esta aplicación no está afiliada al Ministerio de Transportes " +
                    "y Movilidad Sostenible. Los datos pueden contener errores o estar " +
                    "desactualizados; verifícalos siempre en la fuente oficial antes de tomar " +
                    "cualquier decisión.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilaFuente(
    nombre: String,
    dominio: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dominio,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario
            )
        }
        Icon(
            imageVector = Icons.Outlined.OpenInNew,
            contentDescription = "Abrir fuente",
            tint = AzulInstitucional,
            modifier = Modifier.size(18.dp)
        )
    }
}
