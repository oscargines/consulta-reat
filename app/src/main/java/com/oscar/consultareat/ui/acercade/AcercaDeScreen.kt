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

private data class ReferenciaLegal(
    val titulo: String,
    val detalle: String,
    val url: String
)

private data class SeccionLegal(
    val titulo: String,
    val items: List<ReferenciaLegal>
)

private val REFERENCIAS_LEGALES = listOf(
    SeccionLegal(
        "Ordenación del transporte y registro REAT",
        listOf(
            ReferenciaLegal(
                "Ley 16/1987, de 30 de julio (LOTT)",
                "Ordenación de los Transportes Terrestres",
                "https://www.boe.es/buscar/act.php?id=BOE-A-1987-18180"
            ),
            ReferenciaLegal(
                "Real Decreto 1211/1990 (ROTT)",
                "Reglamento de la Ley de Ordenación de los Transportes Terrestres",
                "https://www.boe.es/buscar/act.php?id=BOE-A-1990-23936"
            ),
            ReferenciaLegal(
                "Reglamento (CE) n.º 1071/2009",
                "Acceso a la profesión de transportista",
                "https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX%3A32009R1071"
            ),
            ReferenciaLegal(
                "Reglamento (CE) n.º 1072/2009",
                "Acceso al mercado de transporte internacional de mercancías",
                "https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX%3A32009R1072"
            )
        )
    ),
    SeccionLegal(
        "Mercancías peligrosas (ADR)",
        listOf(
            ReferenciaLegal(
                "ADR 2025",
                "Acuerdo europeo relativo al transporte internacional de mercancías peligrosas por carretera",
                "https://unece.org/transport/dangerous-goods/adr-2025-vol-i-and-ii"
            ),
            ReferenciaLegal(
                "Real Decreto 97/2014, de 14 de febrero",
                "Operaciones de transporte de mercancías peligrosas por carretera en territorio español",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2014-2110"
            ),
            ReferenciaLegal(
                "Directiva 2008/68/CE",
                "Transporte terrestre de mercancías peligrosas",
                "https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX%3A32008L0068"
            ),
            ReferenciaLegal(
                "Manual de inspección de mercancías peligrosas",
                "Manual de inspección del transporte de mercancías peligrosas por carretera",
                "https://www.transportes.gob.es/transporte-terrestre/inspeccion-y-seguridad-en-el-transporte"
            )
        )
    ),
    SeccionLegal(
        "Transporte escolar y seguridad vial",
        listOf(
            ReferenciaLegal(
                "Real Decreto 443/2001, de 27 de abril",
                "Condiciones de seguridad en el transporte escolar y de menores",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2001-8503"
            ),
            ReferenciaLegal(
                "RDLeg 6/2015, de 30 de octubre (LSV)",
                "Tráfico, Circulación de Vehículos a Motor y Seguridad Vial",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2015-11733"
            ),
            ReferenciaLegal(
                "Real Decreto 1428/2003 (RGCir)",
                "Reglamento General de Circulación",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2003-21539"
            ),
            ReferenciaLegal(
                "Real Decreto 2822/1998 (RGVeh)",
                "Reglamento General de Vehículos",
                "https://www.boe.es/buscar/act.php?id=BOE-A-1999-3896"
            ),
            ReferenciaLegal(
                "Real Decreto 920/2017 (ITV)",
                "Inspección técnica de vehículos",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2017-12841"
            )
        )
    ),
    SeccionLegal(
        "Reutilización de datos y privacidad",
        listOf(
            ReferenciaLegal(
                "Ley 37/2007, de 16 de noviembre",
                "Reutilización de la información del sector público",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2007-19814"
            ),
            ReferenciaLegal(
                "Directiva (UE) 2019/1024",
                "Datos abiertos y reutilización de la información del sector público",
                "https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX%3A32019L1024"
            ),
            ReferenciaLegal(
                "Reglamento (UE) 2016/679 (RGPD)",
                "Protección de datos personales",
                "https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX%3A32016R0679"
            ),
            ReferenciaLegal(
                "Ley Orgánica 3/2018 (LOPDGDD)",
                "Protección de Datos Personales y garantía de los derechos digitales",
                "https://www.boe.es/buscar/act.php?id=BOE-A-2018-16673"
            )
        )
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
                        text = "Consulta Transportes",
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
                text = "Consulta Transportes es una aplicación no oficial de apoyo a la inspección del " +
                    "transporte por carretera. Agrupa la consulta pública del REAT, la consulta del ADR 2025 " +
                    "(códigos ONU, regla de los 1000 puntos, placas y etiquetas) y la guía de inspección del " +
                    "transporte escolar y de menores.",
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
                text = "Referencias legales",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            REFERENCIAS_LEGALES.forEach { seccion ->
                Text(
                    text = seccion.titulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 6.dp)
                )
                seccion.items.forEach { referencia ->
                    FilaFuente(
                        nombre = referencia.titulo,
                        dominio = referencia.detalle,
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(referencia.url))
                            )
                        }
                    )
                }
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
