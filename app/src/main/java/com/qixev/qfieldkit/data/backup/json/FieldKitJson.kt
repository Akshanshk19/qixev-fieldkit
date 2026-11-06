package com.qixev.qfieldkit.data.backup.json

sealed class JsonValue {
    data class JsonObject(val fields: Map<String, JsonValue>) : JsonValue() {
        fun get(key: String): JsonValue? = fields[key]
        fun getString(key: String): String? = (fields[key] as? JsonString)?.value
        fun getLong(key: String): Long? = (fields[key] as? JsonNumber)?.toLong()
        fun getInt(key: String): Int? = (fields[key] as? JsonNumber)?.toInt()
        fun getDouble(key: String): Double? = (fields[key] as? JsonNumber)?.toDouble()
        fun getBoolean(key: String): Boolean? = (fields[key] as? JsonBoolean)?.value
        fun getObject(key: String): JsonObject? = fields[key] as? JsonObject
        fun getArray(key: String): JsonArray? = fields[key] as? JsonArray
    }

    data class JsonArray(val elements: List<JsonValue>) : JsonValue()
    data class JsonString(val value: String) : JsonValue()
    data class JsonNumber(val raw: String) : JsonValue() {
        fun toLong(): Long = raw.toLongOrNull() ?: raw.toDouble().toLong()
        fun toInt(): Int = raw.toIntOrNull() ?: raw.toDouble().toInt()
        fun toDouble(): Double = raw.toDouble()
    }
    data class JsonBoolean(val value: Boolean) : JsonValue()
    data object JsonNull : JsonValue()
}

class JsonParseException(message: String, val line: Int = 1, val column: Int = 1) :
    IllegalArgumentException("$message at line $line, column $column")

class FieldKitJsonParser(private val src: String) {
    private var pos = 0
    private var line = 1
    private var col = 1

    fun parse(): JsonValue {
        skipWhitespace()
        if (pos >= src.length) {
            throw error("Empty JSON input")
        }
        val result = parseValue()
        skipWhitespace()
        if (pos < src.length) {
            throw error("Unexpected trailing character '${src[pos]}'")
        }
        return result
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        if (pos >= src.length) throw error("Unexpected end of JSON input")
        return when (val ch = src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.JsonString(parseString())
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            '-', in '0'..'9' -> parseNumber()
            else -> throw error("Unexpected character '$ch'")
        }
    }

    private fun parseObject(): JsonValue.JsonObject {
        expect('{')
        skipWhitespace()
        val map = linkedMapOf<String, JsonValue>()
        if (peek() == '}') {
            pos++
            col++
            return JsonValue.JsonObject(map)
        }

        while (true) {
            skipWhitespace()
            if (peek() != '"') throw error("Expected string key in object")
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            val next = peek()
            if (next == '}') {
                pos++
                col++
                break
            } else if (next == ',') {
                pos++
                col++
                skipWhitespace()
            } else {
                throw error("Expected ',' or '}' in object, found '$next'")
            }
        }
        return JsonValue.JsonObject(map)
    }

