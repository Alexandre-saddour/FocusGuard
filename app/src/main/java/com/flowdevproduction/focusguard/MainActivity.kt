@file:OptIn(ExperimentalMaterial3Api::class)

package com.flowdevproduction.focusguard

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flowdevproduction.focusguard.domain.model.AppInfo
import com.flowdevproduction.focusguard.ui.MainViewModel
import com.flowdevproduction.focusguard.ui.components.ConsentDialog
import com.flowdevproduction.focusguard.ui.theme.FocusGuardTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FocusGuardTheme { MainScreen(viewModel) } }
    }

    companion object {
        const val PRIVACY_POLICY_URL = "https://firebase.google.com/support/privacy"
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var hasUsageAccess by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(checkOverlayPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = checkUsageStatsPermission(context)
                hasOverlayPermission = checkOverlayPermission(context)

                // Start service if both permissions are granted
                if (hasUsageAccess && hasOverlayPermission) {
                    val serviceIntent =
                        Intent(
                            context,
                            com.flowdevproduction.focusguard.service
                                .UsageMonitorService::class
                                .java
                        )
                    context.startForegroundService(serviceIntent)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val apps by viewModel.uiState.collectAsState()
    val frictionSentence by viewModel.frictionSentence.collectAsState()
    val allowDuration by viewModel.allowDuration.collectAsState()
    val isAnalyticsEnabled by viewModel.isAnalyticsEnabled.collectAsState()

    // Firebase Initialization Effect
    LaunchedEffect(isAnalyticsEnabled) {
        isAnalyticsEnabled?.let { isAnalyticsEnabled ->
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = isAnalyticsEnabled
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(isAnalyticsEnabled)
        }
    }

    if (isAnalyticsEnabled == null) {
        ConsentDialog(
            onConfirm = { viewModel.setAnalyticsEnabled(true) },
            onDismiss = { viewModel.setAnalyticsEnabled(false) },
            onPrivacyPolicyClick = {
                val intent = Intent(Intent.ACTION_VIEW, MainActivity.PRIVACY_POLICY_URL.toUri())
                context.startActivity(intent)
            }
        )
    }

    Scaffold(
        topBar = {
            val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text =
                            stringResource(
                                when {
                                    isServiceEnabled -> R.string.service_active_title
                                    else -> R.string.service_paused_title
                                }
                            ),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                actions = {
                    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()
                    Switch(
                        checked = isServiceEnabled,
                        onCheckedChange = { viewModel.toggleServiceEnabled(it) },
                        modifier = Modifier.padding(end = 16.dp),
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor =
                                    MaterialTheme.colorScheme.primary,
                                checkedTrackColor =
                                    MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor =
                                    MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ServiceStatusCard(hasUsageAccess, hasOverlayPermission, context) }

            item {
                ConfigurationSection(
                    frictionSentence = frictionSentence,
                    allowDuration = allowDuration,
                    isAnalyticsEnabled = isAnalyticsEnabled == true,
                    onUpdateSentence = { viewModel.updateFrictionSentence(it) },
                    onUpdateDuration = { viewModel.updateAllowDuration(it) },
                    onToggleAnalytics = { viewModel.setAnalyticsEnabled(it) }
                )
            }

            item {
                val searchQuery by viewModel.searchQuery.collectAsState()
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text(stringResource(id = R.string.search_apps_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.clear_search)
                                )
                            }
                        }
                    }
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
                    onToggle = {
                        viewModel.toggleAppBlock(
                            app.packageName,
                            app.isBlocked
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ServiceStatusCard(hasUsageAccess: Boolean, hasOverlayPermission: Boolean, context: Context) {
    val isFullyEnabled = hasUsageAccess && hasOverlayPermission

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = when {
                    isFullyEnabled -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isFullyEnabled -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = when {
                        isFullyEnabled -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isFullyEnabled ->
                                stringResource(id = R.string.service_active)

                            else -> stringResource(id = R.string.permissions_required)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isFullyEnabled ->
                                MaterialTheme.colorScheme.onPrimaryContainer

                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    if (!isFullyEnabled) {
                        Text(
                            text = stringResource(id = R.string.grant_permissions_below),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (!hasUsageAccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                ) { Text(stringResource(id = R.string.grant_usage_access)) }
            }

            if (!hasOverlayPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent =
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                ) { Text(stringResource(id = R.string.grant_overlay_permission)) }
            }
        }
    }
}

@Composable
fun ConfigurationSection(
    frictionSentence: String,
    allowDuration: Long,
    isAnalyticsEnabled: Boolean,
    onUpdateSentence: (String) -> Unit,
    onUpdateDuration: (Long) -> Unit,
    onToggleAnalytics: (Boolean) -> Unit
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
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
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

                Spacer(modifier = Modifier.height(16.dp))

                // Analytics Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.share_usage_data_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(id = R.string.share_usage_data_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isAnalyticsEnabled, onCheckedChange = onToggleAnalytics)
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
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder Icon
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
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

fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode =
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}
