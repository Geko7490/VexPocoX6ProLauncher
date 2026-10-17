package com.vex.launcher.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vex.launcher.BuildConfig
import com.vex.launcher.R
import com.vex.launcher.core.model.AccentTheme
import com.vex.launcher.core.model.AnimationLevel
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.WallpaperChoice
import com.vex.launcher.core.util.AppSearch
import com.vex.launcher.data.IconRepository
import com.vex.launcher.di.AppContainer
import com.vex.launcher.ui.components.AppIcon
import com.vex.launcher.ui.components.ColorChoice
import com.vex.launcher.ui.components.SectionTitle
import com.vex.launcher.ui.components.SegmentedOption
import com.vex.launcher.ui.components.SettingsInfoRow
import com.vex.launcher.ui.components.SettingsSliderRow
import com.vex.launcher.ui.components.SettingsSwitchRow
import com.vex.launcher.ui.components.VexSearchField
import com.vex.launcher.ui.rememberSystemAnimationScale
import com.vex.launcher.ui.theme.gradientColors
import com.vex.launcher.ui.theme.primary

private const val DOCK_SLOT_COUNT = 5

/**
 * Ayarlar ekranı.
 *
 * Her kontrol gerçek bir işleve bağlıdır ve DataStore'a yazılır.
 * Henüz uygulanmamış özellikler "Yol haritası" bölümünde açıkça işaretlenir.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    isDefaultHome: Boolean,
    onRequestDefaultHome: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val systemAnimationScale = rememberSystemAnimationScale()

    var dockSlotToEdit by remember { mutableStateOf<Int?>(null) }
    var showHiddenManager by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.applyPhotoWallpaper(uri)
        }
    }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        val text = when (current) {
            SettingsMessage.WallpaperApplied -> context.getString(R.string.wallpaper_photo_set)
            SettingsMessage.WallpaperFailed -> context.getString(R.string.wallpaper_photo_failed)
            SettingsMessage.SettingsReset -> context.getString(R.string.about_reset_done)
        }
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                // ---------------- Görünüm ----------------
                SectionTitle(text = stringResource(R.string.section_appearance))
                SettingsSwitchRow(
                    title = stringResource(R.string.label_dark_theme),
                    description = stringResource(R.string.label_dark_theme_desc),
                    checked = settings.darkTheme,
                    onCheckedChange = viewModel::setDarkTheme,
                )
                AccentPicker(
                    selected = settings.accent,
                    onSelect = viewModel::setAccent,
                )

                // ---------------- Ana ekran düzeni ----------------
                SectionTitle(text = stringResource(R.string.section_home))
                SettingsSliderRow(
                    title = stringResource(R.string.label_icon_size),
                    valueLabel = stringResource(
                        R.string.label_icon_size_value,
                        (settings.iconScale * 100).toInt(),
                    ),
                    value = settings.iconScale,
                    valueRange = 0.6f..1.5f,
                    steps = 18,
                    onValueChange = viewModel::setIconScale,
                )
                SettingsSliderRow(
                    title = stringResource(R.string.label_columns),
                    valueLabel = stringResource(R.string.label_columns_value, settings.gridColumns),
                    value = settings.gridColumns.toFloat(),
                    valueRange = 3f..8f,
                    steps = 4,
                    onValueChange = { viewModel.setGridColumns(it.toInt()) },
                )
                SettingsSliderRow(
                    title = stringResource(R.string.label_rows),
                    valueLabel = stringResource(R.string.label_rows_value, settings.gridRows),
                    value = settings.gridRows.toFloat(),
                    valueRange = 3f..8f,
                    steps = 4,
                    onValueChange = { viewModel.setGridRows(it.toInt()) },
                )
                SettingsSliderRow(
                    title = stringResource(R.string.label_pages),
                    valueLabel = stringResource(R.string.label_pages_value, settings.homePages),
                    value = settings.homePages.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    onValueChange = { viewModel.setHomePages(it.toInt()) },
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.label_show_labels),
                    description = null,
                    checked = settings.showLabels,
                    onCheckedChange = viewModel::setShowLabels,
                )
                SettingsSliderRow(
                    title = stringResource(R.string.label_label_size),
                    valueLabel = stringResource(
                        R.string.label_label_size_value,
                        (settings.labelScale * 100).toInt(),
                    ),
                    value = settings.labelScale,
                    valueRange = 0.8f..1.3f,
                    steps = 4,
                    onValueChange = viewModel::setLabelScale,
                )

                // ---------------- Animasyon ----------------
                SectionTitle(text = stringResource(R.string.section_animation))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SegmentedOption(
                        label = stringResource(R.string.anim_off),
                        selected = settings.animationLevel == AnimationLevel.OFF,
                        onClick = { viewModel.setAnimationLevel(AnimationLevel.OFF) },
                    )
                    SegmentedOption(
                        label = stringResource(R.string.anim_reduced),
                        selected = settings.animationLevel == AnimationLevel.REDUCED,
                        onClick = { viewModel.setAnimationLevel(AnimationLevel.REDUCED) },
                    )
                    SegmentedOption(
                        label = stringResource(R.string.anim_full),
                        selected = settings.animationLevel == AnimationLevel.FULL,
                        onClick = { viewModel.setAnimationLevel(AnimationLevel.FULL) },
                    )
                }
                SettingsInfoRow(
                    label = stringResource(R.string.label_animation),
                    value = stringResource(
                        R.string.anim_system_note,
                        formatScale(systemAnimationScale),
                    ),
                )

                // ---------------- Dock ----------------
                SectionTitle(text = stringResource(R.string.section_dock))
                DockSlotList(
                    dockPackages = settings.dockPackages,
                    apps = apps,
                    iconRepository = container.iconRepository,
                    onEdit = { index -> dockSlotToEdit = index },
                )

                // ---------------- Duvar kağıdı ----------------
                SectionTitle(text = stringResource(R.string.section_wallpaper))
                WallpaperPicker(
                    selected = settings.wallpaper,
                    onSelect = viewModel::setWallpaper,
                    onPickPhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onOpenSystem = viewModel::openSystemWallpaper,
                )

                // ---------------- Gizli uygulamalar ----------------
                SectionTitle(text = stringResource(R.string.section_hidden))
                SettingsInfoRow(
                    label = stringResource(R.string.hidden_apps_desc),
                    value = settings.hiddenPackages.size.toString(),
                )
                TextButton(
                    onClick = { showHiddenManager = true },
                    modifier = Modifier.padding(horizontal = 14.dp),
                ) {
                    Text(stringResource(R.string.hidden_manager))
                }

                // ---------------- Varsayılan ana ekran ----------------
                SectionTitle(text = stringResource(R.string.section_role))
                SettingsInfoRow(
                    label = stringResource(R.string.section_role),
                    value = if (isDefaultHome) {
                        stringResource(R.string.role_active)
                    } else {
                        stringResource(R.string.role_inactive)
                    },
                )
                TextButton(
                    onClick = onRequestDefaultHome,
                    modifier = Modifier.padding(horizontal = 14.dp),
                ) {
                    Text(
                        if (isDefaultHome) {
                            stringResource(R.string.role_manage)
                        } else {
                            stringResource(R.string.home_default_action)
                        },
                    )
                }

                // ---------------- Hakkında ----------------
                SectionTitle(text = stringResource(R.string.section_about))
                SettingsInfoRow(
                    label = stringResource(R.string.about_version),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
                SettingsInfoRow(
                    label = stringResource(R.string.about_device),
                    value = "${Build.MANUFACTURER} ${Build.MODEL}",
                )
                SettingsInfoRow(
                    label = stringResource(R.string.about_android),
                    value = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                )
                SettingsInfoRow(
                    label = stringResource(R.string.about_stack),
                    value = stringResource(R.string.about_stack_value),
                )
                TextButton(
                    onClick = { viewModel.resetSettings() },
                    modifier = Modifier.padding(horizontal = 14.dp),
                ) {
                    Text(stringResource(R.string.about_reset))
                }

                // ---------------- Yol haritası ----------------
                SectionTitle(text = stringResource(R.string.section_roadmap))
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        text = stringResource(R.string.roadmap_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoadmapRow(stringResource(R.string.roadmap_drag))
                    RoadmapRow(stringResource(R.string.roadmap_folders))
                    RoadmapRow(stringResource(R.string.roadmap_widgets))
                    RoadmapRow(stringResource(R.string.roadmap_backup))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    dockSlotToEdit?.let { slotIndex ->
        AppPickerDialog(
            apps = apps,
            iconRepository = container.iconRepository,
            onDismiss = { dockSlotToEdit = null },
            onSelect = { app ->
                viewModel.setDockSlot(slotIndex, app.packageName)
                dockSlotToEdit = null
            },
        )
    }

    if (showHiddenManager) {
        HiddenAppsDialog(
            apps = apps,
            hiddenPackages = settings.hiddenPackages.toSet(),
            iconRepository = container.iconRepository,
            onToggle = viewModel::toggleHidden,
            onDismiss = { showHiddenManager = false },
        )
    }
}

@Composable
private fun AccentPicker(
    selected: AccentTheme,
    onSelect: (AccentTheme) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.label_accent),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentTheme.entries.forEach { accent ->
                ColorChoice(
                    color = accent.primary(),
                    selected = accent == selected,
                    onClick = { onSelect(accent) },
                )
            }
        }
    }
}

@Composable
private fun DockSlotList(
    dockPackages: List<String>,
    apps: List<AppInfo>,
    iconRepository: IconRepository,
    onEdit: (Int) -> Unit,
) {
    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    Column {
        repeat(DOCK_SLOT_COUNT) { index ->
            val packageName = dockPackages.getOrNull(index)
            val app = packageName?.let { appByPackage[it] }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(index) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (app != null) {
                    AppIcon(app = app, sizeDp = 36, iconRepository = iconRepository)
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = app?.label ?: stringResource(R.string.dock_slot_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (app != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.dock_pick_app),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WallpaperPicker(
    selected: WallpaperChoice,
    onSelect: (WallpaperChoice) -> Unit,
    onPickPhoto: () -> Unit,
    onOpenSystem: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
        Text(
            text = stringResource(R.string.wallpaper_builtin),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WallpaperChoice.entries
                .filter { it != WallpaperChoice.CUSTOM }
                .forEach { choice ->
                    WallpaperSwatch(
                        choice = choice,
                        selected = choice == selected,
                        onClick = { onSelect(choice) },
                    )
                }
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onPickPhoto) {
            Text(stringResource(R.string.wallpaper_photo))
        }
        TextButton(onClick = onOpenSystem) {
            Text(stringResource(R.string.wallpaper_system))
        }
    }
}

@Composable
private fun WallpaperSwatch(
    choice: WallpaperChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = choice.gradientColors()
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RoadmapRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.roadmap_state),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<AppInfo>,
    iconRepository: IconRepository,
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val filtered = remember(apps, query) { AppSearch.filter(apps, query) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(18.dp),
        ) {
            Text(
                text = stringResource(R.string.dock_picker_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VexSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.dock_picker_search),
                focusRequester = focusRequester,
                onClear = { query = "" },
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app = app, sizeDp = 36, iconRepository = iconRepository)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenAppsDialog(
    apps: List<AppInfo>,
    hiddenPackages: Set<String>,
    iconRepository: IconRepository,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(18.dp),
        ) {
            Text(
                text = stringResource(R.string.hidden_manager),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.hidden_apps_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.drawer_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggle(app.packageName, !hiddenPackages.contains(app.packageName))
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app = app, sizeDp = 32, iconRepository = iconRepository)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = hiddenPackages.contains(app.packageName),
                            onCheckedChange = { checked ->
                                onToggle(app.packageName, checked)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatScale(scale: Float): String {
    val percent = (scale * 100).toInt()
    return if (percent <= 0) "0%" else "$percent%"
}
