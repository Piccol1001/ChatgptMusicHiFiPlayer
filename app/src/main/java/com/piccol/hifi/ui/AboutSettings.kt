package com.piccol.hifi.ui

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.PackageInfoCompat

@Composable
fun AboutSettings() {
    val context = LocalContext.current
    val installed = remember(context) { installedAppVersion(context) }
    Text("App Version", style = MaterialTheme.typography.titleMedium)
    Text(installed)
    Text(
        "Native Android · Media3 playback\nOutput quality depends on the decoder, Android mixer and connected device. Bit-perfect output is not verified.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Suppress("DEPRECATION")
internal fun installedAppVersion(context: Context): String {
    val installed = context.packageManager.getPackageInfo(context.packageName, 0)
    return "${installed.versionName} (code ${PackageInfoCompat.getLongVersionCode(installed)})"
}
