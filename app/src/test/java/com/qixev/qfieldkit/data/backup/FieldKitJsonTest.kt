package com.qixev.qfieldkit.data.backup

import com.qixev.qfieldkit.data.backup.json.FieldKitJson
import com.qixev.qfieldkit.data.backup.json.JsonParseException
import com.qixev.qfieldkit.data.backup.json.JsonValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldKitJsonTest {

    @Test
    fun parseSimpleObject() {
        val json = """{"key": "value", "num": 42, "flag": true, "empty": null}"""
        val parsed = FieldKitJson.parse(json) as JsonValue.JsonObject

        assertEquals("value", parsed.getString("key"))
        assertEquals(42, parsed.getInt("num"))
        assertEquals(true, parsed.getBoolean("flag"))
        assertNull(parsed.getString("empty"))
    }

    @Test
    fun parseNestedArraysAndObjects() {
        val json = """
            {
                "items": [
                    {"id": 1, "name": "Screen"},
                    {"id": 2, "name": "Battery"}
                ]
            }
        """.trimIndent()
        val parsed = FieldKitJson.parse(json) as JsonValue.JsonObject
        val items = parsed.getArray("items")
        assertNotNull(items)
        assertEquals(2, items!!.elements.size)

        val first = items.elements[0] as JsonValue.JsonObject
        assertEquals(1L, first.getLong("id"))
        assertEquals("Screen", first.getString("name"))
    }

    @Test
    fun handleEscapesProperly() {
        val json = """{"text": "Line 1\nLine 2\t\"quoted\" \\ backslash"}"""
        val parsed = FieldKitJson.parse(json) as JsonValue.JsonObject
        assertEquals("Line 1\nLine 2\t\"quoted\" \\ backslash", parsed.getString("text"))

        val stringified = FieldKitJson.stringify(parsed)
        val reparsed = FieldKitJson.parse(stringified) as JsonValue.JsonObject
        assertEquals("Line 1\nLine 2\t\"quoted\" \\ backslash", reparsed.getString("text"))
    }

    @Test(expected = JsonParseException::class)
    fun rejectMalformedSyntax() {
        FieldKitJson.parse("""{"broken": """)
    }

    @Test(expected = JsonParseException::class)
    fun rejectUnclosedString() {
        FieldKitJson.parse("""{"broken": "unterminated}""")
    }

    @Test(expected = JsonParseException::class)
    fun rejectTrailingGarbage() {
        FieldKitJson.parse("""{"key": 1} trailing""")
    }
}

