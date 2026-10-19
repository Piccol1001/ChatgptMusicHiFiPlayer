package com.piccol.hifi.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.piccol.hifi.visualization.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlin.math.*

private val visualNames=listOf("Spectrum","Oscilloscope","Stereo Meter","Spectrogram","Circular","Reactive")
@OptIn(ExperimentalMaterial3Api::class) @UnstableApi @Composable
fun VisualizerScreen(vm:PlayerViewModel,fullScreen:Boolean,optionsRequest:Int=0,browse:()->Unit={},full:(Boolean)->Unit){
    val settings by vm.visualSettings.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()
    val analysis=VisualSettings(fftSize=settings.fftSize,bars=settings.bars,fps=settings.fps,logarithmic=settings.logarithmic,smoothing=settings.smoothing,holdMs=settings.holdMs,decay=settings.decay,timeMs=settings.timeMs,quality=settings.quality)
    val frames=remember(vm,analysis){vm.visualization.frames(analysis).flowOn(Dispatchers.Default)}
    val frame by frames.collectAsStateWithLifecycle(initialValue=VisualFrame())
    var options by remember{mutableStateOf(false)}
    var overlay by remember(fullScreen,playback.item?.mediaId){mutableStateOf(true)}
    var revealControls by remember { mutableIntStateOf(0) }
    var modeMenu by remember { mutableStateOf(false) }
    LaunchedEffect(optionsRequest){if(optionsRequest>0)options=true}
    LaunchedEffect(fullScreen,playback.item?.mediaId,revealControls,options,modeMenu){overlay=true;if(fullScreen&&!options&&!modeMenu){delay(3000);overlay=false}}
    val context=LocalContext.current
    BackHandler(fullScreen){full(false)}
    DisposableEffect(fullScreen){val window=(context as? Activity)?.window;val controller=window?.let{WindowCompat.getInsetsController(it,it.decorView)}
        if(fullScreen){controller?.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;controller?.hide(WindowInsetsCompat.Type.systemBars())}
        onDispose{controller?.show(WindowInsetsCompat.Type.systemBars())}}
    val cover=playback.item?.mediaMetadata?.artworkUri?.toString()
    val plot:@Composable (Modifier)->Unit={modifier->
        Box(modifier.clip(MaterialTheme.shapes.large).background(Color.Black)) {
            if(playback.playing)VisualDisplay(settings,frame,cover,Modifier.fillMaxSize())
            else MusicEmptyState("Play music to see your sound","Real audio analysis appears here during playback.","Browse Library",browse,Icons.Rounded.GraphicEq,Modifier.align(Alignment.Center))
            if(playback.playing&&overlay)Surface(color=Color.Black.copy(alpha=.65f),shape=MaterialTheme.shapes.small,modifier=Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Column(Modifier.padding(horizontal=10.dp,vertical=8.dp).widthIn(max=240.dp)) {
                    Text(playback.item?.mediaMetadata?.title?.toString().orEmpty(),style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=Color.White)
                    Text(playback.item?.mediaMetadata?.artist?.toString().orEmpty(),style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=Color(0xFFA4ADBF))
                }
            }
        }
    }
    if(fullScreen)Box(Modifier.fillMaxSize().background(Color.Black)
        .clickable { revealControls++ }.semantics { contentDescription="Show visualizer controls" }) {
        if(playback.playing)plot(Modifier.fillMaxSize()) else MusicEmptyState("Play music to see your sound","Choose local music to see its audio.","Browse Library",browse,Icons.Rounded.GraphicEq,Modifier.align(Alignment.Center))
        if(overlay) Row(Modifier.align(Alignment.TopStart).displayCutoutPadding().padding(16.dp)
            .clip(MaterialTheme.shapes.small).background(Color.Black.copy(alpha=.65f))) {
            Box {
                IconButton({modeMenu=true;revealControls++}) { Icon(Icons.Rounded.GraphicEq,"Visualization mode") }
                DropdownMenu(modeMenu,{modeMenu=false}) {
                    VisualMode.entries.forEach { mode -> DropdownMenuItem(text={Text(visualNames[mode.ordinal])},onClick={vm.changeVisual{it.copy(mode=mode)};modeMenu=false;revealControls++}) }
                }
            }
            IconButton({vm.changeVisual{it.copy(theme=(it.theme+1)%3)};revealControls++}) { Icon(Icons.Rounded.Palette,"Visualizer color theme") }
            IconButton({options=true;revealControls++}) { Icon(Icons.Rounded.Tune,"Visualizer settings") }
        }
        FilledTonalIconButton({modeMenu=false;full(false)},Modifier.align(Alignment.TopEnd).displayCutoutPadding().padding(16.dp)){Icon(Icons.Rounded.FullscreenExit,"Exit fullscreen")}
    }else BoxWithConstraints(Modifier.fillMaxSize()) {
        val displayHeight=(maxHeight-136.dp).coerceIn(120.dp,900.dp)
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                if(playback.playing)plot(Modifier.fillMaxWidth().height(displayHeight))
                else Surface(color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.large) {
                    MusicEmptyState("Play music to see your sound","Six views of your music. No microphone, no simulated signal.","Browse Library",browse,Icons.Rounded.GraphicEq)
                }
            }
            item {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(VisualMode.entries){mode->CompactTab(visualNames[mode.ordinal],settings.mode==mode,{vm.changeVisual{it.copy(mode=mode)}},
                        icon=when(mode){VisualMode.SPECTRUM->Icons.Rounded.BarChart;VisualMode.SCOPE->Icons.Rounded.Waves;VisualMode.METERS->Icons.Rounded.GraphicEq;VisualMode.SPECTROGRAM->Icons.Rounded.Gradient;VisualMode.CIRCULAR->Icons.Rounded.DonutLarge;VisualMode.REACTIVE->Icons.Rounded.AutoAwesome},
                        modifier=Modifier.semantics{contentDescription=visualNames[mode.ordinal]})}
                }
            }
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    TextButton({full(true)}){Icon(Icons.Rounded.Fullscreen,null,Modifier.size(18.dp));Text("Full Screen")}
                    IconButton({options=true}){Icon(Icons.Rounded.Tune,"Visualizer settings")}
                    TextButton({vm.changeVisual{it.copy(theme=(it.theme+1)%3)}}){Icon(Icons.Rounded.Palette,null,Modifier.size(18.dp));Text("Color Theme")}
                }
            }
            if(settings.mode==VisualMode.REACTIVE)item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Flow","Orbit","Prism").forEachIndexed{i,name->CompactTab(name,settings.effect==i,{vm.changeVisual{it.copy(effect=i)}})}}}
        }
    }
    if(options)ModalBottomSheet({options=false}){LazyColumn(Modifier.heightIn(max=600.dp),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("Visualization",style=MaterialTheme.typography.titleLarge)}
        item{var details by remember{mutableStateOf(false)};TextButton({details=!details}){Text("Technical details")};if(details)Text("Post-DSP PCM before Android volume/routing. Analysis suspends when hidden or paused; multichannel displays monitor the front pair. FFT ${settings.fftSize} · target ${settings.fps} FPS. RMS smoothing 300 ms; digital peak is not true peak or acoustic SPL. Reactive effects are original ES 2.0 shaders, not projectM.",style=MaterialTheme.typography.bodySmall)}
        item{ChoiceRow("Target FPS",listOf(30,60),settings.fps,{it.toString()}){v->vm.changeVisual{it.copy(fps=v)}}}
        item{ChoiceRow("Quality",VisualQuality.entries.toList(),settings.quality,{it.name}){v->vm.changeVisual{it.copy(quality=v)}}}
        item{ChoiceRow("FFT size",listOf(512,1024,2048,4096,8192),settings.fftSize,{it.toString()}){v->vm.changeVisual{it.copy(fftSize=v)}}}
        item{ChoiceRow("Bars",listOf(16,32,64,128),settings.bars,{it.toString()}){v->vm.changeVisual{it.copy(bars=v)}}}
        item{SwitchRow("Logarithmic frequency axis",settings.logarithmic){v->vm.changeVisual{it.copy(logarithmic=v)}}}
        item{Text("Smoothing %.2f".format(settings.smoothing));Slider(settings.smoothing,{v->vm.changeVisual{it.copy(smoothing=v)}},valueRange=0f.. .95f)}
        item{Text("Peak hold ${settings.holdMs} ms");Slider(settings.holdMs.toFloat(),{v->vm.changeVisual{it.copy(holdMs=v.toInt())}},valueRange=0f..2000f)}
        item{Text("Peak decay ${settings.decay.toInt()} dB/s");Slider(settings.decay,{v->vm.changeVisual{it.copy(decay=v)}},valueRange=3f..96f)}
        item{ChoiceRow("Color",listOf(0,1,2),settings.theme,{listOf("Warm white","Amber","Mint")[it]}){v->vm.changeVisual{it.copy(theme=v)}}}
        item{SwitchRow("Stereo waveform",settings.stereo){v->vm.changeVisual{it.copy(stereo=v)}}}
        item{Text("Waveform window ${settings.timeMs} ms");Slider(settings.timeMs.toFloat(),{v->vm.changeVisual{it.copy(timeMs=v.toInt())}},valueRange=5f..150f)}
        item{Text("Waveform scale %.1f×".format(settings.amplitude));Slider(settings.amplitude,{v->vm.changeVisual{it.copy(amplitude=v)}},valueRange=.5f..8f)}
        item{SwitchRow("Vertical stereo meters",settings.verticalMeters){v->vm.changeVisual{it.copy(verticalMeters=v)}}}
        item{Text("Spectrogram history ${settings.history} frames");Slider(settings.history.toFloat(),{v->vm.changeVisual{it.copy(history=v.toInt())}},valueRange=60f..240f)}
        item{SwitchRow("Reduce decorative motion",settings.reducedMotion){v->vm.changeVisual{it.copy(reducedMotion=v)}}}
        item{SwitchRow("Circular rotation",settings.rotation){v->vm.changeVisual{it.copy(rotation=v)}}}
        item{SwitchRow("Circular album artwork",settings.artwork){v->vm.changeVisual{it.copy(artwork=v)}}}
        item{TextButton({vm.changeVisual{VisualSettings()}}){Text("Reset visualization settings")}}
    }}
}
@Composable private fun <T> ChoiceRow(label:String,choices:List<T>,value:T,text:(T)->String,choose:(T)->Unit){Column{Text(label,style=MaterialTheme.typography.titleSmall);Row(Modifier.horizontalScroll(rememberScrollState())){choices.forEach{v->FilterChip(value==v,{choose(v)},label={Text(text(v))})}}}}
@Composable private fun SwitchRow(label:String,value:Boolean,change:(Boolean)->Unit){Row(verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(value,change)}}

