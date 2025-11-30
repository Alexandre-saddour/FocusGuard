package com.flowdevproduction.focusguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowdevproduction.focusguard.R
import androidx.activity.viewModels
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.flowdevproduction.focusguard.ui.theme.FocusGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow

@AndroidEntryPoint
class FrictionActivity : ComponentActivity() {

    private val viewModel: FrictionViewModel by viewModels()

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)

        setContent {
            FocusGuardTheme(darkTheme = true) { // Force dark theme for focus
                FrictionScreen(
                        frictionSentenceFlow = viewModel.frictionSentence,
                        allowDurationFlow = viewModel.allowDuration,
                        onUnlock = { allowDuration ->
                            if (targetPackage != null) {
                                viewModel.unlockApp(targetPackage, allowDuration)
                                val launchIntent =
                                        packageManager.getLaunchIntentForPackage(targetPackage)
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                }
                            }
                            finish()
                        }
                )
            }
        }
    }
}

@Composable
fun FrictionScreen(
        frictionSentenceFlow: StateFlow<String>,
        allowDurationFlow: StateFlow<Long>,
        onUnlock: (Long) -> Unit
) {
    val requiredSentence by frictionSentenceFlow.collectAsState()
    val allowDuration by allowDurationFlow.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.friction_pause),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.friction_confirmation),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            )
                    ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                id = R.string.friction_prompt
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = requiredSentence,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Left
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = {
                    Text(stringResource(id = R.string.friction_input_label))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.outline,
                        focusedLabelColor =
                            MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            if (inputText == requiredSentence) {
                                onUnlock(allowDuration)
                            }
                        }
                    )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onUnlock(allowDuration) },
                enabled = inputText == requiredSentence,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
            ) {
                Text(
                    text =
                        stringResource(
                            id = R.string.friction_proceed_button
                        ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
