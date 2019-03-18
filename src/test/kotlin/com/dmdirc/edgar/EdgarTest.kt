package com.dmdirc.edgar

import com.dmdirc.edgar.Edgar.getLanguages
import com.dmdirc.edgar.Edgar.init
import com.dmdirc.edgar.Edgar.loadDomain
import com.dmdirc.edgar.Edgar.reset
import com.dmdirc.edgar.Edgar.setLanguage
import com.dmdirc.edgar.Edgar.tr
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path

internal class EdgarTest {

    private var fs = Jimfs.newFileSystem(Configuration.unix())

    @BeforeEach
    fun setup() {
        reset()
    }

    @Test
    fun `init throws if folder is not a directory`() {
        assertThrows<IllegalArgumentException> {
            init(fs.getPath("/foo"))
        }
    }

    @Test
    fun `getLanguages throws if Edgar is not initialised`() {
        assertThrows<IllegalStateException> {
            getLanguages()
        }
    }

    @Test
    fun `getLanguages returns all non-hidden folder names in the translation root`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/en-GB"))
        Files.createDirectory(fs.getPath("/translations/fr"))
        Files.createDirectory(fs.getPath("/translations/.git"))
        Files.createFile(fs.getPath("/translations/metadata"))

        init(fs.getPath("/translations"))
        assertEquals(setOf("en-GB", "fr"), getLanguages().toSet())
    }

    @Test
    fun `setLanguage throws if Edgar is not initialised`() {
        assertThrows<IllegalStateException> {
            setLanguage("en-GB")
        }
    }

    @Test
    fun `setLanguage throws if language folder does not exist`() {
        Files.createDirectory(fs.getPath("/translations"))
        init(fs.getPath("/translations"))

        assertThrows<IllegalArgumentException> {
            setLanguage("en-GB")
        }
    }

    @Test
    fun `setLanguage throws if language folder starts with a period`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/.git"))
        init(fs.getPath("/translations"))

        assertThrows<IllegalArgumentException> { setLanguage(".git") }
        assertThrows<IllegalArgumentException> { setLanguage(".") }
    }

    @Test
    fun `setLanguage throws if language is not a folder`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createFile(fs.getPath("/translations/en-GB"))
        init(fs.getPath("/translations"))

        assertThrows<IllegalArgumentException> {
            setLanguage("en-GB")
        }
    }

    @Test
    fun `setLanguage throws if default domain doesn't exist in folder`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/en-GB"))
        init(fs.getPath("/translations"))

        assertThrows<IllegalArgumentException> {
            setLanguage("en-GB")
        }
    }

    @Test
    fun `loadDomain throws if Edgar is not initialised`() {
        assertThrows<IllegalStateException> {
            loadDomain("messages")
        }
    }

    @Test
    fun `loadDomain throws if domain doesn't exist`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/en-GB"))
        Files.createFile(fs.getPath("/translations/en-GB/messages.po"))
        init(fs.getPath("/translations"))
        setLanguage("en-GB")

        assertThrows<IllegalArgumentException> {
            loadDomain("foo")
        }
    }

    @Test
    fun `tr throws if Edgar is not initialised`() {
        assertThrows<IllegalStateException> {
            tr("en-GB")
        }
    }

    @Test
    fun `tr returns translation from current language and domain`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/de"))
        putResource("/basic.po", fs.getPath("/translations/de/mydomain.po"))
        init(fs.getPath("/translations"), "mydomain")
        setLanguage("de")

        assertEquals("Hacke den Planeten!", tr("Hack the planet!"))
    }

    @Test
    fun `tr returns msgid for unknown translations`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/de"))
        putResource("/basic.po", fs.getPath("/translations/de/mydomain.po"))
        init(fs.getPath("/translations"), "mydomain")
        setLanguage("de")

        assertEquals("Crash and burn", tr("Crash and burn"))
    }

    @Test
    fun `tr returns msgid for blank translations`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/de"))
        putResource("/basic.po", fs.getPath("/translations/de/mydomain.po"))
        init(fs.getPath("/translations"), "mydomain")
        setLanguage("de")

        assertEquals("Untranslated", tr("Untranslated"))
    }

    @Test
    fun `tr returns translation from current language and specific domain`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/de"))
        Files.createFile(fs.getPath("/translations/de/messages.po"))
        putResource("/basic.po", fs.getPath("/translations/de/mydomain.po"))
        init(fs.getPath("/translations"))
        setLanguage("de")

        assertEquals("Hacke den Planeten!", tr("mydomain", "Hack the planet!"))
    }

    @Test
    fun `tr throws if specific domain is unknown`() {
        Files.createDirectory(fs.getPath("/translations"))
        Files.createDirectory(fs.getPath("/translations/de"))
        Files.createFile(fs.getPath("/translations/de/messages.po"))
        putResource("/basic.po", fs.getPath("/translations/de/mydomain.po"))
        init(fs.getPath("/translations"))
        setLanguage("de")

        assertThrows<IllegalArgumentException> {
            tr("other", "Hack the planet!")
        }
    }


    private fun putResource(name: String, path: Path) = Files.copy(javaClass.getResourceAsStream(name), path)

}
