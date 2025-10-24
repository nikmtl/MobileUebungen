package com.example.composeexamples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Example showing correct use of `by remember`, `rememberSaveable`, and `derivedStateOf`.
@Composable
fun RememberResultExample() {
    // input survives rotation -> use rememberSaveable
    var input by rememberSaveable { mutableStateOf("") }

    // manualResult is a mutable value stored across recompositions
    var manualResult by remember { mutableStateOf("") }

    // autoResult is computed from `input`; using derivedStateOf avoids recomputing unless input changes
    val autoResult by remember {
        derivedStateOf {
            // example "expensive" computation (simple here)
            input.trim().reversed().uppercase()
        }
    }

    Column {
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Type something") }
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = { manualResult = "Manual: ${input.length} chars" }) {
            Text("Compute manual result")
        }

        Spacer(Modifier.height(8.dp))

        // display both manual and auto results
        Text("Manual result (set by button): $manualResult")
        Text("Auto result (derived from input): $autoResult")
    }
}

