package com.oscar.consultareat.ui.consulta

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.oscar.consultareat.data.client.RgtClient
import com.oscar.consultareat.domain.ConsultaRequest
import com.oscar.consultareat.domain.TipoConsulta
import com.oscar.consultareat.domain.TipoIdentificacion
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONTokener
import kotlin.coroutines.resume

private const val TAG = "ConsultaREAT.ConsultaScreen"

private val combinaciones = mapOf(
    TipoConsulta.AUTORIZACIONES to listOf(
        TipoIdentificacion.NIF,
        TipoIdentificacion.NOMBRE,
        TipoIdentificacion.AUTORIZACION,
        TipoIdentificacion.LICENCIA_INTERNACIONAL
    ),
    TipoConsulta.COMPETENCIA_CONSEJERO_CAP to listOf(
        TipoIdentificacion.NIF,
        TipoIdentificacion.NOMBRE
    ),
    TipoConsulta.VEHICULO to listOf(
        TipoIdentificacion.MATRICULA
    )
)

private fun etiquetaConsulta(tipo: TipoConsulta): String = when (tipo) {
    TipoConsulta.AUTORIZACIONES -> "Autorizaciones"
    TipoConsulta.COMPETENCIA_CONSEJERO_CAP -> "Competencia Profesional"
    TipoConsulta.VEHICULO -> "Vehículos"
}

private fun etiquetaIdentificacion(tipo: TipoIdentificacion): String = when (tipo) {
    TipoIdentificacion.NIF -> "NIF"
    TipoIdentificacion.NOMBRE -> "Nombre / Razón social"
    TipoIdentificacion.MATRICULA -> "Matrícula"
    TipoIdentificacion.AUTORIZACION -> "Nº autorización"
    TipoIdentificacion.LICENCIA_INTERNACIONAL -> "Licencia internacional"
}

private fun etiquetaIdentificacionCorta(tipo: TipoIdentificacion): String = when (tipo) {
    TipoIdentificacion.NIF -> "NIF"
    TipoIdentificacion.NOMBRE -> "Nombre"
    TipoIdentificacion.MATRICULA -> "Matrícula"
    TipoIdentificacion.AUTORIZACION -> "Autorización"
    TipoIdentificacion.LICENCIA_INTERNACIONAL -> "Licencia int."
}

private fun esFormularioEnRespuesta(html: String): Boolean {
    val tieneFormulario = html.contains("id=\"consultaForm\"") ||
        html.contains("id='consultaForm'") ||
        html.contains("name=\"tpsolic\"") ||
        html.contains("name='tpsolic'")
    val tieneResultado = html.contains("fichaIdentidad") ||
        html.contains("No se han encontrado resultados") ||
        html.contains("no tiene títulos habilitantes") ||
        html.contains("no tiene titulos habilitantes") ||
        html.contains("no constan datos") ||
        html.contains("sin datos")
    return tieneFormulario && !tieneResultado
}

