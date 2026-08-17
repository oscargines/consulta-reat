package com.oscar.consultareat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oscar.consultareat.ui.theme.ConsultaREATTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsultaREATTheme {
                ConsultaRgtApp()
            }
        }
    }
}

@Composable
fun ConsultaRgtApp(modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        com.oscar.consultareat.ui.AppNavigation()
    }
}
