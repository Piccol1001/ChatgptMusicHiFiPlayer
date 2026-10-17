package com.piccol.hifi.audio

import android.content.Context
import android.media.*
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow

enum class OutputCategory(val label: String) {
    SPEAKER("Phone speaker"),
    WIRED("Wired headphones"),
    BLUETOOTH("Bluetooth"),
    USB("USB DAC"),
    HDMI("HDMI"),
    OTHER("Other"),
}

data class OutputDevice(
    val id: Int,
    val name: String,
    val category: OutputCategory,
    val rates: List<Int>,
    val channels: List<Int>,
    val encodings: List<Int>,
)

data class OutputRoute(
    val device: OutputDevice? = null,
    val clientRate: Int? = null,
    val clientChannels: Int? = null,
    val clientEncoding: Int? = null,
)

fun outputCategory(type: Int) =
    when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> OutputCategory.SPEAKER
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG -> OutputCategory.WIRED
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        26,
        27,
        30 -> OutputCategory.BLUETOOTH
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> OutputCategory.USB
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        29 -> OutputCategory.HDMI
        else -> OutputCategory.OTHER
    }

fun AudioDeviceInfo.snapshot() =
    OutputDevice(
        id,
        productName.toString(),
        outputCategory(type),
        sampleRates.toList(),
        channelCounts.toList(),
        encodings.toList(),
    )

/** Reports AudioTrack.routedDevice, never infers the active route from the device inventory. */
class OutputMonitor(context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    val available = MutableStateFlow<List<OutputDevice>>(emptyList())
    val route = MutableStateFlow(OutputRoute())
    @Volatile
    var createdTrackCount = 0L
        private set

    private var track = WeakReference<AudioTrack>(null)

    private fun inventory() {
        available.value = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.snapshot() }
        refresh()
    }

    private val callback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(devices: Array<out AudioDeviceInfo>) {
                inventory()
            }

            override fun onAudioDevicesRemoved(devices: Array<out AudioDeviceInfo>) {
                inventory()
            }
        }

    init {
        manager.registerAudioDeviceCallback(callback, handler)
        inventory()
    }

    fun attach(value: AudioTrack) {
        createdTrackCount++
        handler.post {
            runCatching { track.get()?.removeOnRoutingChangedListener(listener) }
            track = WeakReference(value)
            runCatching { value.addOnRoutingChangedListener(listener, handler) }
            refresh()
        }
    }

    private val listener = AudioRouting.OnRoutingChangedListener { refresh() }

    fun refresh() {
        val t = track.get()
        route.value =
            runCatching {
                    if (t == null || t.state != AudioTrack.STATE_INITIALIZED) OutputRoute()
                    else
                        OutputRoute(
                            t.routedDevice?.snapshot(),
                            t.sampleRate,
                            t.channelCount,
                            t.audioFormat,
                        )
                }
                .getOrDefault(OutputRoute())
    }

    fun clear() {
        handler.post {
            runCatching { track.get()?.removeOnRoutingChangedListener(listener) }
            track.clear()
            route.value = OutputRoute()
        }
    }
}