@Composable
fun ConsultaScreen(
    uiState: UiState,
    onConsultar: (ConsultaRequest) -> Unit,
    onResultadoHtmlObtenido: (String, TipoConsulta) -> Unit,
    onVolverFormulario: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (uiState) {
            is UiState.CaptchaRequerido -> {
                CaptchaWebView(
                    request = uiState.request,
                    onResultadoHtmlObtenido = onResultadoHtmlObtenido,
                    onVolver = onVolverFormulario,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> ConsultaForm(
                uiState = uiState,
                onConsultar = onConsultar,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState is UiState.Loading) {
            IndicadorCarga()
        }
    }
}

@Composable
private fun IndicadorCarga() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Consultando en fuentes oficiales…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ConsultaForm(
    uiState: UiState,
    onConsultar: (ConsultaRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var tipoConsulta by remember { mutableStateOf(TipoConsulta.AUTORIZACIONES) }
    var tipoIdentificacion by remember { mutableStateOf(TipoIdentificacion.NIF) }
    var valor by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarScanner by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) mostrarScanner = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        Text(
            text = "Consulta pública",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Introduce los datos de la empresa o vehículo que deseas consultar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tipo de consulta",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    combinaciones.keys.forEach { tipo ->
                        OpcionTipoConsulta(
                            texto = etiquetaConsulta(tipo),
                            selected = tipoConsulta == tipo,
                            onClick = {
                                tipoConsulta = tipo
                                tipoIdentificacion = combinaciones.getValue(tipo).first()
                                error = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tipo de identificación",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    combinaciones.getValue(tipoConsulta).forEach { id ->
                        FilterChip(
                            selected = tipoIdentificacion == id,
                            onClick = {
                                tipoIdentificacion = id
                                error = null
                            },
                            label = { Text(etiquetaIdentificacionCorta(id)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = AzulClaro,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = valor,
                    onValueChange = {
                        valor = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(etiquetaIdentificacion(tipoIdentificacion)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        if (tipoIdentificacion == TipoIdentificacion.MATRICULA) {
                            IconButton(onClick = {
                                val permiso = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permiso) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    mostrarScanner = true
                                } else {
                                    permissionLauncher.launch(permiso)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "Capturar matrícula con la cámara",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = if (tipoIdentificacion == TipoIdentificacion.NIF) {
                            KeyboardType.Ascii
                        } else {
                            KeyboardType.Text
                        }
                    )
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        Log.d(TAG, "Botón Consultar pulsado: tipoConsulta=$tipoConsulta, tipoIdentificacion=$tipoIdentificacion, valor='$valor'")
                        try {
                            val request = ConsultaRequest.Builder()
                                .tipoConsulta(tipoConsulta)
                                .tipoIdentificacion(tipoIdentificacion)
                                .valor(valor)
                                .build()
                            Log.d(TAG, "Request construido correctamente: $request")
                            onConsultar(request)
                            Log.d(TAG, "onConsultar invocado con $request")
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Request inválido: ${e.message}")
                            error = e.message
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Consultar")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        }

        if (mostrarScanner) {
            CapturaMatriculaScreen(
                onMatricula = { matricula ->
                    valor = matricula
                    error = null
                    mostrarScanner = false
                },
                onCancelar = { mostrarScanner = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun OpcionTipoConsulta(
    texto: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AzulInstitucional else AzulClaro)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (selected) Color.White else AzulInstitucional,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun CaptchaWebView(
    request: ConsultaRequest,
    onResultadoHtmlObtenido: (String, TipoConsulta) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = (context as LifecycleOwner).lifecycleScope
    var isLoading by remember { mutableStateOf(true) }
    var handled by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            TextButton(onClick = onVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
                Text("Volver al formulario")
            }
        }

        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx: android.content.Context ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        displayZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean = false

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            Log.d(TAG, "onPageStarted: $url")
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            isLoading = false
                            Log.d(TAG, "onPageFinished: $url")
                            url?.let { u ->
                                if (u.contains("ServletController") && !u.contains("accion=inicio")) {
                                    coroutineScope.launch {
                                        delay(1500)
                                        view.evaluateJavascript(diagnosticoJs()) { diag ->
                                            Log.d(TAG, "DIAG resultado: $diag")
                                        }
                                        if (!handled) {
                                            handled = true
                                            view.evaluateJavascript(
                                                "javascript:document.documentElement.outerHTML;"
                                            ) { html ->
                                                val decoded = decodificarHtml(html)
                                                Log.d(TAG, "HTML resultado: len=${decoded?.length}, form=${decoded?.contains("consultaForm")}, tpsolic=${decoded?.contains("tpsolic")}, ficha=${decoded?.contains("fichaIdentidad")}, noResultados=${decoded?.contains("No se han encontrado resultados")}, noTitulos=${decoded?.contains("títulos habilitantes")}")
                                                if (!decoded.isNullOrEmpty() && !esFormularioEnRespuesta(decoded)) {
                                                    Log.d(TAG, "HTML resultado es resultado real, extrayendo")
                                                    onResultadoHtmlObtenido(decoded, request.tipoConsulta)
                                                } else {
                                                    Log.d(TAG, "HTML resultado sigue siendo formulario, ignorando")
                                                    handled = false
                                                }
                                            }
                                        }
                                    }
                                } else if (u.contains("ServletController") && u.contains("accion=inicio")) {
                                    view.evaluateJavascript(
                                        "javascript:" +
                                            "(function(){" +
                                            "function setRadio(name,value){" +
                                            "var els=document.getElementsByName(name);" +
                                            "for(var i=0;i<els.length;i++){" +
                                            "if(els[i].value===value){els[i].checked=true;}" +
                                            "}}" +
                                            "setRadio('tpsolic','"+tpsolic(request)+"');" +
                                            "setRadio('accion','"+accion(request)+"');" +
                                            "if(typeof habilita==='function'){habilita();}" +
                                            "var inp=document.querySelector('input[name=consulta],input[id*=consulta]');" +
                                            "if(inp){inp.value='"+escapeJs(request.valor)+"';}" +
                                            "})();"
                                    ) { }
                                    view.evaluateJavascript(diagnosticoJs()) { diag ->
                                        Log.d(TAG, "DIAG inicio: $diag")
                                    }
                                    coroutineScope.launch {
                                        Log.d(TAG, "Auto-submit: iniciando bucle")
                                        for (intento in 1..20) {
                                            delay(1000L)
                                            if (handled) return@launch
                                            val estado = enviarFormulario(view)
                                            Log.d(TAG, "Auto-submit intento $intento -> $estado")
                                            if (estado == "CAPTCHA") return@launch
                                        }
                                        Log.d(TAG, "Auto-submit: 20 intentos sin navegación, página sigue en formulario")
                                    }
                                }
                            }
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(RgtClient.baseUrl())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )

        if (isLoading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun tpsolic(request: ConsultaRequest): String = when (request.tipoConsulta) {
    TipoConsulta.AUTORIZACIONES -> "E"
    TipoConsulta.COMPETENCIA_CONSEJERO_CAP -> "P"
    TipoConsulta.VEHICULO -> "M"
}

private fun accion(request: ConsultaRequest): String = when (request.tipoIdentificacion) {
    TipoIdentificacion.NIF -> "consultar_nif"
    TipoIdentificacion.NOMBRE -> "consultar_empre"
    TipoIdentificacion.MATRICULA -> "consultar_nif"
    TipoIdentificacion.AUTORIZACION -> "consultar_auto"
    TipoIdentificacion.LICENCIA_INTERNACIONAL -> "consultar_inter"
}

private fun escapeJs(s: String): String =
    s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n")

private suspend fun enviarFormulario(view: WebView): String =
    suspendCancellableCoroutine { cont ->
        view.evaluateJavascript(
            "javascript:(function(){" +
                "var f=document.getElementById('consultaForm');" +
                "if(!f){return Promise.resolve('NOFORM');}" +
                "var v2=document.getElementById('g-recaptcha')" +
                "||document.querySelector('.g-recaptcha')" +
                "||document.querySelector('iframe[src*=\"recaptcha\"]');" +
                "if(v2){return Promise.resolve('CAPTCHA');}" +
                "var btn=document.querySelector('input[name=btnOk],button[name=btnOk]');" +
                "function clickOrSubmit(){" +
                "if(btn){btn.click();return 'SUBMIT';}" +
                "f.submit();return 'SUBMIT';" +
                "}" +
                "var g=window.grecaptcha;" +
                "if(g&&g.enterprise&&typeof g.enterprise.execute==='function'){" +
                "return g.enterprise.execute('6Lc9-JUsAAAAAH_d0YPX0-qaWU8bcR2-uatHtsg4',{action:'submit'})" +
                ".then(function(token){" +
                "var cap=document.querySelector('#g-recaptcha-response,[name=\"g-recaptcha-response\"]');" +
                "if(cap){cap.value=token;}" +
                "return clickOrSubmit();" +
                "}).catch(function(e){return clickOrSubmit();});" +
                "}" +
                "return Promise.resolve(clickOrSubmit());" +
                "})();"
        ) { res -> cont.resume(res?.trim('"') ?: "NOFORM") }
    }

private fun diagnosticoJs(): String =
    "javascript:JSON.stringify({" +
        "url:location.href," +
        "ready:document.readyState," +
        "form:!!document.getElementById('consultaForm')," +
        "tpsolic:(document.querySelector('[name=tpsolic]:checked')||{}).value||''," +
        "accion:(document.querySelector('[name=accion]:checked')||{}).value||''," +
        "consulta:(document.querySelector('input[name=consulta],input[id*=consulta]')||{}).value||''," +
        "btn:!!document.querySelector('input[name=btnOk],button[name=btnOk]')," +
        "grecaptcha:typeof window.grecaptcha," +
        "enterprise:!!(window.grecaptcha&&window.grecaptcha.enterprise)," +
        "v2:!!document.getElementById('g-recaptcha')||!!document.querySelector('.g-recaptcha')||!!document.querySelector('iframe[src*=\"recaptcha\"]')" +
        "})"

private fun decodificarHtml(resultadoJs: String?): String? {
    if (resultadoJs.isNullOrBlank()) return null
    return try {
        JSONTokener(resultadoJs).nextValue() as? String
    } catch (e: Exception) {
        resultadoJs.removeSurrounding("\"").replace("\\\"", "\"")
    }
}
