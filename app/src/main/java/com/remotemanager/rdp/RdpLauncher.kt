package com.remotemanager.rdp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.remotemanager.R
import com.remotemanager.data.model.Server
import java.io.File
import java.net.URLEncoder

fun launchRdp(context: Context, server: Server) {
    if (server.type != com.remotemanager.data.model.ConnectionType.RDP) return

    // Try URI scheme first
    val uri = buildRdpUri(server)
    val uriIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (uriIntent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(uriIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to file sharing
        }
    }

    // Fallback: generate a temporary .rdp file and open it
    try {
        val rdpFile = generateRdpFile(context, server)
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            rdpFile
        )
        val fileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/x-rdp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fileIntent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.rdp_not_installed),
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun buildRdpUri(server: Server): Uri {
    val params = mutableListOf<String>()
    val address = "${server.host}:${server.port}"
    params.add("full%20address=s:${encodeRdpValue(address)}")
    if (server.username.isNotBlank()) {
        params.add("username=s:${encodeRdpValue(server.username)}")
    }
    if (!server.password.isNullOrBlank()) {
        params.add("password=s:${encodeRdpValue(server.password)}")
    }
    server.rdpWidth?.let { params.add("desktopwidth=i:$it") }
    server.rdpHeight?.let { params.add("desktopheight=i:$it") }
    params.add("session%20bpp=i:${server.rdpColorDepth}")
    params.add("use%20multimon=i:0")
    params.add("screen%20mode%20id=i:1") // windowed by default
    params.add("enablecredsspsupport=i:${if (server.useNla) 1 else 0}")

    val uriString = "rdp://${params.joinToString("&")}"
    return Uri.parse(uriString)
}

private fun encodeRdpValue(value: String): String {
    return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

private fun generateRdpFile(context: Context, server: Server): File {
    val content = buildString {
        appendLine("screen mode id:i:1")
        appendLine("use multimon:i:0")
        appendLine("full address:s:${server.host}:${server.port}")
        appendLine("username:s:${server.username}")
        if (!server.password.isNullOrBlank()) {
            appendLine("password 51:b:${server.password}")
        }
        server.rdpWidth?.let { appendLine("desktopwidth:i:$it") }
        server.rdpHeight?.let { appendLine("desktopheight:i:$it") }
        appendLine("session bpp:i:${server.rdpColorDepth}")
        appendLine("authentication level:i:2")
        appendLine("negotiate security layer:i:1")
        appendLine("enablecredsspsupport:i:${if (server.useNla) 1 else 0}")
        appendLine("redirectclipboard:i:1")
        appendLine("redirectprinters:i:0")
        appendLine("redirectsmartcards:i:0")
        appendLine("redirectcomports:i:0")
        appendLine("redirectdrives:i:0")
        appendLine("disable wallpaper:i:0")
        appendLine("disable full window drag:i:0")
        appendLine("disable menu anims:i:1")
        appendLine("disable themes:i:0")
        appendLine("disable cursor setting:i:0")
        appendLine("bitmapcachepersistenable:i:1")
    }

    val cacheDir = File(context.cacheDir, "rdp")
    cacheDir.mkdirs()
    val safeName = server.name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
    val file = File(cacheDir, "${safeName}.rdp")
    file.writeText(content)
    return file
}
