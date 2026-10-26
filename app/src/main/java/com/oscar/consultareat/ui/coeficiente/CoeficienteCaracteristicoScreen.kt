package com.oscar.consultareat.ui.coeficiente

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oscar.consultareat.R
import com.oscar.consultareat.ui.theme.AzulClaro
import com.oscar.consultareat.ui.theme.AzulInstitucional
import com.oscar.consultareat.ui.theme.FondoGris
import com.oscar.consultareat.ui.theme.RojoEstado
import com.oscar.consultareat.ui.theme.TextoSecundario
import com.oscar.consultareat.ui.theme.VerdeEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoeficienteCaracteristicoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CoeficienteCaracteristicoViewModel = viewModel(factory = CoeficienteCaracteristicoViewModelFactory())

    Scaffold(
        modifier = modifier,
        containerColor = FondoGris,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.coeficiente_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
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
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .padding(bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CabeceraCoeficiente()

                val error = viewModel.error
                if (error != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
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
                            stringResource(R.string.coeficiente_datos_entrada),
                            style = MaterialTheme.typography.titleMedium,
                            color = AzulInstitucional,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            stringResource(R.string.coeficiente_grupo_activacion),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextoSecundario,
                            fontWeight = FontWeight.Bold
                        )

                        CampoCoeficiente(
                            value = viewModel.coeficienteWa,
                            onValueChange = { viewModel.onCoeficienteWaChange(it) },
                            label = stringResource(R.string.coeficiente_label_wa),
                            icon = Icons.Filled.Calculate,
                            imeAction = ImeAction.Next
                        )

                        CampoCoeficiente(
                            value = viewModel.circunferenciaLa,
                            onValueChange = { viewModel.onCircunferenciaLaChange(it) },
                            label = stringResource(R.string.coeficiente_label_la),
                            icon = Icons.Filled.Speed,
                            imeAction = ImeAction.Next
                        )

                        Text(
                            stringResource(R.string.coeficiente_grupo_periodico),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextoSecundario,
                            fontWeight = FontWeight.Bold
                        )

                        CampoCoeficiente(
                            value = viewModel.coeficienteWp,
                            onValueChange = { viewModel.onCoeficienteWpChange(it) },
                            label = stringResource(R.string.coeficiente_label_wp),
                            icon = Icons.Filled.Calculate,
                            imeAction = ImeAction.Next
                        )

                        CampoCoeficiente(
                            value = viewModel.circunferenciaLp,
                            onValueChange = { viewModel.onCircunferenciaLpChange(it) },
                            label = stringResource(R.string.coeficiente_label_lp),
                            icon = Icons.Filled.Speed,
                            imeAction = ImeAction.Done
                        )

                        Button(
                            onClick = { viewModel.calculate() },
                            enabled = viewModel.isFormValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.coeficiente_btn_calcular))
                        }
                    }
                }

                viewModel.calculationResult?.let { result ->
                    ResultCard(result = result)
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        if (viewModel.showResultDialog) {
            viewModel.calculationResult?.let { result ->
                ResultDialog(
                    result = result,
                    onDismiss = { viewModel.dismissResultDialog() }
                )
            }
        }
    }
}

@Composable
private fun CampoCoeficiente(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = AzulInstitucional) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onNext = { }),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun CabeceraCoeficiente() {
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
            Icon(Icons.Filled.Calculate, null, tint = AzulInstitucional, modifier = Modifier.size(36.dp))
            Column {
                Text(stringResource(R.string.coeficiente_titulo_cabecera), fontWeight = FontWeight.Bold, color = AzulInstitucional)
                Text(
                    stringResource(R.string.coeficiente_subtitulo_cabecera),
                    color = AzulInstitucional,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ResultCard(result: CalculationResult) {
    val hayFraude = result.hayFraude
    val colorEstado = if (hayFraude) RojoEstado else VerdeEstado
    val iconEstado = if (hayFraude) Icons.Filled.Warning else Icons.Filled.CheckCircle
    val textoEstado = if (hayFraude) stringResource(R.string.coeficiente_fraude_detectado) else stringResource(R.string.coeficiente_sin_fraude)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hayFraude) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(iconEstado, contentDescription = "", tint = colorEstado, modifier = Modifier.size(28.dp))
                Text(
                    textoEstado,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorEstado
                )
            }
            Spacer(Modifier.height(16.dp))

            if (result.circunferenciasIguales) {
                Text(
                    stringResource(R.string.coeficiente_sin_fraude_l_igual),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )
            } else {
                ResultadoFila(stringResource(R.string.coeficiente_gamma), "%.2f".format(result.gamma))
                ResultadoFila(stringResource(R.string.coeficiente_beta), "%.2f".format(result.beta))
                ResultadoFila(stringResource(R.string.coeficiente_alfa), "%.2f".format(result.alfa))
                ResultadoFila(
                    stringResource(R.string.coeficiente_dseta),
                    "%.2f%%".format(result.dseta),
                    isHighlighted = true,
                    color = colorEstado
                )
            }
        }
    }
}

@Composable
private fun ResultDialog(
    result: CalculationResult,
    onDismiss: () -> Unit
) {
    val hayFraude = result.hayFraude
    val colorEstado = if (hayFraude) RojoEstado else VerdeEstado
    val iconEstado = if (hayFraude) Icons.Filled.Warning else Icons.Filled.CheckCircle
    val textoEstado = if (hayFraude) stringResource(R.string.coeficiente_fraude_detectado) else stringResource(R.string.coeficiente_sin_fraude)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(iconEstado, contentDescription = "", tint = colorEstado, modifier = Modifier.size(28.dp))
                Text(
                    textoEstado,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorEstado
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hayFraude) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.coeficiente_resultados_calculo),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorEstado
                        )
                        if (result.circunferenciasIguales) {
                            Text(
                                stringResource(R.string.coeficiente_sin_fraude_l_igual),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundario
                            )
                        } else {
                            ResultadoFila(stringResource(R.string.coeficiente_gamma), "%.2f".format(result.gamma))
                            ResultadoFila(stringResource(R.string.coeficiente_beta), "%.2f".format(result.beta))
                            ResultadoFila(stringResource(R.string.coeficiente_alfa), "%.2f".format(result.alfa))
                            ResultadoFila(
                                stringResource(R.string.coeficiente_dseta),
                                "%.2f%%".format(result.dseta),
                                isHighlighted = true,
                                color = colorEstado
                            )
                        }
                    }
                }

                if (hayFraude) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = "", tint = RojoEstado, modifier = Modifier.size(24.dp))
                                Text(
                                    stringResource(R.string.coeficiente_accion_requerida_titulo),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RojoEstado
                                )
                            }
                            Text(
                                stringResource(R.string.coeficiente_accion_requerida_texto),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RojoEstado
                            )
                        }
                    }
                } else if (!result.circunferenciasIguales) {
                    Text(
                        stringResource(R.string.coeficiente_sin_fraude_mensaje),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VerdeEstado,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accept_action))
            }
        }
    )
}

@Composable
private fun ResultadoFila(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Text(
            value,
            style = if (isHighlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
