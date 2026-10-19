package com.piccol.hifi.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val PlayerTypography =
    Typography(
        headlineLarge =
            TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineMedium =
            TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall =
            TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleLarge =
            TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium =
            TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
        titleSmall =
            TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge =
            TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium =
            TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    )

@Composable
fun CompactTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .16f)
                else Color.Transparent
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionHeading(
    title: String,
    detail: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun MusicEmptyState(
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Rounded.Album,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .09f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(action)
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    description: String? = null,
    icon: ImageVector = Icons.Rounded.Tune,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null)
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
fun artworkAccent(uri: String?, enabled: Boolean, fallback: Color): Color {
    val context = LocalContext.current
    var sampled by remember(uri) { mutableStateOf<Color?>(null) }
    LaunchedEffect(uri, enabled) {
        sampled = null
        if (enabled && uri != null) {
            sampled =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val result =
                                context.imageLoader.execute(
                                    ImageRequest.Builder(context)
                                        .data(uri)
                                        .size(48)
                                        .allowHardware(false)
                                        .build()
                                )
                            val bitmap =
                                (result.drawable as? BitmapDrawable)?.bitmap
                                    ?: return@runCatching null
                            var red = 0L
                            var green = 0L
                            var blue = 0L
                            var n = 0
                            for (y in 0 until bitmap.height step 3) for (x in
                                0 until bitmap.width step 3) {
                                val p = bitmap.getPixel(x, y)
                                red += android.graphics.Color.red(p)
                                green += android.graphics.Color.green(p)
                                blue += android.graphics.Color.blue(p)
                                n++
                            }
                            val hsv = FloatArray(3)
                            android.graphics.Color.RGBToHSV(
                                (red / n).toInt(),
                                (green / n).toInt(),
                                (blue / n).toInt(),
                                hsv,
                            )
                            hsv[1] = hsv[1].coerceIn(.25f, .6f)
                            hsv[2] = .95f
                            Color(android.graphics.Color.HSVToColor(hsv))
                        }
                        .getOrNull()
                }
        }
    }
    return if (enabled) sampled ?: fallback else fallback
}

object PlayerSpacing {
    val screen = 16.dp
    val gap = 8.dp
    val section = 24.dp
    val artworkRadius = 14.dp
}

@Composable
fun ScreenHeader(
    title: String,
    back: (() -> Unit)? = null,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) IconButton(back) { Icon(Icons.Rounded.ChevronLeft, backLabel) }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun PlayerNavigation(selected: PrimarySection?, open: (PrimarySection) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        PrimarySection.entries.forEach { section ->
            val active = selected == section
            val tint =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                Modifier.weight(1f)
                    .selectable(active, role = Role.Tab) { open(section) }
                    .semantics { contentDescription = "Open ${section.label}" }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    when (section) {
                        PrimarySection.LIBRARY -> Icons.Outlined.LibraryMusic
                        PrimarySection.PLAYER -> Icons.Outlined.Album
                        PrimarySection.AUDIO -> Icons.Outlined.Tune
                    },
                    null,
                    Modifier.size(24.dp),
                    tint,
                )
                Text(
                    section.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun LibraryControls(
    filter: com.piccol.hifi.data.LibraryFilter,
    playlists: Boolean,
    search: (String) -> Unit,
    category: (com.piccol.hifi.data.Category) -> Unit,
    openPlaylists: () -> Unit,
    rescan: () -> Unit,
) {
    @Composable
    fun CategoryTab(label: String, selected: Boolean, click: () -> Unit) {
        CompactTab(
            label,
            selected,
            click,
            modifier = Modifier.semantics { contentDescription = "Library category $label" },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextField(
            filter.search,
            search,
            Modifier.fillMaxWidth().padding(horizontal = PlayerSpacing.screen, vertical = 4.dp),
            singleLine = true,
            placeholder = {
                Text(
                    if (playlists && filter.playlist == null && filter.smart == null)
                        "Search playlists"
                    else "Songs, artists, albums",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (filter.search.isNotEmpty())
                    IconButton({ search("") }) { Icon(Icons.Rounded.Close, "Clear search") }
            },
            shape = RoundedCornerShape(14.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                CategoryTab(
                    "Songs",
                    !playlists && filter.category == com.piccol.hifi.data.Category.ALL,
                    { category(com.piccol.hifi.data.Category.ALL) },
                )
            }
            item {
                CategoryTab(
                    "Albums",
                    !playlists && filter.category == com.piccol.hifi.data.Category.ALBUMS,
                    { category(com.piccol.hifi.data.Category.ALBUMS) },
                )
            }
            item {
                CategoryTab(
                    "Artists",
                    !playlists && filter.category == com.piccol.hifi.data.Category.ARTISTS,
                    { category(com.piccol.hifi.data.Category.ARTISTS) },
                )
            }
            item { CategoryTab("Playlists", playlists, openPlaylists) }
            item {
                CategoryTab(
                    "Folders",
                    !playlists && filter.category == com.piccol.hifi.data.Category.FOLDERS,
                    { category(com.piccol.hifi.data.Category.FOLDERS) },
                )
            }
            item {
                var more by remember { mutableStateOf(false) }
                Box {
                    IconButton({ more = true }) {
                        Icon(Icons.Rounded.MoreHoriz, "More library views")
                    }
                    DropdownMenu(more, { more = false }) {
                        listOf(
                                com.piccol.hifi.data.Category.GENRES,
                                com.piccol.hifi.data.Category.FAVORITES,
                                com.piccol.hifi.data.Category.ADDED,
                                com.piccol.hifi.data.Category.RECENT,
                                com.piccol.hifi.data.Category.MOST,
                            )
                            .forEach { view ->
                                DropdownMenuItem(
                                    text = { Text(view.label) },
                                    onClick = {
                                        category(view)
                                        more = false
                                    },
                                )
                            }
                        DropdownMenuItem(
                            text = { Text("Rescan library") },
                            onClick = {
                                rescan()
                                more = false
                            },
                        )
                    }
                }
            }
        }
        if (
            !playlists &&
                filter.category in
                    listOf(
                        com.piccol.hifi.data.Category.GENRES,
                        com.piccol.hifi.data.Category.FAVORITES,
                        com.piccol.hifi.data.Category.ADDED,
                        com.piccol.hifi.data.Category.RECENT,
                        com.piccol.hifi.data.Category.MOST,
                    )
        )
            Text(
                filter.category.label,
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
    }
}
