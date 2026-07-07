package com.remotemanager.ssh

import androidx.compose.ui.text.AnnotatedString

class TerminalEmulator {

    private val buffer = StringBuilder()
    private val pendingEscape = StringBuilder()
    private var inEscape = false

    val text: AnnotatedString
        get() = AnnotatedString(buffer.toString())

    fun append(data: String) {
        for (char in data) {
            when {
                char == '\u001B' -> {
                    inEscape = true
                    pendingEscape.clear()
                    pendingEscape.append(char)
                }
                inEscape -> {
                    pendingEscape.append(char)
                    if (char.isLetter() || char == '~') {
                        // End of escape sequence; ignore it for now
                        inEscape = false
                        pendingEscape.clear()
                    } else if (pendingEscape.length > 32) {
                        // Safety limit
                        inEscape = false
                        pendingEscape.clear()
                    }
                }
                char == '\r' -> {
                    // Ignore carriage returns
                }
                char == '\u0007' -> {
                    // Bell, ignore
                }
                char == '\b' -> {
                    if (buffer.isNotEmpty()) {
                        buffer.deleteCharAt(buffer.length - 1)
                    }
                }
                char.isISOControl() && char != '\n' && char != '\t' -> {
                    // Ignore other control characters
                }
                else -> {
                    buffer.append(char)
                }
            }
        }

        // Keep buffer from growing too large
        if (buffer.length > MAX_BUFFER_SIZE) {
            buffer.delete(0, buffer.length - MAX_BUFFER_SIZE)
        }
    }

    fun clear() {
        buffer.clear()
        pendingEscape.clear()
        inEscape = false
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 50000
    }
}
