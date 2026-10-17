package com.vex.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vex.launcher.R
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.core.model.LauncherSettings
import com.vex.launcher.core.util.detailsIntent
import com.vex.launcher.core.util.launchIntent
import com.vex.launcher.core.util.uninstallIntent
import com.vex.launcher.data.IconRepository
import com.vex.launcher.di.AppContainer
import com.vex.launcher.ui.components.AppGridItem
import com.vex.launcher.ui.components.AppLongPressSheet
import com.vex.launcher.ui.components.MessageBlock
import com.vex.launcher.ui.components.currentLocale
import com.vex.launcher.ui.theme.gradientColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val BASE_ICON_SIZE_DP = 48

/**
 * Ana ekran: saat, uygulama ızgarası (sayfalanabilir), dock ve çekmece tutamağı.
 */
@Composable
fun HomeScreen(
    container: AppContainer,
    settings: LauncherSettings,
    isDefaultHome: Boolean,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    val iconSizeDp = (BASE_ICON_SIZE_DP * settings.iconScale).roundToInt().coerceIn(36, 84)
    val wallpaperBrush = remember(settings.wallpaper) {
        Brush.verticalGradient(settings.wallpaper.gradientColors())
    }

    val openApp: (AppInfo) -> Unit = { app ->
        app.launchIntent(context)?.let { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    val openAppInfo: (AppInfo) -> Unit = { app ->
        runCatching { context.startActivity(app.detailsIntent()) }
    }

    val uninstallApp: (AppInfo) -> Unit = { app ->
        runCatching { context.startActivity(app.uninstallIntent()) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(wallpaperBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            HomeTopBar(
                onOpenSearch = { onOpenDrawer(true) },
                onOpenSettings = onOpenSettings,
            )

            if (!isDefaultHome) {
                DefaultHomeBanner(
                    onRequestDefaultHome = onRequestDefaultHome,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            val pages = state.pages
            if (state.isLoading && pages.all { it.isEmpty() }) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.drawer_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.error != null && pages.all { it.isEmpty() }) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    MessageBlock(
                        title = stringResource(R.string.drawer_error_title),
                        body = stringResource(R.string.drawer_error_body),
                        action = {
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(),
                            ) {
                                Text(stringResource(R.string.action_retry))
                            }
                        },
                    )
                }
            } else {
                HomePager(
                    pages = pages,
                    settings = settings,
                    iconRepository = container.iconRepository,
                    onAppClick = openApp,
                    onAppLongClick = { selectedApp = it },
                    modifier = Modifier.weight(1f),
                )
            }

            DrawerHandle(onOpen = { onOpenDrawer(false) })

            DockBar(
                dockApps = state.dockApps,
                iconSizeDp = iconSizeDp,
                settings = settings,
                iconRepository = container.iconRepository,
                onAppClick = openApp,
                onAppLongClick = { selectedApp = it },
            )
        }
    }

    selectedApp?.let { app ->
        AppLongPressSheet(
            app = app,
            isInDock = state.dockApps.any { it.packageName == app.packageName },
            dockFull = state.dockApps.size >= HomeViewModel.MAX_DOCK_ITEMS,
            isHidden = settings.hiddenPackages.contains(app.packageName),
            iconRepository = container.iconRepository,
            onDismiss = { selectedApp = null },
            onOpen = {
                selectedApp = null
                openApp(app)
            },
            onAppInfo = {
                selectedApp = null
                openAppInfo(app)
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
                uninstallApp(app)
            },
        )
    }
}

@Composable
private fun HomeTopBar(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClockHeader(modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_open_drawer),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ClockHeader(modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM", locale) }

    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            time = now.format(timeFormatter)
            date = now.format(dateFormatter)
            delay(1_000L)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = time,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DefaultHomeBanner(
    onRequestDefaultHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_default_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_default_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRequestDefaultHome,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.home_default_action))
        }
    }
}

@Composable
private fun HomePager(
    pages: List<List<AppInfo>>,
    settings: LauncherSettings,
    iconRepository: IconRepository,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            pageSpacing = 0.dp,
        ) { page ->
            AppGridPage(
                apps = pages[page],
                settings = settings,
                iconRepository = iconRepository,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
            )
        }
        PageIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
        )
    }
}

@Composable
private fun AppGridPage(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    iconRepository: IconRepository,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconSizeDp = (BASE_ICON_SIZE_DP * settings.iconScale).roundToInt().coerceIn(36, 84)

    if (apps.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.home_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.gridColumns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = apps, key = { it.packageName }) { app ->
            AppGridItem(
                app = app,
                iconSizeDp = iconSizeDp,
                showLabel = settings.showLabels,
                labelScale = settings.labelScale,
                iconRepository = iconRepository,
                onClick = onAppClick,
                onLongClick = onAppLongClick,
            )
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    if (pageCount <= 1) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pageCount) { index ->
                val active = index == currentPage
                Box(
                    modifier = Modifier
                        .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DrawerHandle(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 4.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .pointerInput(onOpen) {
                var triggered = false
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        // Yukarı kaydırma çekmeceyi açar.
                        if (!triggered && dragAmount < -18f) {
                            triggered = true
                            onOpen()
                        }
                    },
                    onDragEnd = { triggered = false },
                    onDragCancel = { triggered = false },
                )
            }
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.home_all_apps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DockBar(
    dockApps: List<AppInfo>,
    iconSizeDp: Int,
    settings: LauncherSettings,
    iconRepository: IconRepository,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dockApps.isEmpty()) {
            Text(
                text = stringResource(R.string.home_dock_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        } else {
            dockApps.forEach { app ->
                key(app.packageName) {
                    AppGridItem(
                        app = app,
                        iconSizeDp = iconSizeDp,
                        showLabel = settings.showLabels,
                        labelScale = settings.labelScale,
                        iconRepository = iconRepository,
                        onClick = onAppClick,
                        onLongClick = onAppLongClick,
                    )
                }
            }
        }
    }
}
