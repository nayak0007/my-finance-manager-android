package com.myfinancemanager.app.data.parser

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

class StatementImporter(private val context: Context) {

    fun parse(uri: Uri, fileName: String): List<ParsedTransaction> {
        val lower = fileName.lowercase()
        val lines = readLines(uri)
        return parseLines(lines, lower)
    }

    fun parseContent(content: String, fileName: String): List<ParsedTransaction> {
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        return parseLines(lines, fileName.lowercase())
    }

    private fun parseLines(lines: List<String>, lowerName: String): List<ParsedTransaction> {
        return when {
            lowerName.endsWith(".csv") -> parseCsvLines(lines)
            lowerName.endsWith(".txt") -> TransactionParser.parseGenericTextLines(lines)
            lowerName.endsWith(".pdf") || lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") -> {
                TransactionParser.parseGenericTextLines(lines).ifEmpty { parseCsvLines(lines) }
            }
            else -> parseCsvLines(lines).ifEmpty { TransactionParser.parseGenericTextLines(lines) }
        }
    }

    private fun parseCsvLines(lines: List<String>): List<ParsedTransaction> {
        if (lines.isEmpty()) return emptyList()
        val headerIndex = lines.indexOfFirst { line ->
            val lower = line.lowercase()
            lower.contains("date") && (lower.contains("amount") || lower.contains("debit") || lower.contains("credit"))
        }.takeIf { it >= 0 } ?: 0
        val headers = splitCsv(lines[headerIndex])
        return lines.drop(headerIndex + 1).mapNotNull { line ->
            val values = splitCsv(line)
            if (values.all { it.isBlank() }) null
            else TransactionParser.parseCsvLine(headers, values)
        }
    }

    private fun readLines(uri: Uri): List<String> {
        val input = context.contentResolver.openInputStream(uri) ?: return emptyList()
        return input.use { stream ->
            BufferedReader(InputStreamReader(stream, Charset.forName("UTF-8"))).readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    }

    private fun splitCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result += current.toString()
        return result
    }
}
