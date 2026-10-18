package com.piccol.hifi.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import com.piccol.hifi.dsp.*
import kotlin.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun EqualizerScreen(vm: PlayerViewModel, presetRequest: Int = 0, profiles: () -> Unit = {}) {
    val profileState by vm.profileState.collectAsStateWithLifecycle()
    val route by vm.outputRoute.collectAsStateWithLifecycle()
    val settings by vm.dspSettings.collectAsStateWithLifecycle()
    val format by vm.audioEngine.format.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var clips by remember { mutableLongStateOf(vm.audioEngine.clippedSamples) }
    LaunchedEffect(vm, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                clips = vm.audioEngine.clippedSamples
                delay(250)
            }
        }
    }
    val custom by vm.customPresets.collectAsStateWithLifecycle()
    val favorites by vm.presetFavorites.collectAsStateWithLifecycle()
    val recent by vm.recentPresets.collectAsStateWithLifecycle()
    val tags by vm.replayTags.collectAsStateWithLifecycle()
    var presetSheet by remember { mutableStateOf(false) }
    var editingFilter by remember { mutableStateOf<Int?>(null) }
    var engineInfo by remember { mutableStateOf(false) }
    var headroomInfo by remember { mutableStateOf(false) }
    LaunchedEffect(presetRequest) { if (presetRequest > 0) presetSheet = true }
    var save by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var preview by remember { mutableStateOf<EqPreset?>(null) }
    var rename by remember { mutableStateOf<EqPreset?>(null) }
    var numeric by remember { mutableStateOf<Pair<String, Pair<Double, (Double) -> Unit>>?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                scope.launch {
                    try {
                        val text =
                            withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)!!.use { input ->
                                    com.piccol.hifi.library.readLimited(input, 2097152)
                                }
                            }
                        vm.importPresets(text)
                    } catch (_: Exception) {
                        vm.message.value =
                            "Could not import presets. Choose a valid HiFi Player JSON file up to 2 MiB."
                    }
                }
        }
    val exporter =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null)
                scope.launch {
                    try {
                        val all =
                            custom.ifEmpty {
                                listOf(EqPreset("export", settings.preset, "Custom", settings))
                            }
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use {
                                it.write(encodePresets(all))
                            }
                        }
                        vm.message.value = "Presets exported"
                    } catch (_: Exception) {
                        vm.message.value = "Could not write the preset file."
                    }
                }
        }
    fun change(transform: (DspSettings) -> DspSettings) {
        vm.changeDsp { transform(it).copy(preset = "Custom") }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 600.dp
        val graphMinimum = (80 + 24 * LocalDensity.current.fontScale).dp
        val graphHeight =
            if (compact) (maxHeight * .22f).coerceIn(graphMinimum, maxOf(graphMinimum, 144.dp))
            else 192.dp
        val bandHeight = if (compact) (maxHeight * .20f).coerceIn(88.dp, 132.dp) else 176.dp
        LazyColumn(
            contentPadding =
                PaddingValues(horizontal = 16.dp, vertical = if (compact) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton({ presetSheet = true }, Modifier.weight(1f)) {
                        Text(settings.preset, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ExpandMore, null)
                    }
                    IconButton({ save = true }) {
                        Icon(Icons.Rounded.BookmarkAdd, "Save custom preset")
                    }
                    IconButton({
                        change {
                            it.copy(
                                ten = List(10) { 0.0 },
                                thirtyOne = List(31) { 0.0 },
                                filters = TEN_BANDS.map { f -> EqFilter(frequency = f) },
                                preamp = 0.0,
                            )
                        }
                    }) {
                        Icon(Icons.Rounded.RestartAlt, "Reset all EQ bands")
                    }
                }
                profileState.active(route)?.let { p ->
                    Text("Editing profile · ${p.name}", style = MaterialTheme.typography.bodySmall)
                    TextButton(vm::useBaseEq) { Text("Return to personal EQ") }
                }
            }
            item {
                val wideTabs = LocalDensity.current.fontScale > 1.2f || maxWidth < 340.dp
                Row(
                    Modifier.fillMaxWidth()
                        .then(
                            if (wideTabs) Modifier.horizontalScroll(rememberScrollState())
                            else Modifier
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EqMode.entries.forEach { mode ->
                        CompactTab(
                            when (mode) {
                                EqMode.TEN -> "10 bands"
                                EqMode.PARAMETRIC -> "Parametric"
                                EqMode.THIRTY_ONE -> "31 bands"
                            },
                            settings.mode == mode,
                            { vm.changeDsp { it.copy(mode = mode) } },
                            modifier = if (wideTabs) Modifier else Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                ResponseGraph(settings, format.rate, graphHeight) { index, f, g ->
                    change { s ->
                        s.copy(
                            filters =
                                s.filters.mapIndexed { i, old ->
                                    if (i == index) old.copy(frequency = f, gain = g) else old
                                }
                        )
                    }
                }
            }
            if (settings.mode != EqMode.PARAMETRIC)
                item {
                    val frequencies = if (settings.mode == EqMode.TEN) TEN_BANDS else THIRD_OCTAVE
                    val gains =
                        if (settings.mode == EqMode.TEN) settings.ten else settings.thirtyOne
                    Text(
                        "Drag a band · tap its value for exact gain",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        frequencies.forEachIndexed { i, f ->
                            val update: (Double) -> Unit = { gain ->
                                change { s ->
                                    if (s.mode == EqMode.TEN)
                                        s.copy(ten = s.ten.toMutableList().apply { set(i, gain) })
                                    else
                                        s.copy(
                                            thirtyOne =
                                                s.thirtyOne.toMutableList().apply { set(i, gain) }
                                        )
                                }
                            }
                            Column(
                                Modifier.width(
                                    if (LocalDensity.current.fontScale > 1.3f) 72.dp else 56.dp
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                TextButton(
                                    { numeric = frequencyLabel(f) to (gains[i] to update) },
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text(
                                        "%+.1f".format(gains[i]),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                VerticalGain(
                                    gains[i],
                                    f < format.rate * .5,
                                    frequencyLabel(f),
                                    update,
                                    bandHeight,
                                )
                                Text(frequencyLabel(f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            if (settings.mode == EqMode.PARAMETRIC) {
                item {
                    SectionHeading(
                        "Filters",
                        "${settings.filters.size} active slots · tap to edit",
                    ) {
                        IconButton(
                            {
                                change {
                                    it.copy(filters = it.filters + EqFilter(frequency = 1000.0))
                                }
                            },
                            enabled = settings.filters.size < 32,
                        ) {
                            Icon(Icons.Rounded.Add, "Add filter")
                        }
                    }
                }
                items(settings.filters.size) { i ->
                    val f = settings.filters[i]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        onClick = { editingFilter = i },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${i+1} · ${f.type.name.replace('_',' ')}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "${frequencyLabel(f.frequency)} · ${if(f.type==FilterType.LOW_PASS||f.type==FilterType.HIGH_PASS)"cutoff" else "%+.1f dB".format(f.gain)} · Q %.2f"
                                        .format(f.q),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                f.enabled,
                                { enabled ->
                                    change { s ->
                                        s.copy(
                                            filters =
                                                s.filters.mapIndexed { j, old ->
                                                    if (j == i) old.copy(enabled = enabled) else old
                                                }
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Preamp", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "%+.1f dB".format(settings.preamp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    settings.preamp.toFloat(),
                    { value -> change { it.copy(preamp = (value * 4).roundToInt() / 4.0) } },
                    valueRange = -24f..12f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Automatic headroom",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        settings.autoHeadroom,
                        { enabled -> change { it.copy(autoHeadroom = enabled) } },
                    )
                }
                val effective =
                    remember(settings, format.rate) { effectivePreamp(settings, format.rate) }
                Text(
                    if (settings.enabled)
                        "Compensation %+.1f dB · Applied %+.1f dB"
                            .format(effective - settings.preamp, effective)
                    else "EQ bypassed · applied EQ preamp 0.0 dB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton({ headroomInfo = !headroomInfo }) {
                    Icon(Icons.Rounded.Info, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Headroom explained")
                }
                if (headroomInfo)
                    Text(
                        "Requested preamp is your slider setting. Automatic headroom can lower it using the combined response and a 0.5 dB safety margin. Applied is the effective EQ preamp; ReplayGain is separate. When EQ is bypassed, its filters and preamp are not applied.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
            item {
                SettingRow(
                    "AutoEq & output profiles",
                    "Import and assign local correction profiles",
                    Icons.Rounded.Headphones,
                    profiles,
                )
            }
            item {
                HorizontalDivider()
                Text("ReplayGain", style = MaterialTheme.typography.titleLarge)
                Row {
                    ReplayMode.entries.forEach { mode ->
                        FilterChip(
                            settings.replayMode == mode,
                            { vm.changeDsp { it.copy(replayMode = mode) } },
                            label = { Text(mode.name) },
                        )
                    }
                }
                Text("ReplayGain preamp: ${"%.1f".format(settings.replayPreamp)} dB")
                Slider(
                    settings.replayPreamp.toFloat(),
                    { value ->
                        vm.changeDsp { it.copy(replayPreamp = (value * 4).roundToInt() / 4.0) }
                    },
                    valueRange = -12f..12f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prevent clipping using file peak", Modifier.weight(1f))
                    Switch(
                        settings.preventClipping,
                        { v -> vm.changeDsp { it.copy(preventClipping = v) } },
                    )
                }
                Text(
                    if (tags.decoderGainActive)
                        "Decoder header gain is active; extra ReplayGain is bypassed to avoid applying gain twice."
                    else if (tags.trackGain == null && tags.albumGain == null)
                        "No supported ReplayGain tag in the current track; no ReplayGain adjustment is applied."
                    else
                        "Current ReplayGain adjustment: %.2f dB".format(replayGain(settings, tags)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Reads ID3 TXXX and FLAC/Vorbis comments. R128/decoder gain is not reapplied. No dynamic compressor or limiter is added.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            item {
                TextButton({ engineInfo = !engineInfo }) {
                    Text("Technical details")
                    Icon(
                        if (engineInfo) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null,
                    )
                }
                if (engineInfo)
                    Text(
                        "16-bit PCM · ${format.rate} Hz · ${clips} clipped samples this service session. No bit-perfect claim. Bands at/above Nyquist are bypassed; mode conversion interpolates control gains and is approximate.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
    }
    editingFilter
        ?.takeIf { it in settings.filters.indices }
        ?.let { index ->
            val f = settings.filters[index]
            fun edit(transform: (EqFilter) -> EqFilter) {
                change { s ->
                    s.copy(
                        filters =
                            s.filters.mapIndexed { i, old ->
                                if (i == index) transform(old) else old
                            }
                    )
                }
            }
            ModalBottomSheet({ editingFilter = null }) {
                Column(
                    Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Filter ${index+1}", style = MaterialTheme.typography.titleLarge)
                    var types by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton({ types = true }) {
                            Text(f.type.name.replace('_', ' '))
                            Icon(Icons.Rounded.ExpandMore, null)
                        }
                        DropdownMenu(types, { types = false }) {
                            FilterType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace('_', ' ')) },
                                    onClick = {
                                        edit { it.copy(type = type) }
                                        types = false
                                    },
                                )
                            }
                        }
                    }
                    TextButton({
                        numeric =
                            "Frequency (10–24000 Hz)" to
                                (f.frequency to { v -> edit { it.copy(frequency = v) } })
                    }) {
                        Text("Frequency · ${frequencyLabel(f.frequency)}")
                    }
                    if (f.type != FilterType.LOW_PASS && f.type != FilterType.HIGH_PASS)
                        TextButton({
                            numeric =
                                "Gain (−12 to +12 dB)" to
                                    (f.gain to { v -> edit { it.copy(gain = v) } })
                        }) {
                            Text("Gain · %+.2f dB".format(f.gain))
                        }
                    TextButton({
                        numeric = "Q (0.1–18)" to (f.q to { v -> edit { it.copy(q = v) } })
                    }) {
                        Text("Q · %.2f".format(f.q))
                    }
                    Row {
                        TextButton({
                            edit { EqFilter(frequency = TEN_BANDS[index.coerceAtMost(9)]) }
                        }) {
                            Text("Reset filter")
                        }
                        TextButton(
                            {
                                change {
                                    it.copy(
                                        filters = it.filters.filterIndexed { i, _ -> i != index }
                                    )
                                }
                                editingFilter = null
                            },
                            enabled = settings.filters.size > 10,
                        ) {
                            Text("Remove filter")
                        }
                    }
                    if (settings.filters.size == 10)
                        Text(
                            "At least ten filter slots are retained; disable a slot to bypass it.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
            }
        }

    if (save)
        PresetNameDialog("Save custom preset", "", { save = false }) {
            vm.savePreset(it)
            save = false
        }
    numeric?.let { (title, pair) ->
        NumberDialog(title, pair.first, { numeric = null }) { value ->
            if (
                title.contains("Frequency") && value !in 10.0..24000.0 ||
                    title.startsWith("Q") && value !in .1..18.0 ||
                    !title.contains("Frequency") && !title.startsWith("Q") && value !in -12.0..12.0
            )
                false
            else {
                pair.second(value)
                numeric = null
                true
            }
        }
    }
    if (presetSheet)
        ModalBottomSheet({ presetSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    TextButton({ importer.launch(arrayOf("application/json", "text/plain")) }) {
                        Text("Import presets")
                    }
                    TextButton({ exporter.launch("hifi-presets.json") }) { Text("Export presets") }
                }
                OutlinedTextField(
                    search,
                    { search = it },
                    label = { Text("Search presets") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("All", "Core", "Genres", "Listening", "Custom", "Favorites", "Recent")
                        .forEach { name ->
                            FilterChip(
                                category == name,
                                { category = name },
                                label = { Text(name) },
                            )
                        }
                }
                val all = BuiltInPresets.all + custom
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(
                        all.filter {
                            it.name.contains(search, true) &&
                                (category == "All" ||
                                    it.category == category ||
                                    category == "Favorites" && it.id in favorites ||
                                    category == "Recent" && it.id in recent)
                        },
                        key = { it.id },
                    ) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text(p.category) },
                            modifier = Modifier,
                            leadingContent = {
                                IconButton({ vm.favoritePreset(p.id) }) {
                                    Icon(
                                        if (p.id in favorites) Icons.Rounded.Star
                                        else Icons.Rounded.StarBorder,
                                        "Favorite ${p.name}",
                                    )
                                }
                            },
                            trailingContent = { TextButton({ preview = p }) { Text("Preview") } },
                        )
                    }
                }
            }
        }
    preview?.let { p ->
        AlertDialog(
            { preview = null },
            title = { Text(p.name) },
            text = {
                Column {
                    ResponseGraph(p.settings, format.rate)
                    Text(p.description, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton({
                            vm.savePreset(p.name + " copy", p.settings)
                            preview = null
                        }) {
                            Text("Duplicate")
                        }
                        if (p.category == "Custom") {
                            TextButton({
                                rename = p
                                preview = null
                            }) {
                                Text("Rename")
                            }
                            TextButton({
                                vm.deletePreset(p.id)
                                preview = null
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({
                    vm.applyPreset(p)
                    preview = null
                    presetSheet = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = { TextButton({ preview = null }) { Text("Cancel") } },
        )
    }
    rename?.let { p ->
        PresetNameDialog("Rename preset", p.name, { rename = null }) {
            vm.renamePreset(p.id, it)
            rename = null
        }
    }
}

@Composable
private fun VerticalGain(
    value: Double,
    enabled: Boolean,
    label: String,
    change: (Double) -> Unit,
    height: Dp = 176.dp,
) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.size(56.dp, height)
            .semantics {
                contentDescription = "$label gain"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), -12f..12f)
                setProgress {
                    if (enabled) {
                        change(it.toDouble().coerceIn(-12.0, 12.0))
                        true
                    } else false
                }
            }
            .pointerInput(enabled) {
                if (enabled)
                    detectDragGestures(
                        onDragStart = { p ->
                            change((12.0 - 24.0 * p.y / size.height).coerceIn(-12.0, 12.0))
                        }
                    ) { c, _ ->
                        c.consume()
                        change(
                            ((12 - 24 * c.position.y / size.height) * 4)
                                .roundToInt()
                                .coerceIn(-48, 48) / 4.0
                        )
                    }
            }
    ) {
        val x = size.width / 2
        val y = ((12 - value) / 24 * size.height).toFloat()
        drawLine(Color.DarkGray, Offset(x, 0f), Offset(x, size.height), 6.dp.toPx())
        drawLine(
            if (enabled) color else Color.Gray,
            Offset(x, size.height / 2),
            Offset(x, y),
            6.dp.toPx(),
        )
        drawCircle(
            if (enabled) color else Color.Gray,
            9.dp.toPx(),
            Offset(x, y.coerceIn(9.dp.toPx(), size.height - 9.dp.toPx())),
        )
    }
}

@Composable
fun ResponseGraph(
    settings: DspSettings,
    rate: Int,
    height: Dp = 192.dp,
    onDrag: ((Int, Double, Double) -> Unit)? = null,
) {
    val filters = settings.activeFilters()
    val ceiling = min(20000.0, rate * .49)
    val curve =
        remember(settings, rate) {
            val coefficients = filters.map { Biquad.design(it, rate) }
            val preamp = effectivePreamp(settings, rate)
            (0..200).map { i ->
                val f = 20.0 * (ceiling / 20).pow(i / 200.0)
                coefficients.sumOf { 20 * log10(it.magnitude(f, rate).coerceAtLeast(1e-12)) } +
                    preamp
            }
        }
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val currentFilters by rememberUpdatedState(filters)
    val currentDrag by rememberUpdatedState(onDrag)
    val labelSize = with(LocalDensity.current) { 10.sp.toPx() }
    val paint =
        remember(labelSize) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = labelSize
                color = android.graphics.Color.rgb(164, 173, 191)
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (settings.enabled) "EQ response · applied preamp included"
            else "EQ bypassed · 0 dB EQ response",
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        Canvas(
            Modifier.fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .semantics {
                    contentDescription =
                        "Calculated equalizer response, logarithmic 20 Hz to ${ceiling.toInt()} Hz, minus 24 to plus 24 dB"
                }
                .pointerInput(settings.mode, rate) {
                    if (settings.mode == EqMode.PARAMETRIC && currentDrag != null) {
                        var selected = 0
                        val left = max(34.dp.toPx(), labelSize * 2.5f)
                        val right = 12.dp.toPx()
                        val top = 16.dp.toPx()
                        val bottom = 26.dp.toPx()
                        detectDragGestures(
                            onDragStart = { p ->
                                val width = (size.width - left - right).coerceAtLeast(1f)
                                selected =
                                    currentFilters.indices.minByOrNull { i ->
                                        abs(
                                            (ln(
                                                    currentFilters[i]
                                                        .frequency
                                                        .coerceIn(20.0, ceiling) / 20
                                                ) / ln(ceiling / 20) * width + left)
                                                .toFloat() - p.x
                                        )
                                    } ?: 0
                            }
                        ) { c, _ ->
                            c.consume()
                            val x =
                                ((c.position.x - left) /
                                        (size.width - left - right).coerceAtLeast(1f))
                                    .coerceIn(0f, 1f)
                            val y =
                                ((c.position.y - top) /
                                        (size.height - top - bottom).coerceAtLeast(1f))
                                    .coerceIn(0f, 1f)
                            currentDrag?.invoke(
                                selected,
                                20.0 * (ceiling / 20).pow(x.toDouble()),
                                if (
                                    currentFilters[selected].type in
                                        listOf(FilterType.LOW_PASS, FilterType.HIGH_PASS)
                                )
                                    0.0
                                else (24 - 48 * y).toDouble().coerceIn(-12.0, 12.0),
                            )
                        }
                    }
                }
        ) {
            val left = max(34.dp.toPx(), labelSize * 2.5f)
            val right = size.width - 12.dp.toPx()
            val top = 16.dp.toPx()
            val bottom = size.height - 26.dp.toPx()
            val width = right - left
            val height = bottom - top
            val grid = Color(0xFF303746)
            for (gain in listOf(24, 12, 0, -12, -24)) {
                val y = top + (24 - gain) / 48f * height
                drawLine(
                    grid.copy(alpha = if (gain == 0) .95f else .5f),
                    Offset(left, y),
                    Offset(right, y),
                    1f,
                )
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(
                    if (gain > 0) "+$gain" else "$gain",
                    left - 6.dp.toPx(),
                    y + labelSize * .35f,
                    paint,
                )
            }
            val ticks = listOf(20.0, 100.0, 1000.0, ceiling)
            ticks.forEachIndexed { i, f ->
                val x = left + (ln(f / 20) / ln(ceiling / 20) * width).toFloat()
                drawLine(grid.copy(alpha = .5f), Offset(x, top), Offset(x, bottom), 1f)
                paint.textAlign =
                    when (i) {
                        0 -> android.graphics.Paint.Align.LEFT
                        ticks.lastIndex -> android.graphics.Paint.Align.RIGHT
                        else -> android.graphics.Paint.Align.CENTER
                    }
                drawContext.canvas.nativeCanvas.drawText(
                    if (f >= 1000) "${"%.2g".format(java.util.Locale.ROOT, f / 1000)}k"
                    else f.toInt().toString(),
                    x,
                    size.height - 8.dp.toPx(),
                    paint,
                )
            }
            val path = Path()
            curve.forEachIndexed { i, g ->
                val x = left + i * width / 200
                val gain = if (settings.enabled) g else 0.0
                val y = top + ((24 - gain.coerceIn(-24.0, 24.0)) / 48 * height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, if (settings.enabled) accent else muted, style = Stroke(2.dp.toPx()))
            if (settings.mode == EqMode.PARAMETRIC)
                filters.forEach { f ->
                    if (f.enabled && f.frequency < ceiling)
                        drawCircle(
                            accent.copy(alpha = if (settings.enabled) 1f else .35f),
                            4.dp.toPx(),
                            Offset(
                                left +
                                    (ln(f.frequency.coerceAtLeast(20.0) / 20) / ln(ceiling / 20) *
                                            width)
                                        .toFloat(),
                                top + ((24 - f.gain) / 48 * height).toFloat(),
                            ),
                        )
                }
        }
    }
}

@Composable
fun NumberDialog(title: String, initial: Double, dismiss: () -> Unit, save: (Double) -> Boolean) {
    var text by remember { mutableStateOf(initial.toString()) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        dismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    text,
                    {
                        text = it
                        error = false
                    },
                    singleLine = true,
                    isError = error,
                )
                if (error) Text("Enter a finite value in the displayed range.")
            }
        },
        confirmButton = {
            TextButton({
                val value = text.toDoubleOrNull()
                error = value == null || !value.isFinite() || !save(value)
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                if (!title.contains("Frequency") && !title.startsWith("Q"))
                    TextButton({ text = "0.0" }) { Text("Zero gain") }
                TextButton(dismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun PresetNameDialog(title: String, initial: String, dismiss: () -> Unit, save: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        dismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { name = it.take(80) },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton({ save(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } },
    )
}

fun frequencyLabel(f: Double) =
    if (f >= 1000) "${"%.2g".format(f/1000)}k" else "${"%.3g".format(f)} Hz"
