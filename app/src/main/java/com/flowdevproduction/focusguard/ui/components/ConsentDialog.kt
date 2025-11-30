package com.flowdevproduction.focusguard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flowdevproduction.focusguard.R

@Composable
fun ConsentDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, onPrivacyPolicyClick: () -> Unit) {
    AlertDialog(
            onDismissRequest = {}, // Prevent dismissal by clicking outside
            title = { Text(text = stringResource(id = R.string.data_privacy_title)) },
            text = {
                Column {
                    Text(text = stringResource(id = R.string.data_privacy_message_intro))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(id = R.string.data_privacy_message_detail))
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onPrivacyPolicyClick) {
                        Text(stringResource(id = R.string.read_privacy_policy))
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirm) { Text(stringResource(id = R.string.accept)) }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) { Text(stringResource(id = R.string.decline)) }
            }
    )
}
