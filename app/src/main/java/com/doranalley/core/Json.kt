package com.doranalley.core

/**
 * Minimal JSON reader/writer so the core has no Android or library dependency
 * and can be unit-tested on a plain JVM. Objects become LinkedHashMap<String, Any?>,
 * arrays become List<Any?>, integers become Long, other numbers Double.
 */
object Json {

    fun parse(text: String): Any? {
        val p = Parser(text)
        p.ws()
        val v = p.value()
        p.ws()
        if (p.pos != text.length) p.fail("Unexpected trailing data")
        return v
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): JObj = parse(text) as? JObj ?: error("JSON root is not an object")

    fun write(value: Any?, pretty: Boolean = true): String {
        val sb = StringBuilder()
        writeValue(sb, value, if (pretty) 0 else -1)
        return sb.toString()
    }

    private fun writeValue(sb: StringBuilder, v: Any?, indent: Int) {
        when (v) {
            null -> sb.append("null")
            is String -> writeString(sb, v)
            is Boolean -> sb.append(v)
            is Int, is Long, is Short, is Byte -> sb.append(v.toString())
            is Float -> writeDouble(sb, v.toDouble())
            is Double -> writeDouble(sb, v)
            is Map<*, *> -> {
                if (v.isEmpty()) { sb.append("{}"); return }
                sb.append('{')
                var first = true
                for ((k, value) in v) {
                    if (!first) sb.append(',')
                    first = false
                    newline(sb, indent + 1)
                    writeString(sb, k.toString())
                    sb.append(if (indent >= 0) ": " else ":")
                    writeValue(sb, value, if (indent >= 0) indent + 1 else -1)
                }
                newline(sb, indent)
                sb.append('}')
            }
            is Iterable<*> -> {
                val list = v.toList()
                if (list.isEmpty()) { sb.append("[]"); return }
                sb.append('[')
                list.forEachIndexed { i, item ->
                    if (i > 0) sb.append(',')
                    newline(sb, indent + 1)
                    writeValue(sb, item, if (indent >= 0) indent + 1 else -1)
                }
                newline(sb, indent)
                sb.append(']')
            }
            is Array<*> -> writeValue(sb, v.toList(), indent)
            else -> writeString(sb, v.toString())
        }
    }

    private fun writeDouble(sb: StringBuilder, d: Double) {
        if (d.isNaN() || d.isInfinite()) sb.append("null")
        else if (d == Math.floor(d) && Math.abs(d) < 1e15) sb.append(d.toLong()).append(".0")
        else sb.append(d.toString())
    }

    private fun newline(sb: StringBuilder, indent: Int) {
        if (indent < 0) return
        sb.append('\n')
        repeat(indent) { sb.append("  ") }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    private class Parser(val s: String) {
        var pos = 0

        fun fail(msg: String): Nothing = throw IllegalArgumentException("$msg at position $pos")

        fun ws() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun value(): Any? {
            if (pos >= s.length) fail("Unexpected end")
            return when (val c = s[pos]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c.isDigit()) num() else fail("Unexpected '$c'")
            }
        }

        private fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, pos)) fail("Expected $word")
            pos += word.length
            return v
        }

        private fun obj(): JObj {
            val m = LinkedHashMap<String, Any?>()
            pos++
            ws()
            if (s.getOrNull(pos) == '}') { pos++; return m }
            while (true) {
                ws()
                if (s.getOrNull(pos) != '"') fail("Expected key")
                val k = str()
                ws()
                if (s.getOrNull(pos) != ':') fail("Expected ':'")
                pos++
                ws()
                m[k] = value()
                ws()
                when (s.getOrNull(pos)) {
                    ',' -> pos++
                    '}' -> { pos++; return m }
                    else -> fail("Expected ',' or '}'")
                }
            }
        }

        private fun arr(): List<Any?> {
            val l = ArrayList<Any?>()
            pos++
            ws()
            if (s.getOrNull(pos) == ']') { pos++; return l }
            while (true) {
                ws()
                l.add(value())
                ws()
                when (s.getOrNull(pos)) {
                    ',' -> pos++
                    ']' -> { pos++; return l }
                    else -> fail("Expected ',' or ']'")
                }
            }
        }

        private fun str(): String {
            val sb = StringBuilder()
            pos++
            while (true) {
                if (pos >= s.length) fail("Unterminated string")
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = s.getOrNull(pos++) ?: fail("Bad escape")
                        when (e) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > s.length) fail("Bad unicode escape")
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> fail("Bad escape '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun num(): Any {
            val start = pos
            if (s[pos] == '-') pos++
            while (pos < s.length && (s[pos].isDigit() || s[pos] in ".eE+-")) pos++
            val t = s.substring(start, pos)
            return if (t.any { it == '.' || it == 'e' || it == 'E' }) t.toDouble() else t.toLong()
        }
    }
}

typealias JObj = Map<String, Any?>

@Suppress("UNCHECKED_CAST")
fun JObj.obj(k: String): JObj = this[k] as? JObj ?: error("Missing object '$k'")

@Suppress("UNCHECKED_CAST")
fun JObj.objOrNull(k: String): JObj? = this[k] as? JObj

fun JObj.str(k: String): String = this[k] as? String ?: error("Missing string '$k'")
fun JObj.strOrNull(k: String): String? = this[k] as? String
fun JObj.arr(k: String): List<Any?> = this[k] as? List<Any?> ?: emptyList()

@Suppress("UNCHECKED_CAST")
fun JObj.objs(k: String): List<JObj> = arr(k).map { it as JObj }
fun JObj.strs(k: String): List<String> = arr(k).map { it.toString() }
fun JObj.long(k: String, def: Long = 0): Long = (this[k] as? Number)?.toLong() ?: def
fun JObj.int(k: String, def: Int = 0): Int = (this[k] as? Number)?.toInt() ?: def
fun JObj.double(k: String, def: Double = 0.0): Double = (this[k] as? Number)?.toDouble() ?: def
fun JObj.bool(k: String, def: Boolean = false): Boolean = this[k] as? Boolean ?: def
fun JObj.loc(k: String): L = objOrNull(k)?.mapValues { it.value.toString() } ?: emptyMap()

@Suppress("UNCHECKED_CAST")
fun JObj.strMap(k: String): Map<String, String> = objOrNull(k)?.mapValues { it.value.toString() } ?: emptyMap()
