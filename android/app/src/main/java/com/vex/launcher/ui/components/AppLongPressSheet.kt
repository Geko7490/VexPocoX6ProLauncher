package com.vex.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vex.launcher.R
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.data.IconRepository

/**
 * Uygulama simgesine uzun basıldığında açılan işlevsel menü.
 * Her öğe gerçek bir işlevi tetikler; sahte/placeholder öğe yoktur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLongPressSheet(
    app: AppInfo,
    isInDock: Boolean,
    dockFull: Boolean,
    isHidden: Boolean,
    iconRepository: IconRepository,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onAddToDock: () -> Unit,
    onRemoveFromDock: () -> Unit,
    onToggleHidden: () -> Unit,
    onUninstall: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, sizeDp = 48, iconRepository = iconRepository)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            ThinDivider()

            MenuRow(
                icon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                label = stringResource(R.string.menu_open_app),
                onClick = onOpen,
            )
            MenuRow(
                icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                label = stringResource(R.string.menu_app_info),
                onClick = onAppInfo,
            )
            if (isInDock) {
                MenuRow(
                    icon = { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null) },
                    label = stringResource(R.string.menu_remove_dock),
                    onClick = onRemoveFromDock,
                )
            } else {
                MenuRow(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = if (dockFull) {
                        stringResource(R.string.menu_dock_full)
                    } else {
                        stringResource(R.string.menu_add_dock)
                    },
                    enabled = !dockFull,
                    onClick = onAddToDock,
                )
            }
            MenuRow(
                icon = {
                    Icon(
                        imageVector = if (isHidden) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        },
                        contentDescription = null,
                    )
                },
                label = if (isHidden) {
                    stringResource(R.string.menu_unhide)
                } else {
                    stringResource(R.string.menu_hide)
                },
                onClick = onToggleHidden,
            )
            MenuRow(
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                label = stringResource(R.string.menu_uninstall),
                onClick = onUninstall,
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxedIcon(icon)
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun BoxedIcon(icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier.size(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
    }
}
