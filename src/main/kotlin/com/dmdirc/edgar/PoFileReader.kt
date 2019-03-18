package com.dmdirc.edgar

import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path

internal class PoFileReader(path: Path) : Closeable {

    private val reader = Files.newBufferedReader(path)
    private var currentType: LineType? = null
    private val currentId = StringBuilder()
    private val currentString = StringBuilder()

    private val entries = mutableMapOf<String, String>()

    fun read(): Map<String, String> {
        while (readBlock()) {
        }
        return entries
    }

    private fun readBlock(): Boolean {
        currentId.clear()
        currentString.clear()

        do {
            val line = reader.readLine() ?: run {
                entries[currentId.toString()] = currentString.toString()
                return false
            }

            when {
                line.startsWith("msgid ") -> handleLine(line.substring(6), LineType.MessageId)
                line.startsWith("msgstr ") -> handleLine(line.substring(7), LineType.MessageStringSingular)
                line.startsWith('"') -> handleLine(line)
            }
        } while (line.trim().isNotEmpty())

        entries[currentId.toString()] = currentString.toString()
        return true
    }

    private fun handleLine(line: String, type: LineType? = null) {
        type?.let { currentType = it }

        when (currentType) {
            LineType.MessageId -> line.parseTo(currentId)
            LineType.MessageStringSingular -> line.parseTo(currentString)
            else -> throw IllegalStateException("Unexpected line '$line'")
        }
    }

    override fun close() = reader.close()

    private enum class LineType {
        MessageId,
        MessageStringSingular
    }

    private fun String.parseTo(builder: StringBuilder) {
        var escaped = false
        var inQuotes = false

        forEach {
            when {
                escaped -> {
                    builder.append(it.unescaped())
                    escaped = false
                }
                it == '"' && inQuotes -> return
                it == '"' -> inQuotes = true
                it == '\\' -> escaped = true
                else -> builder.append(it)
            }
        }
    }

    private fun Char.unescaped(): Char = when (this) {
        'n' -> '\n'
        'r' -> '\r'
        '"' -> '"'
        else -> throw IllegalArgumentException("Unknown escape char: $this")
    }

}
