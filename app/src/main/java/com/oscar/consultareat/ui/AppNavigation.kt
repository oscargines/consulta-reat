package com.oscar.consultareat.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oscar.consultareat.RgtRepository
import com.oscar.consultareat.data.api.ReatApiClient
import com.oscar.consultareat.data.api.ReatApiConfig
import com.oscar.consultareat.data.cache.HistorialCache
import com.oscar.consultareat.data.client.RgtClient
import com.oscar.consultareat.ui.acercade.AcercaDeScreen
import com.oscar.consultareat.ui.consulta.ConsultaScreen
import com.oscar.consultareat.ui.excepciones.ExcepcionesScreen
import com.oscar.consultareat.ui.historial.HistorialScreen
import com.oscar.consultareat.ui.inicio.PantallaPrincipal
import com.oscar.consultareat.ui.resultado.ResultadoScreen
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.TextoSecundario
import com.oscar.consultareat.ui.viewmodel.ConsultaViewModel
import com.oscar.consultareat.ui.viewmodel.ConsultaViewModelFactory
import com.oscar.consultareat.ui.viewmodel.UiState

private const val TAG = "ConsultaREAT.Navigation"

private const val RUTA_INICIO = "inicio"
private const val RUTA_CONSULTAS = "consultas"
private const val RUTA_HISTORIAL = "historial"
private const val RUTA_EXCEPCIONES = "excepciones"
private const val RUTA_ACERCADE = "acercade"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val repository = remember {
        val apiConfig = ReatApiConfig.desdeRecursos(context)
        RgtRepository(
            client = RgtClient(apiClient = ReatApiClient(apiConfig)),
            cache = HistorialCache(context)
        )
    }
    val viewModelFactory = remember { ConsultaViewModelFactory(repository) }
    val viewModel: ConsultaViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val historial by viewModel.historial.collectAsState(initial = emptyList())
    val avisoDuplicado by viewModel.avisoDuplicado.collectAsState()
    val avisoSinResultados by viewModel.avisoSinResultados.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val pantallaActual = backStackEntry?.destination?.route

    var mostrarComentario by remember { mutableStateOf(false) }
    var textoComentario by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        Log.d(TAG, "uiState cambiado: ${uiState::class.simpleName}, pantallaActual=$pantallaActual")
        if (uiState is UiState.Success && pantallaActual == RUTA_INICIO) {
            Log.d(TAG, "Navegando a consultas")
            navController.navigate(RUTA_CONSULTAS) { launchSingleTop = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (pantallaActual != RUTA_INICIO) {
                BarraInferior(
                    pantallaActual = pantallaActual,
                    onSelect = { ruta ->
                        if (ruta == RUTA_INICIO) {
                            // Volver a la pantalla raíz debe desapilar la pantalla actual.
                            if (!navController.popBackStack(RUTA_INICIO, inclusive = false)) {
                                navController.navigate(RUTA_INICIO) {
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate(ruta) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNuevaConsulta = {
                        viewModel.nuevaConsulta()
                        navController.navigate(RUTA_CONSULTAS) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RUTA_INICIO,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RUTA_INICIO) {
                PantallaPrincipal(
                    onConsultas = { navController.navigate(RUTA_CONSULTAS) { launchSingleTop = true } },
                    onHistorial = { navController.navigate(RUTA_HISTORIAL) { launchSingleTop = true } },
                    onExcepciones = { navController.navigate(RUTA_EXCEPCIONES) { launchSingleTop = true } },
                    onAcercaDe = { navController.navigate(RUTA_ACERCADE) { launchSingleTop = true } }
                )
            }
            composable(RUTA_CONSULTAS) {
                if (uiState is UiState.Success || uiState is UiState.Error || uiState is UiState.Loading) {
                    ResultadoScreen(
                        uiState = uiState,
                        onBack = {
                            viewModel.volverAlFormulario()
                            navController.navigate(RUTA_INICIO) { launchSingleTop = true }
                        }
                    )
                } else {
                    ConsultaScreen(
                        uiState = uiState,
                        onConsultar = { viewModel.ejecutarConsulta(it) },
                        onResultadoHtmlObtenido = { html, tipo -> viewModel.parsearHtmlResultado(html, tipo) },
                        onVolverFormulario = { viewModel.volverAlFormulario() }
                    )
                }
            }
            composable(RUTA_HISTORIAL) {
                HistorialScreen(
                    historial = historial,
                    onBack = { navController.navigate(RUTA_INICIO) { launchSingleTop = true } },
                    onItemClick = { item ->
                        viewModel.mostrarHistorialItem(item)
                        navController.navigate(RUTA_CONSULTAS) { launchSingleTop = true }
                    },
                    onItemDelete = { viewModel.eliminarHistorial(it) }
                )
            }
            composable(RUTA_EXCEPCIONES) {
                ExcepcionesScreen(
                    onBack = { navController.navigate(RUTA_INICIO) { launchSingleTop = true } }
                )
            }
            composable(RUTA_ACERCADE) {
                AcercaDeScreen(
                    onBack = { navController.navigate(RUTA_INICIO) { launchSingleTop = true } }
                )
            }
        }
    }

    avisoDuplicado?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { viewModel.avisoDuplicadoConsumido() },
            title = { Text("Aviso") },
            text = { Text(mensaje) },
            confirmButton = {
                TextButton(onClick = { viewModel.avisoDuplicadoConsumido() }) {
                    Text("Aceptar")
                }
            }
        )
    }
avisoSinResultados?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { viewModel.avisoSinResultadosConsumido() },
            title = { Text("Sin resultados") },
            text = { Text(mensaje) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.avisoSinResultadosConsumido()
                    mostrarComentario = true
                }) {
                    Text("Añadir comentario")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.avisoSinResultadosConsumido() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (mostrarComentario) {
        AlertDialog(
            onDismissRequest = {
                mostrarComentario = false
                textoComentario = ""
            },
            title = { Text("Comentario de la búsqueda") },
            text = {
                Column {
                    Text(
                        "Añade un comentario sobre esta búsqueda sin resultados (motivo, marca y modelo, empresa, actividad que realiza, etc.)."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textoComentario,
                        onValueChange = { textoComentario = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        placeholder = { Text("Escribe aquí tu comentario…") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.guardarComentario(textoComentario)
                    mostrarComentario = false
                    textoComentario = ""
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarComentario = false
                    textoComentario = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BarraInferior(
    pantallaActual: String?,
    onSelect: (String) -> Unit,
    onNuevaConsulta: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElementoInferior(
                etiqueta = "Inicio",
                icono = if (pantallaActual == RUTA_INICIO) Icons.Filled.Home else Icons.Outlined.Home,
                seleccionado = pantallaActual == RUTA_INICIO,
                onClick = { onSelect(RUTA_INICIO) },
                modifier = Modifier.weight(1f)
            )
            ElementoInferior(
                etiqueta = "Consultas",
                icono = if (pantallaActual == RUTA_CONSULTAS) Icons.Filled.ListAlt else Icons.Outlined.ListAlt,
                seleccionado = pantallaActual == RUTA_CONSULTAS,
                onClick = { onSelect(RUTA_CONSULTAS) },
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.IconButton(
                onClick = onNuevaConsulta,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AzulInstitucional),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Nueva consulta",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            ElementoInferior(
                etiqueta = "Historial",
                icono = if (pantallaActual == RUTA_HISTORIAL) Icons.Filled.History else Icons.Outlined.History,
                seleccionado = pantallaActual == RUTA_HISTORIAL,
                onClick = { onSelect(RUTA_HISTORIAL) },
                modifier = Modifier.weight(1f)
            )
            ElementoInferior(
                etiqueta = "Acerca de",
                icono = if (pantallaActual == RUTA_ACERCADE) Icons.Filled.Info else Icons.Outlined.Info,
                seleccionado = pantallaActual == RUTA_ACERCADE,
                onClick = { onSelect(RUTA_ACERCADE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ElementoInferior(
    etiqueta: String,
    icono: ImageVector,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = etiqueta,
            tint = if (seleccionado) AzulInstitucional else TextoSecundario
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = if (seleccionado) AzulInstitucional else TextoSecundario
        )
    }
}
