@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.intentblocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.intentblocker.ui.AppInfo
import com.example.intentblocker.ui.MainViewModel
import com.example.intentblocker.ui.theme.IntentBlockerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IntentBlockerTheme { MainScreen(viewModel) } }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(checkAccessibilityServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = checkAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val apps by viewModel.uiState.collectAsState()
    val frictionSentence by viewModel.frictionSentence.collectAsState()
    val allowDuration by viewModel.allowDuration.collectAsState()

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = {
                            Text(
                                    stringResource(id = R.string.app_name),
                                    style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                            )
                            )
                        },
                        colors =
                                TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.primary
                                )
                )
            }
    ) { padding ->
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ServiceStatusCard(isServiceEnabled, context) }

            item {
                ConfigurationSection(
                        frictionSentence = frictionSentence,
                        allowDuration = allowDuration,
                        onUpdateSentence = { viewModel.updateFrictionSentence(it) },
                        onUpdateDuration = { viewModel.updateAllowDuration(it) }
                )
            }

            item {
                Text(
                        text = stringResource(id = R.string.blocked_apps),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(apps) { app ->
                AppItem(
                        app = app,
                        onToggle = { viewModel.toggleAppBlock(app.packageName, app.isBlocked) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ServiceStatusCard(isEnabled: Boolean, context: Context) {
    Card(
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isEnabled) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                    ),
            modifier = Modifier.fillMaxWidth()
    ) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector =
                            if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint =
                            if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = if (isEnabled) stringResource(id = R.string.service_active) else stringResource(id = R.string.service_inactive),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                                if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                )
                if (!isEnabled) {
                    Text(
                            text = stringResource(id = R.string.enable_accessibility),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            if (!isEnabled) {
                Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                )
                ) { Text(stringResource(id = R.string.enable_button)) }
            }
        }
    }
}

@Composable
fun ConfigurationSection(
        frictionSentence: String,
        allowDuration: Long,
        onUpdateSentence: (String) -> Unit,
        onUpdateDuration: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(id = R.string.configuration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings))
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Friction Sentence
                var tempSentence by remember(frictionSentence) { mutableStateOf(frictionSentence) }
                OutlinedTextField(
                        value = tempSentence,
                        onValueChange = { tempSentence = it },
                        label = { Text(stringResource(id = R.string.friction_sentence_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                )
                if (tempSentence != frictionSentence) {
                    TextButton(
                            onClick = { onUpdateSentence(tempSentence) },
                            modifier = Modifier.align(Alignment.End)
                    ) { Text(stringResource(id = R.string.save_sentence_button)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration
                var tempDuration by
                        remember(allowDuration) {
                            mutableStateOf((allowDuration / 1000).toString())
                        }
                OutlinedTextField(
                        value = tempDuration,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) tempDuration = it
                        },
                        label = { Text(stringResource(id = R.string.unlock_duration_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                )
                if (tempDuration.toLongOrNull()?.times(1000) != allowDuration) {
                    TextButton(
                            onClick = {
                                tempDuration.toLongOrNull()?.let { onUpdateDuration(it * 1000) }
                            },
                            modifier = Modifier.align(Alignment.End)
                    ) { Text(stringResource(id = R.string.save_duration_button)) }
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onToggle: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder Icon
            Box(
                    modifier =
                            Modifier.size(40.dp)
                                    .background(
                                            brush =
                                                    Brush.linearGradient(
                                                            colors =
                                                                    listOf(
                                                                            MaterialTheme
                                                                                    .colorScheme
                                                                                    .primary,
                                                                            MaterialTheme
                                                                                    .colorScheme
                                                                                    .secondary
                                                                    )
                                                    ),
                                            shape = CircleShape
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Text(
                        text = app.label.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                )
                Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                    checked = app.isBlocked,
                    onCheckedChange = { onToggle() },
                    colors =
                            SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
            )
        }
    }
}

fun checkAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}
