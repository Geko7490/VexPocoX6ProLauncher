package com.vex.launcher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vex.launcher.R
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.core.util.detailsIntent
import com.vex.launcher.core.util.launchIntent
import com.vex.launcher.core.util.uninstallIntent
import com.vex.launcher.di.AppContainer
import com.vex.launcher.ui.components.AppGridItem
import com.vex.launcher.ui.components.AppLongPressSheet
import com.vex.launcher.ui.components.MessageBlock
import com.vex.launcher.ui.components.VexSearchField
import com.vex.launcher.ui.theme.gradientColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tam ekran uygulama çekmecesi.
 *
 * - Arama alanı her zaman görünürdür (kalıcı arama).
 * - Liste lazy grid olarak çizilir; filtreleme ViewModel'de, ana iş parçacığı dışında yapılır.
 * - Sağ taraftaki A-Z çubuğu ile hızlı kaydırma yapılabilir.
 */
@Composable
fun AppDrawerScreen(
    container: AppContainer,
    settings: LauncherSettings,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AppDrawerViewModel = viewModel(factory = AppDrawerViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    val wallpaperBrush = remember(settings.wallpaper) {
        Brush.verticalGradient(settings.wallpaper.gradientColors())
    }
    val iconSizeDp = (48 * settings.iconScale).toInt().coerceIn(36, 84)

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            // Ekran geçiş animasyonunun tamamlanması beklenir, sonra arama alanına odaklanılır.
            delay(260)
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(wallpaperBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.drawer_search_hint),
                    focusRequester = focusRequester,
                    onClear = viewModel::clearQuery,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = stringResource(R.string.drawer_count, state.apps.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading && state.apps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.drawer_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    state.error != null && state.apps.isEmpty() -> {
                        MessageBlock(
                            title = stringResource(R.string.drawer_error_title),
                            body = stringResource(R.string.drawer_error_body),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    state.apps.isEmpty() -> {
                        MessageBlock(
                            title = stringResource(R.string.drawer_empty_title),
                            body = stringResource(R.string.drawer_empty_body),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(settings.gridColumns),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 2.dp,
                                end = 26.dp,
                                top = 4.dp,
                                bottom = 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(items = state.apps, key = { it.packageName }) { app ->
                                AppGridItem(
                                    app = app,
                                    iconSizeDp = iconSizeDp,
                                    showLabel = settings.showLabels,
                                    labelScale = settings.labelScale,
                                    iconRepository = container.iconRepository,
                                    onClick = { selected ->
                                        selected.launchIntent(context)?.let { intent ->
                                            runCatching { context.startActivity(intent) }
                                        }
                                    },
                                    onLongClick = { selectedApp = it },
                                )
                            }
                        }

                        AlphabetScroller(
                            letters = state.letterIndex.keys.toList(),
                            onSelect = { letter ->
                                state.letterIndex[letter]?.let { index ->
                                    coroutineScope.launch {
                                        gridState.scrollToItem(index.coerceAtLeast(0))
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }

    selectedApp?.let { app ->
        AppLongPressSheet(
            app = app,
            isInDock = settings.dockPackages.contains(app.packageName),
            dockFull = settings.dockPackages.size >= AppDrawerViewModel.MAX_DOCK_ITEMS,
            isHidden = settings.hiddenPackages.contains(app.packageName),
            iconRepository = container.iconRepository,
            onDismiss = { selectedApp = null },
            onOpen = {
                selectedApp = null
                app.launchIntent(context)?.let { intent ->
                    runCatching { context.startActivity(intent) }
                }
            },
            onAppInfo = {
                selectedApp = null
                runCatching { context.startActivity(app.detailsIntent()) }
            },
            onAddToDock = {
                viewModel.addToDock(app.packageName)
                selectedApp = null
            },
            onRemoveFromDock = {
                viewModel.removeFromDock(app.packageName)
                selectedApp = null
            },
            onToggleHidden = {
                if (settings.hiddenPackages.contains(app.packageName)) {
                    viewModel.unhideApp(app.packageName)
                } else {
                    viewModel.hideApp(app.packageName)
                }
                selectedApp = null
            },
            onUninstall = {
                selectedApp = null
                runCatching { context.startActivity(app.uninstallIntent()) }
            },
        )
    }
}

@Composable
private fun AlphabetScroller(
    letters: List<Char>,
    onSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.size < 3) return
    Column(
        modifier = modifier.width(22.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                fontSize = 9.sp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSelect(letter) }
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            )
        }
    }
}
