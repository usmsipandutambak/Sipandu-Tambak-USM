package id.ac.usm.sipandutambak.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.data.local.ThresholdConfig
import id.ac.usm.sipandutambak.ui.theme.NavyDark

@Composable
fun ThresholdSettingsDialog(
    currentConfig: ThresholdConfig,
    onDismiss: () -> Unit,
    onSave: (ThresholdConfig) -> Unit
) {
    var minDo by remember { mutableStateOf(currentConfig.minDo.toString()) }
    var minPh by remember { mutableStateOf(currentConfig.minPh.toString()) }
    var maxPh by remember { mutableStateOf(currentConfig.maxPh.toString()) }
    var minTemp by remember { mutableStateOf(currentConfig.minTemp.toString()) }
    var maxTemp by remember { mutableStateOf(currentConfig.maxTemp.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Standar Ambang Batas Air",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = NavyDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tentukan batas toleransi sensor agar sistem memberi peringatan anomali.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = minDo,
                    onValueChange = { minDo = it },
                    label = { Text("Batas Kritis DO Min (mg/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minPh,
                        onValueChange = { minPh = it },
                        label = { Text("pH Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxPh,
                        onValueChange = { maxPh = it },
                        label = { Text("pH Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minTemp,
                        onValueChange = { minTemp = it },
                        label = { Text("Suhu Min (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxTemp,
                        onValueChange = { maxTemp = it },
                        label = { Text("Suhu Max (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = ThresholdConfig(
                        minDo = minDo.toDoubleOrNull() ?: currentConfig.minDo,
                        minPh = minPh.toDoubleOrNull() ?: currentConfig.minPh,
                        maxPh = maxPh.toDoubleOrNull() ?: currentConfig.maxPh,
                        minTemp = minTemp.toDoubleOrNull() ?: currentConfig.minTemp,
                        maxTemp = maxTemp.toDoubleOrNull() ?: currentConfig.maxTemp
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
            ) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}