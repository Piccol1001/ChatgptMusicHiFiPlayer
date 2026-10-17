package com.piccol.hifi.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.HiFiApplication
import com.piccol.hifi.MainActivity
import com.piccol.hifi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CompactWidget : PlayerWidget(0)

class StandardWidget : PlayerWidget(1)

class ExpandedWidget : PlayerWidget(2)

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CompactWidget()
}

class StandardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StandardWidget()
}

class ExpandedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ExpandedWidget()
}

@androidx.annotation.OptIn(UnstableApi::class)
abstract class PlayerWidget(private val kind: Int) : GlanceAppWidget() {
    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(120.dp, 120.dp),
                DpSize(250.dp, 110.dp),
                DpSize(250.dp, 260.dp),
                DpSize(320.dp, 340.dp),
            )
        )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HiFiApplication
        app.widgets.seed(app.preferences.snapshot(), force = !app.serviceRunning.value)
        val initial = app.widgets.snapshots.first()
        val appearance = app.widgets.appearance.first()
        provideContent {
            val stored by app.widgets.snapshots.collectAsState(initial)
            val style by app.widgets.appearance.collectAsState(appearance)
            val serviceAlive by app.serviceRunning.collectAsState()
            val snapshot = if (serviceAlive) stored else stored.copy(playing = false)
            val art by
                produceState<Bitmap?>(null, snapshot.artwork) {
                    value = loadWidgetArt(context, snapshot.artwork)
                }
            WidgetContent(context, snapshot, style, art, kind)
        }
    }
}

suspend fun refreshWidgets(context: Context) {
    CompactWidget().updateAll(context)
    StandardWidget().updateAll(context)
    ExpandedWidget().updateAll(context)
}

private suspend fun loadWidgetArt(context: Context, value: String?): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
                if (value.isNullOrBlank()) return@runCatching null
                val uri = Uri.parse(value)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                val options =
                    BitmapFactory.Options().apply {
                        inSampleSize =
                            Integer.highestOneBit(
                                (maxOf(bounds.outWidth, bounds.outHeight) / 256).coerceAtLeast(1)
                            )
                    }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }
            .getOrNull()
    }

@UnstableApi
@Composable
private fun WidgetContent(
    context: Context,
    s: WidgetSnapshot,
    appearance: WidgetAppearance,
    art: Bitmap?,
    kind: Int,
) {
    val size = LocalSize.current
    val accent =
        ColorProvider(
            listOf(Color(0xFF58A6FF), Color(0xFF7BE0C3), Color(0xFFCCA3FF))[appearance.accent]
        )
    val open =
        actionStartActivity(
            Intent(context, MainActivity::class.java)
                .putExtra("open_player", true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
        )
    val foreground = ColorProvider(Color(0xFFE8EEF8))
    val secondary = ColorProvider(Color(0xFFB8C7DA))
    val root =
        GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(
                ImageProvider(
                    if (appearance.transparent) R.drawable.widget_background_transparent
                    else R.drawable.widget_background_dark
                )
            )
            .cornerRadius(20.dp)
            .clickable(open)
            .padding(8.dp)
    val compact = kind == 0 || size.width < 230.dp
    val expanded = kind == 2 && size.height >= 240.dp && !compact
    @Composable
    fun artwork(modifier: GlanceModifier) {
        Image(
            art?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.ic_launcher),
            "Open Now Playing: ${s.title}",
            modifier.clickable(open).cornerRadius(12.dp),
            contentScale = ContentScale.Fit,
        )
    }
    @Composable
    fun control(command: String, label: String, icon: Int, selected: Boolean = false) {
        Image(
            ImageProvider(icon),
            label,
            GlanceModifier.size(48.dp)
                .clickable(
                    actionRunCallback<WidgetAction>(actionParametersOf(widgetCommand to command))
                )
                .padding(12.dp),
            colorFilter = ColorFilter.tint(if (selected) accent else foreground),
        )
    }
    @Composable
    fun transport() {
        Row(
            GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            if (!compact)
                control("previous", "Previous track", android.R.drawable.ic_media_previous)
            control(
                "toggle",
                if (s.playing) "Pause" else "Play",
                if (s.playing) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                true,
            )
            control("next", "Next track", android.R.drawable.ic_media_next)
        }
    }
    Column(root, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
        if (compact) {
            artwork(GlanceModifier.fillMaxWidth().defaultWeight())
            transport()
        } else if (!expanded) {
            Row(
                GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                artwork(GlanceModifier.size(64.dp))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        s.title,
                        maxLines = 1,
                        style =
                            TextStyle(
                                color = foreground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Text(
                        s.artist,
                        maxLines = 1,
                        style = TextStyle(color = secondary, fontSize = 12.sp),
                    )
                    transport()
                }
            }
        } else {
            artwork(GlanceModifier.fillMaxWidth().defaultWeight())
            Text(
                s.title,
                maxLines = 1,
                style =
                    TextStyle(color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            Text(s.artist, maxLines = 1, style = TextStyle(color = secondary, fontSize = 14.sp))
            if (size.height >= 300.dp && s.album.isNotBlank())
                Text(s.album, maxLines = 1, style = TextStyle(color = secondary, fontSize = 12.sp))
            val position = s.currentPosition()
            if (s.playing && s.speed == 1f) {
                val views = RemoteViews(context.packageName, R.layout.widget_clock)
                views.setChronometer(
                    R.id.widget_elapsed,
                    SystemClock.elapsedRealtime() - position,
                    null,
                    true,
                )
                AndroidRemoteViews(
                    views,
                    GlanceModifier.fillMaxWidth().height(26.dp).clickable(open),
                )
                Text(
                    "/ ${clock(s.duration)} · tap to seek",
                    style = TextStyle(color = secondary, fontSize = 12.sp),
                )
            } else
                Text(
                    "${clock(position)} / ${clock(s.duration)}${if(s.playing)" · ${s.speed}×"else ""} · tap to seek",
                    style = TextStyle(color = secondary, fontSize = 12.sp),
                )
            transport()
            Row(
                GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                control(
                    "shuffle",
                    if (s.shuffle) "Shuffle on" else "Shuffle off",
                    R.drawable.ic_widget_shuffle,
                    s.shuffle,
                )
                control(
                    "repeat",
                    "Repeat ${when(s.repeat){1->"one"
2->"all"
else->"off"}}",
                    if (s.repeat == 1) R.drawable.ic_widget_repeat_one
                    else R.drawable.ic_widget_repeat,
                    s.repeat != 0,
                )
                Image(
                    ImageProvider(R.drawable.ic_launcher),
                    "Open full player",
                    GlanceModifier.size(48.dp).clickable(open).padding(12.dp),
                )
            }
        }
    }
}

private fun clock(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