@Composable private fun VisualDisplay(s:VisualSettings,f:VisualFrame,artwork:String?,modifier:Modifier){
    val color=when(s.theme){1->Color(0xFFFFB45B);2->Color(0xFF7BE0C3);else->Color(0xFFF4ECDC)}
    val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=android.graphics.Color.LTGRAY;textSize=26f}}
    if(s.mode==VisualMode.REACTIVE){ReactiveLayer(reactiveUniforms(f,s),s.quality,modifier);return}
    val spectrogram=remember(s.bars,s.history,s.theme){SpectrogramImage(s.history,s.bars,s.theme)}
    SideEffect{if(s.mode==VisualMode.SPECTROGRAM)spectrogram.push(f)}
    Box(modifier.background(Color.Black)){
        Canvas(Modifier.fillMaxSize()){
            val grid=Color(0xFF263248)
            fun grid(){for(i in 1..3)drawLine(grid,Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4),1f)}
            when(s.mode){
                VisualMode.SPECTRUM->{grid();val count=f.spectrum.size
                    for(i in 0 until count){val x=i*size.width/count;val width=size.width/count*.72f;val height=((f.spectrum[i]+96)/96).coerceIn(0f,1f)*(size.height-32)
                        drawRect(color,Offset(x,size.height-24-height),Size(width,height));val y=size.height-24-((f.peaks[i]+96)/96).coerceIn(0f,1f)*(size.height-32);drawLine(color.copy(alpha=.6f),Offset(x,y),Offset(x+width,y),2f)}
                    drawContext.canvas.nativeCanvas.drawText("0 dBFS / bin",8f,28f,paint)
                    val end=min(20000,f.rate/2);drawContext.canvas.nativeCanvas.drawText(if(s.logarithmic)"20      100        1k        ${end/1000}k Hz"else "0         ${end/2000}k         ${end/1000}k Hz",8f,size.height-3,paint)
                }
                VisualMode.SCOPE->{grid();fun trace(samples:FloatArray,center:Float,tint:Color){if(samples.isEmpty())return;val path=Path();samples.forEachIndexed{i,v->val x=i*size.width/(samples.size-1).coerceAtLeast(1);val y=center-v*s.amplitude*size.height/(if(s.stereo)4 else 2);if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};drawPath(path,tint,style=Stroke(2.dp.toPx()))}
                    if(s.stereo){trace(f.waveL,size.height*.25f,color);trace(f.waveR,size.height*.75f,Color(0xFF58A6FF))}else trace(FloatArray(f.waveL.size){(f.waveL[it]+f.waveR[it])/2},size.height*.5f,color)
                    drawContext.canvas.nativeCanvas.drawText("${min(s.timeMs.toDouble(),16383.0/f.rate*1000).toInt()} ms · ${if(s.stereo)"L / R"else "Mono"}",8f,28f,paint)
                }
                VisualMode.METERS->{grid();val rms=floatArrayOf(f.rmsL,f.rmsR);val peak=floatArrayOf(f.peakL,f.peakR)
                    for(i in 0..1){val value=((rms[i]+96)/96).coerceIn(0f,1f);val held=((peak[i]+96)/96).coerceIn(0f,1f)
                        if(s.verticalMeters){val x=size.width*(.15f+i*.45f);drawRect(color,Offset(x,size.height*(1-value)),Size(size.width*.25f,size.height*value));drawLine(Color.Red,Offset(x,size.height*(1-held)),Offset(x+size.width*.25f,size.height*(1-held)),4f)}
                        else{val y=size.height*(.25f+i*.4f);drawRect(color,Offset(0f,y),Size(size.width*value,28.dp.toPx()));drawLine(Color.Red,Offset(size.width*held,y),Offset(size.width*held,y+28.dp.toPx()),4f)}
                        drawContext.canvas.nativeCanvas.drawText("${if(i==0)"L"else "R"}: RMS %.1f  Peak %.1f dBFS".format(rms[i],peak[i]),8f,size.height*(.18f+i*.4f),paint)
                    }
                }
                VisualMode.SPECTROGRAM->{val image=spectrogram.bitmap.asImageBitmap();val cursor=spectrogram.cursor;val first=s.history-cursor
                    if(first>0)drawImage(image,IntOffset(cursor,0),IntSize(first,s.bars),IntOffset.Zero,IntSize((size.width*first/s.history).toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)))
                    if(cursor>0)drawImage(image,IntOffset.Zero,IntSize(cursor,s.bars),IntOffset((size.width*first/s.history).toInt(),0),IntSize((size.width*cursor/s.history).toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)))
                    drawContext.canvas.nativeCanvas.drawText("${min(20000,f.rate/2)/1000}k Hz",8f,28f,paint);drawContext.canvas.nativeCanvas.drawText("20 Hz · time →",8f,size.height-8,paint)
                }
                VisualMode.CIRCULAR->{val radius=min(size.width,size.height)*.23f;drawCircle(color.copy(alpha=.15f),radius,center,style=Stroke(2.dp.toPx()))
                    f.spectrum.forEachIndexed{i,db->val angle=2*PI*i/f.spectrum.size+(if(s.rotation&&!s.reducedMotion)f.seconds*.12 else 0.0);val length=((db+96)/96).coerceIn(0f,1f)*radius
                        val unit=Offset(cos(angle).toFloat(),sin(angle).toFloat());drawLine(color,center+unit*radius,center+unit*(radius+length),3.dp.toPx())}
                }
                else->Unit
            }
        }
        if(s.mode==VisualMode.CIRCULAR&&s.artwork&&artwork!=null)AsyncImage(artwork,"Album artwork",modifier=Modifier.align(Alignment.Center).size(96.dp).clip(CircleShape),contentScale=androidx.compose.ui.layout.ContentScale.Crop)
    }
}
private class SpectrogramImage(private val history:Int,private val bands:Int,private val theme:Int){
    val bitmap=Bitmap.createBitmap(history,bands,Bitmap.Config.ARGB_8888).apply{eraseColor(android.graphics.Color.BLACK)}
    private val column=IntArray(bands);var cursor=0;private set
    private var sequence=Long.MIN_VALUE;private var generation=-1
    fun push(frame:VisualFrame){if(!frame.playing||frame.spectrum.size!=bands||frame.sequence==sequence)return
        if(frame.generation!=generation){bitmap.eraseColor(android.graphics.Color.BLACK);cursor=0;generation=frame.generation}
        sequence=frame.sequence
        for(i in 0 until bands){val v=((frame.spectrum[i]+96)/96).coerceIn(0f,1f);val hue=when(theme){1->35f*v;2->160f-v*80;else->240f-v*240};column[bands-1-i]=android.graphics.Color.HSVToColor(floatArrayOf(hue,.8f,v))}
        bitmap.setPixels(column,0,1,cursor,0,1,bands);cursor=(cursor+1)%history
    }
}
@Composable private fun ReactiveLayer(uniforms:ReactiveUniforms,quality:VisualQuality,modifier:Modifier){
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    var error by remember{mutableStateOf<String?>(null)}
    val view=remember(context){ReactiveSurface(context){error=it}}
    DisposableEffect(view,owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE)view.onPause()else if(event==Lifecycle.Event.ON_RESUME)view.onResume()};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer);view.onPause()}}
    Box(modifier){AndroidView(factory={view},modifier=Modifier.fillMaxSize(),update={it.uniforms=uniforms;it.quality(quality);it.requestRender()});error?.let{Text(it,Modifier.align(Alignment.Center).padding(24.dp),color=Color.White)}}
}
