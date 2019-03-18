package com.dmdirc.edgar

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files

internal class PoFileReaderTest {

    private var fs = Jimfs.newFileSystem(Configuration.unix())

    @Test
    fun `parses basic translation`() {
        val reader = PoFileReader(getResource("/basic.po"))
        val result = reader.read()
        assertEquals("Hacke den Planeten!", result["Hack the planet!"])
    }

    @Test
    fun `parses multi-line translation`() {
        val reader = PoFileReader(getResource("/multiline.po"))
        val result = reader.read()
        assertEquals("Hacke den Planeten!", result["Hack the planet!"])
    }

    @Test
    fun `parses escaped fields`() {
        val reader = PoFileReader(getResource("/escaped.po"))
        val result = reader.read()
        assertEquals("\"Hacke den Planeten!\"\r\n", result["\"Hack the planet!\"\n"])
    }

    @Test
    fun `parses metadata`() {
        val reader = PoFileReader(getResource("/basic.po"))
        val result = reader.read()
        assertEquals(
                "Project-Id-Version: Edgar 1.0\n" +
                        "Language: de\n" +
                        "MIME-Version: 1.0\n" +
                        "Content-Type: text/plain; charset=UTF-8\n" +
                        "Content-Transfer-Encoding: 8bit\n" +
                        "Plural-Forms: nplurals=2; plural=(n != 1);\n", result[""])
    }

    @Test
    fun `throws for malformed file`() {
        val reader = PoFileReader(getResource("/malformed.po"))
        assertThrows<IllegalStateException>("Unexpected line '\"\"'") {
            reader.read()
        }
    }

    @Test
    fun `throws for bad escape char`() {
        val reader = PoFileReader(getResource("/badescape.po"))
        assertThrows<IllegalArgumentException>("Unknown escape char: a") {
            reader.read()
        }
    }

    private fun getResource(name: String) = fs.getPath("/file.po").also {
        Files.copy(javaClass.getResourceAsStream(name), it)
    }

}