package com.example.reporti.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Placeholder — replaced in the next commit with the real dispatch screen. */
@Composable
fun AdminAssignmentsScreen(bottomContentPadding: Dp, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AdminTopBar("Assignments", onBack = onBack)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Admin dispatch is coming in the next commit.\nMeanwhile, agents can self-claim on the web.",
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )
        }
    }
}
