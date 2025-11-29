package com.example.intentblocker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.intentblocker.data.AppPrefs
import com.example.intentblocker.ui.theme.IntentBlockerTheme
import kotlinx.coroutines.launch

class FrictionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appPrefs = AppPrefs(applicationContext)
        val targetPackage = intent.getStringExtra("target_package")

        setContent {
            IntentBlockerTheme(darkTheme = true) { // Force dark theme for focus
                FrictionScreen(
                        appPrefs = appPrefs,
                        onUnlock = { allowDuration ->
                            if (targetPackage != null) {
                                com.example.intentblocker.service.BlockManager
                                        .temporarilyAllowPackage(targetPackage, allowDuration)
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
fun FrictionScreen(appPrefs: AppPrefs, onUnlock: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    var requiredSentence by remember { mutableStateOf("") }
    var allowDuration by remember { mutableStateOf(60000L) }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch { appPrefs.frictionSentence.collect { requiredSentence = it } }
        launch { appPrefs.allowDuration.collect { allowDuration = it } }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(
                    text = "Pause.",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                    text = "Are you sure you want to open this app?",
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
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                            text = "Type the following to proceed:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                            text = requiredSentence,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Type here") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
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
            ) { Text(text = "Proceed", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
