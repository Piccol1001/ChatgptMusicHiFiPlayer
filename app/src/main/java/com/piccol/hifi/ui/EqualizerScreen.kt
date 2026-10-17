package com.piccol.hifi.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
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
fun EqualizerScreen(vm: PlayerViewModel, profiles: () -> Unit = {}) {
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
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(profiles) { Text("AutoEq & output profiles") }
            profileState.active(route)?.let { p ->
                Text(
                    "Editing active profile: ${p.name}",
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(vm::useBaseEq) { Text("Return to personal EQ") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(settings.preset, style = MaterialTheme.typography.headlineSmall)
                    Text("App audio · PCM processing", style = MaterialTheme.typography.labelSmall)
                }
                Switch(settings.enabled, { vm.changeDsp { s -> s.copy(enabled = it) } })
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EqMode.entries.forEach { mode ->
                    FilterChip(
                        settings.mode == mode,
                        { vm.changeDsp { it.copy(mode = mode) } },
                        label = {
                            Text(
                                when (mode) {
                                    EqMode.TEN -> "10 bands"
                                    EqMode.PARAMETRIC -> "Parametric"
                                    EqMode.THIRTY_ONE -> "31 bands"
                                }
                            )
                        },
                    )
                }
            }
        }
        item {
            ResponseGraph(settings, format.rate) { index, f, g ->
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ presetSheet = true }) { Text("Presets") }
                TextButton({ save = true }) { Text("Save custom") }
                TextButton({
                    change {
                        it.copy(
                            ten = List(10) { 0.0 },
                            thirtyOne = List(31) { 0.0 },
                            filters = TEN_BANDS.map { f -> EqFilter(frequency = f) },
                            preamp = 0.0,
                        )
                    }
                }) {
                    Text("Reset all")
                }
            }
        }
        item {
            Text(
                "Preamp · ${"%.1f".format(settings.preamp)} dB",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                settings.preamp.toFloat(),
                { value -> change { it.copy(preamp = (value * 4).roundToInt() / 4.0) } },
                valueRange = -24f..12f,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Automatic headroom", Modifier.weight(1f))
                Switch(
                    settings.autoHeadroom,
                    { enabled -> change { it.copy(autoHeadroom = enabled) } },
                )
            }
            val headroom =
                remember(settings, format.rate) { effectivePreamp(settings, format.rate) }
            Text(
                "Applied EQ preamp: ${"%.1f".format(headroom)} dB when enabled. Headroom estimate includes the combined response and a 0.5 dB safety margin.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton({ change { it.copy(preamp = 0.0) } }) { Text("Reset preamp") }
        }
        if (settings.mode != EqMode.PARAMETRIC)
            item {
                val frequencies = if (settings.mode == EqMode.TEN) TEN_BANDS else THIRD_OCTAVE
                val gains = if (settings.mode == EqMode.TEN) settings.ten else settings.thirtyOne
                Text(
                    "Scroll bands · tap a gain for exact entry",
                    style = MaterialTheme.typography.labelSmall,
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            Modifier.width(64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TextButton({ numeric = frequencyLabel(f) to (gains[i] to update) }) {
                                Text("%+.1f".format(gains[i]))
                            }
                            VerticalGain(gains[i], f < format.rate * .5, frequencyLabel(f), update)
                            Text(frequencyLabel(f), style = MaterialTheme.typography.labelSmall)
                            IconButton({ update(0.0) }) {
                                Icon(Icons.Rounded.RestartAlt, "Reset ${frequencyLabel(f)}")
                            }
                        }
                    }
                }
                Text(
                    "Bands at or above Nyquist are bypassed. 10/31-band preset mapping interpolates control gains; the resulting response can differ because bandwidths differ.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        if (settings.mode == EqMode.PARAMETRIC)
            items(settings.filters.size) { i ->
                val f = settings.filters[i]
                fun edit(transform: (EqFilter) -> EqFilter) {
                    change { s ->
                        s.copy(
                            filters =
                                s.filters.mapIndexed { j, old ->
                                    if (j == i) transform(old) else old
                                }
                        )
                    }
                }
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filter ${i+1}", Modifier.weight(1f))
                            Switch(f.enabled, { enabled -> edit { it.copy(enabled = enabled) } })
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            FilterType.entries.forEach { type ->
                                FilterChip(
                                    f.type == type,
                                    { edit { it.copy(type = type) } },
                                    label = { Text(type.name.replace('_', ' ')) },
                                )
                            }
                        }
                        Row {
                            TextButton({
                                numeric =
                                    "Frequency (10–24000 Hz)" to
                                        (f.frequency to
                                            { value: Double ->
                                                if (value in 10.0..24000.0)
                                                    edit { it.copy(frequency = value) }
                                            })
                            }) {
                                Text(frequencyLabel(f.frequency))
                            }
                            if (f.type != FilterType.LOW_PASS && f.type != FilterType.HIGH_PASS)
                                TextButton({
                                    numeric =
                                        "Gain (−12 to +12 dB)" to
                                            (f.gain to
                                                { value: Double ->
                                                    if (value in -12.0..12.0)
                                                        edit { it.copy(gain = value) }
                                                })
                                }) {
                                    Text("%+.2f dB".format(f.gain))
                                }
                            TextButton({
                                numeric =
                                    "Q (0.1–18)" to
                                        (f.q to
                                            { value: Double ->
                                                if (value in .1..18.0) edit { it.copy(q = value) }
                                            })
                            }) {
                                Text("Q %.2f".format(f.q))
                            }
                        }
                        if (f.type == FilterType.LOW_PASS || f.type == FilterType.HIGH_PASS)
                            Text(
                                "Pass filters use cutoff and Q; gain does not apply.",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        TextButton({
                            edit { EqFilter(frequency = TEN_BANDS[i.coerceAtMost(9)]) }
                        }) {
                            Text("Reset filter")
                        }
                    }
                }
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
                else "Current ReplayGain adjustment: %.2f dB".format(replayGain(settings, tags)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Reads ID3 TXXX and FLAC/Vorbis comments. R128/decoder gain is not reapplied. No dynamic compressor or limiter is added.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text(
                "DSP clips detected this service session: ${clips}. No bit-perfect output claim. Processing uses 16-bit PCM after Media3 conversion; float/offload bypass is disabled.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Row {
                TextButton({ importer.launch(arrayOf("application/json", "text/plain")) }) {
                    Text("Import presets")
                }
                TextButton({ exporter.launch("hifi-presets.json") }) { Text("Export presets") }
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
private fun VerticalGain(value: Double, enabled: Boolean, label: String, change: (Double) -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.size(64.dp, 200.dp)
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
    onDrag: ((Int, Double, Double) -> Unit)? = null,
) {
    val filters = settings.activeFilters()
    val ceiling = min(20000.0, rate * .49)
    val coefficients = remember(settings, rate) { filters.map { Biquad.design(it, rate) } }
    val curve =
        remember(settings, rate) {
            val preamp = effectivePreamp(settings, rate)
            (0..200).map { i ->
                val f = 20.0 * (ceiling / 20).pow(i / 200.0)
                coefficients.sumOf { 20 * log10(it.magnitude(f, rate).coerceAtLeast(1e-12)) } +
                    preamp
            }
        }
    val color = MaterialTheme.colorScheme.primary
    val currentFilters by rememberUpdatedState(filters)
    val currentDrag by rememberUpdatedState(onDrag)
    Column {
        Text(
            "Configured response · ${rate/1000.0} kHz · ${if(settings.enabled)"EQ enabled" else "EQ bypassed"}",
            style = MaterialTheme.typography.labelSmall,
        )
        Canvas(
            Modifier.fillMaxWidth()
                .height(180.dp)
                .semantics {
                    contentDescription =
                        "Calculated equalizer response, logarithmic 20 Hz to ${ceiling.toInt()} Hz, minus 24 to plus 24 dB"
                }
                .pointerInput(settings.mode, rate) {
                    if (settings.mode == EqMode.PARAMETRIC && currentDrag != null) {
                        var selected = 0
                        detectDragGestures(
                            onDragStart = { p ->
                                selected =
                                    currentFilters.indices.minByOrNull { i ->
                                        abs(
                                            (ln(
                                                    currentFilters[i]
                                                        .frequency
                                                        .coerceIn(20.0, ceiling) / 20
                                                ) / ln(ceiling / 20) * size.width)
                                                .toFloat() - p.x
                                        )
                                    } ?: 0
                            }
                        ) { c, _ ->
                            c.consume()
                            currentDrag?.invoke(
                                selected,
                                20.0 *
                                    (ceiling / 20).pow(
                                        (c.position.x / size.width).coerceIn(0f, 1f).toDouble()
                                    ),
                                if (
                                    currentFilters[selected].type in
                                        listOf(FilterType.LOW_PASS, FilterType.HIGH_PASS)
                                )
                                    0.0
                                else
                                    (24 - 48 * c.position.y / size.height)
                                        .toDouble()
                                        .coerceIn(-12.0, 12.0),
                            )
                        }
                    }
                }
        ) {
            for (i in 0..4) {
                val y = i * size.height / 4
                drawLine(Color.DarkGray, Offset(0f, y), Offset(size.width, y), 1f)
            }
            for (f in listOf(20.0, 100.0, 1000.0, 10000.0)) {
                if (f < ceiling) {
                    val x = (ln(f / 20) / ln(ceiling / 20) * size.width).toFloat()
                    drawLine(Color.DarkGray, Offset(x, 0f), Offset(x, size.height), 1f)
                }
            }
            val path = Path()
            curve.forEachIndexed { i, g ->
                val x = i * size.width / 200
                val y = ((24 - g.coerceIn(-24.0, 24.0)) / 48 * size.height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
            )
            if (settings.mode == EqMode.PARAMETRIC)
                filters.forEach { f ->
                    if (f.enabled && f.frequency < ceiling)
                        drawCircle(
                            color,
                            5.dp.toPx(),
                            Offset(
                                (ln(f.frequency.coerceAtLeast(20.0) / 20) / ln(ceiling / 20) *
                                        size.width)
                                    .toFloat(),
                                ((24 - f.gain) / 48 * size.height).toFloat(),
                            ),
                        )
                }
        }
        Text(
            "20 Hz                     1 kHz                     ${ceiling.toInt()/1000} kHz   •   ±24 dB",
            style = MaterialTheme.typography.labelSmall,
        )
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
        dismissButton = { TextButton(dismiss) { Text("Cancel") } },
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