    private fun parseArray(): JsonValue.JsonArray {
        expect('[')
        skipWhitespace()
        val list = mutableListOf<JsonValue>()
        if (peek() == ']') {
            pos++
            col++
            return JsonValue.JsonArray(list)
        }

        while (true) {
            val value = parseValue()
            list.add(value)
            skipWhitespace()
            val next = peek()
            if (next == ']') {
                pos++
                col++
                break
            } else if (next == ',') {
                pos++
                col++
                skipWhitespace()
            } else {
                throw error("Expected ',' or ']' in array, found '$next'")
            }
        }
        return JsonValue.JsonArray(list)
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            val ch = src[pos++]
            col++
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= src.length) throw error("Unexpected end of escape sequence")
                    val escaped = src[pos++]
                    col++
                    when (escaped) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 > src.length) throw error("Incomplete unicode escape")
                            val hex = src.substring(pos, pos + 4)
                            pos += 4
                            col += 4
                            val code = hex.toIntOrNull(16) ?: throw error("Invalid unicode escape: \\u$hex")
                            sb.append(code.toChar())
                        }
                        else -> throw error("Invalid escape character '\\$escaped'")
                    }
                }
                '\n' -> {
                    line++
                    col = 1
                    sb.append(ch)
                }
                else -> sb.append(ch)
            }
        }
        throw error("Unterminated string")
    }

    private fun parseNumber(): JsonValue.JsonNumber {
        val start = pos
        if (peek() == '-') {
            pos++
            col++
        }
        if (pos >= src.length || src[pos] !in '0'..'9') {
            throw error("Invalid number format")
        }
        while (pos < src.length && src[pos] in '0'..'9') {
            pos++
            col++
        }
        if (pos < src.length && src[pos] == '.') {
            pos++
            col++
            if (pos >= src.length || src[pos] !in '0'..'9') {
                throw error("Expected digit after decimal point")
            }
            while (pos < src.length && src[pos] in '0'..'9') {
                pos++
                col++
            }
        }
        if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
            pos++
            col++
            if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) {
                pos++
                col++
            }
            if (pos >= src.length || src[pos] !in '0'..'9') {
                throw error("Expected digit in exponent")
            }
            while (pos < src.length && src[pos] in '0'..'9') {
                pos++
                col++
            }
        }
        val raw = src.substring(start, pos)
        return JsonValue.JsonNumber(raw)
    }

    private fun parseBoolean(): JsonValue.JsonBoolean {
        if (src.startsWith("true", pos)) {
            pos += 4
            col += 4
            return JsonValue.JsonBoolean(true)
        } else if (src.startsWith("false", pos)) {
            pos += 5
            col += 5
            return JsonValue.JsonBoolean(false)
        }
        throw error("Invalid boolean literal")
    }

    private fun parseNull(): JsonValue.JsonNull {
        if (src.startsWith("null", pos)) {
            pos += 4
            col += 4
            return JsonValue.JsonNull
        }
        throw error("Invalid null literal")
    }

    private fun skipWhitespace() {
        while (pos < src.length) {
            when (src[pos]) {
                ' ', '\t', '\r' -> {
                    pos++
                    col++
                }
                '\n' -> {
                    pos++
                    line++
                    col = 1
                }
                else -> return
            }
        }
    }

    private fun peek(): Char = if (pos < src.length) src[pos] else throw error("Unexpected end of input")

    private fun expect(expected: Char) {
        val ch = peek()
        if (ch != expected) throw error("Expected '$expected' but found '$ch'")
        pos++
        col++
    }

    private fun error(msg: String): JsonParseException = JsonParseException(msg, line, col)
}

object FieldKitJson {
    fun parse(src: String): JsonValue = FieldKitJsonParser(src).parse()

    fun stringify(value: JsonValue, indent: Int = 2): String {
        val sb = StringBuilder()
        write(value, sb, 0, indent)
        return sb.toString()
    }

    private fun write(value: JsonValue, sb: StringBuilder, level: Int, indent: Int) {
        when (value) {
            is JsonValue.JsonObject -> {
                if (value.fields.isEmpty()) {
                    sb.append("{}")
                    return
                }
                sb.append("{\n")
                val entries = value.fields.entries.toList()
                entries.forEachIndexed { index, (k, v) ->
                    sb.append(" ".repeat((level + 1) * indent))
                    sb.append('"').append(escape(k)).append("\": ")
                    write(v, sb, level + 1, indent)
                    if (index < entries.size - 1) {
                        sb.append(",")
                    }
                    sb.append("\n")
                }
                sb.append(" ".repeat(level * indent)).append("}")
            }
            is JsonValue.JsonArray -> {
                if (value.elements.isEmpty()) {
                    sb.append("[]")
                    return
                }
                sb.append("[\n")
                value.elements.forEachIndexed { index, item ->
                    sb.append(" ".repeat((level + 1) * indent))
                    write(item, sb, level + 1, indent)
                    if (index < value.elements.size - 1) {
                        sb.append(",")
                    }
                    sb.append("\n")
                }
                sb.append(" ".repeat(level * indent)).append("]")
            }
            is JsonValue.JsonString -> sb.append('"').append(escape(value.value)).append('"')
            is JsonValue.JsonNumber -> sb.append(value.raw)
            is JsonValue.JsonBoolean -> sb.append(value.value.toString())
            is JsonValue.JsonNull -> sb.append("null")
        }
    }

    private fun escape(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append(String.format("\\u%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }
}

