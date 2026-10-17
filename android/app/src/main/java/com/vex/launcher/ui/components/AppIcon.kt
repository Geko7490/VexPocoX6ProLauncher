package com.vex.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vex.launcher.R
import com.vex.launcher.core.model.AppInfo
import com.vex.launcher.data.IconRepository
import kotlin.math.roundToInt

/**
 * Uygulama simgesi.
 *
 * Simge, hedef boyutta bitmap'e dönüştürülüp önbellekten okunur. Yükleme sırasında
 * baş harf içeren bir yer tutucu gösterilir; bu sayede ilk karede boşluk oluşmaz.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    sizeDp: Int,
    iconRepository: IconRepository,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizePx = remember(sizeDp, density) {
        with(density) { sizeDp.dp.toPx() }.roundToInt().coerceAtLeast(48)
    }

    val imageBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        app.packageName,
        app.activityName,
        sizePx,
    ) {
        value = iconRepository.load(app, sizePx)
    }

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.cd_app_icon, app.label),
                modifier = Modifier.fillMaxSize(),
                filterQuality = FilterQuality.Low,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(sizeDp.dp / 4))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Ana ekran / çekmece ızgarasındaki tek bir uygulama öğesi (simge + etiket).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    iconSizeDp: Int,
    showLabel: Boolean,
    labelScale: Float,
    iconRepository: IconRepository,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onClick(app) },
                onLongClick = { onLongClick(app) },
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            app = app,
            sizeDp = iconSizeDp,
            iconRepository = iconRepository,
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11f * labelScale).sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
