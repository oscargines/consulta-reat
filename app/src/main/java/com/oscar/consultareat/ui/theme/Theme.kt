package com.oscar.consultareat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AzulInstitucional,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulInstitucional,
    secondary = AzulAccent,
    tertiary = VerdeEstado,
    background = FondoGris,
    surface = SuperficieBlanco,
    onSurface = TextoPrincipal,
    onSurfaceVariant = TextoSecundario,
    surfaceVariant = FondoGris,
    outline = BordeSuave,
    error = RojoEstado
)

@Composable
fun ConsultaREATTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}