package com.example.reporti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.reporti.ui.ReportFormScreen
import com.example.reporti.ui.theme.ReportiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ReportFormScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}