package com.vlcsheets.app.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vlcsheets.app.MainViewModel
import com.vlcsheets.app.R
import com.vlcsheets.app.data.SideAction
import com.vlcsheets.app.data.ThemeMode
import com.vlcsheets.app.ui.components.BrandMark
import com.vlcsheets.app.ui.components.ConfirmDialog
import com.vlcsheets.app.ui.theme.BrandGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var clearConfirm by remember { mutableStateOf(false) }
    // Which pedal key is being assigned: 1 = next page, -1 = previous page, 0 = none.
    var captureKey by remember { mutableIntStateOf(0) }
    var pickHold by remember { mutableIntStateOf(0) }
    var teachHandwriting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) vm.restoreBackup(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionTitle(stringResource(R.string.section_appearance))
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.setting_theme),
                        subtitle = stringResource(R.string.setting_theme_desc),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { vm.setThemeMode(mode) },
                                label = { Text(themeLabel(mode)) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(
                        icon = Icons.Rounded.Translate,
                        title = stringResource(R.string.setting_language),
                        subtitle = stringResource(R.string.setting_language_desc),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Each language is written in its own language, so it can be found whatever is on screen.
                        listOf("" to stringResource(R.string.theme_system), "ca" to "Català", "es" to "Castellano", "en" to "English")
                            .forEach { (tag, label) ->
                                FilterChip(
                                    selected = settings.language == tag,
                                    onClick = {
                                        if (settings.language != tag) {
                                            vm.updateSettings { it.copy(language = tag) }
                                            context.findActivity()?.recreate()
                                        }
                                    },
                                    label = { Text(label) },
                                    shape = CircleShape,
                                )
                            }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SwitchRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.setting_dynamic_color),
                    subtitle = stringResource(R.string.setting_dynamic_color_desc),
                    checked = settings.dynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = { vm.updateSettings { s -> s.copy(dynamicColor = it) } },
                )
            }
        }

        item {
            SectionTitle(stringResource(R.string.section_reading))
            SettingsCard {
                SwitchRow(
                    icon = Icons.Rounded.ScreenLockRotation,
                    title = stringResource(R.string.setting_keep_screen_on),
                    subtitle = stringResource(R.string.setting_keep_screen_on_desc),
                    checked = settings.keepScreenOn,
                    onCheckedChange = { vm.updateSettings { s -> s.copy(keepScreenOn = it) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SwitchRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = stringResource(R.string.setting_volume_keys),
                    subtitle = stringResource(R.string.setting_volume_keys_desc),
                    checked = settings.volumeKeys,
                    onCheckedChange = { vm.updateSettings { s -> s.copy(volumeKeys = it) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SwitchRow(
                    icon = Icons.Rounded.TouchApp,
                    title = stringResource(R.string.setting_tap_to_turn),
                    subtitle = stringResource(R.string.setting_tap_to_turn_desc),
                    checked = settings.tapToTurn,
                    onCheckedChange = { vm.updateSettings { s -> s.copy(tapToTurn = it) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.Keyboard,
                    title = stringResource(R.string.setting_next_key),
                    subtitle = keyLabel(settings.nextPageKey),
                ) { captureKey = 1 }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.Keyboard,
                    title = stringResource(R.string.setting_prev_key),
                    subtitle = keyLabel(settings.prevPageKey),
                ) { captureKey = -1 }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.TouchApp,
                    title = stringResource(R.string.setting_left_hold),
                    subtitle = sideActionLabel(settings.leftHold),
                ) { pickHold = -1 }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.TouchApp,
                    title = stringResource(R.string.setting_right_hold),
                    subtitle = sideActionLabel(settings.rightHold),
                ) { pickHold = 1 }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SwitchRow(
                    icon = Icons.Rounded.InvertColors,
                    title = stringResource(R.string.setting_invert_pages),
                    subtitle = stringResource(R.string.setting_invert_pages_desc),
                    checked = settings.invertPagesInDark,
                    onCheckedChange = { vm.updateSettings { s -> s.copy(invertPagesInDark = it) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.Draw,
                    title = stringResource(R.string.handwriting_teach),
                    subtitle = stringResource(R.string.handwriting_teach_desc),
                ) { teachHandwriting = true }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SwitchRow(
                    icon = Icons.Rounded.AutoFixHigh,
                    title = stringResource(R.string.setting_auto_handwriting),
                    subtitle = stringResource(R.string.setting_auto_handwriting_desc),
                    checked = settings.autoHandwriting,
                    onCheckedChange = { vm.updateSettings { s -> s.copy(autoHandwriting = it) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(
                        icon = Icons.Rounded.FormatSize,
                        title = stringResource(R.string.setting_handwriting_size),
                        subtitle = stringResource(R.string.setting_handwriting_size_desc),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(R.string.size_auto, R.string.size_small, R.string.size_medium, R.string.size_large)
                            .forEachIndexed { index, label ->
                                FilterChip(
                                    selected = settings.handwritingSize == index,
                                    onClick = { vm.updateSettings { it.copy(handwritingSize = index) } },
                                    label = { Text(stringResource(label)) },
                                    shape = CircleShape,
                                )
                            }
                    }
                }
            }
        }

        item {
            SectionTitle(stringResource(R.string.section_data))
            SettingsCard {
                ClickRow(
                    icon = Icons.Rounded.Backup,
                    title = stringResource(R.string.backup_export),
                    subtitle = stringResource(R.string.backup_export_desc),
                ) {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("vlc-sheets-backup-$stamp.zip")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.Restore,
                    title = stringResource(R.string.backup_restore),
                    subtitle = stringResource(R.string.backup_restore_desc),
                ) {
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ClickRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.clear_library),
                    subtitle = stringResource(R.string.clear_library_desc),
                    tint = MaterialTheme.colorScheme.error,
                ) { clearConfirm = true }
            }
        }

        item {
            SectionTitle(stringResource(R.string.section_about))
            SettingsCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(modifier = Modifier.size(56.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.version_label, versionName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text(
                                library.songs.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.songs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column {
                            Text(
                                library.songs.sumOf { it.pageCount }.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.pages),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column {
                            Text(
                                library.songs.sumOf { it.annotations.size }.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.annotations),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            // Signature with a link to the author's GitHub. Only the browser opens it: the app itself stays offline.
            val uriHandler = LocalUriHandler.current
            Text(
                text = stringResource(R.string.developed_by),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { runCatching { uriHandler.openUri(AUTHOR_URL) } }
                    .padding(vertical = 10.dp),
            )
        }

        item {
            Text(
                text = stringResource(R.string.made_with_love),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
    }

    if (captureKey != 0) KeyCaptureDialog(captureKey, vm) { captureKey = 0 }
    if (teachHandwriting) HandwritingTrainer(vm) { teachHandwriting = false }
    if (pickHold != 0) {
        val side = pickHold
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickHold = 0 },
            title = { Text(stringResource(if (side < 0) R.string.setting_left_hold else R.string.setting_right_hold)) },
            text = {
                Column {
                    SideAction.entries.forEach { action ->
                        val selected = action == (if (side < 0) settings.leftHold else settings.rightHold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.updateSettings { s -> if (side < 0) s.copy(leftHold = action) else s.copy(rightHold = action) }
                                    pickHold = 0
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Text(sideActionLabel(action))
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { pickHold = 0 }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    if (clearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.clear_library),
            text = stringResource(R.string.clear_library_confirm),
            confirmLabel = stringResource(R.string.action_delete_all),
            destructive = true,
            onConfirm = { vm.clearLibrary() },
            onDismiss = { clearConfirm = false },
        )
    }

    if (busy) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            icon = { CircularProgressIndicator() },
            title = { Text(stringResource(R.string.please_wait)) },
            text = { Text(stringResource(R.string.backup_progress)) },
        )
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun sideActionLabel(action: SideAction): String = stringResource(
    when (action) {
        SideAction.NONE -> R.string.side_none
        SideAction.FIRST_PAGE -> R.string.side_first_page
        SideAction.LAST_PAGE -> R.string.side_last_page
        SideAction.PREV_SONG -> R.string.side_prev_song
        SideAction.NEXT_SONG -> R.string.side_next_song
        SideAction.AUDIO -> R.string.side_audio
        SideAction.METRONOME -> R.string.side_metronome
    },
)

@Composable
private fun keyLabel(code: Int): String =
    if (code == 0) stringResource(R.string.key_none) else android.view.KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

/** Waits for the next key press and saves it as the next- or previous-page key. */
@Composable
private fun KeyCaptureDialog(direction: Int, vm: MainViewModel, onDone: () -> Unit) {
    val activity = LocalContext.current.findActivity() as? com.vlcsheets.app.MainActivity
    DisposableEffect(activity) {
        activity?.keyCaptureHandler = { code ->
            vm.updateSettings { s -> if (direction > 0) s.copy(nextPageKey = code) else s.copy(prevPageKey = code) }
            onDone()
        }
        onDispose { activity?.keyCaptureHandler = null }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.key_capture_title)) },
        text = { Text(stringResource(R.string.key_capture_text)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                vm.updateSettings { s -> if (direction > 0) s.copy(nextPageKey = 0) else s.copy(prevPageKey = 0) }
                onDone()
            }) { Text(stringResource(R.string.key_clear)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDone) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ClickRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon, tint = tint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

private const val AUTHOR_URL = "https://github.com/Vlarac03"
