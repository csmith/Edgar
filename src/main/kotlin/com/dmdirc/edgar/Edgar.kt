package com.dmdirc.edgar

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Edgar reads GNU gettext po files and returns translations.
 *
 * To use Edgar, [init] it with the translations folder and an optional default domain. Edgar expects the translations
 * path to contain a folder per language, and each language to contain a `.po` file per domain:
 *
 *     translations/
 *         en-GB/
 *              messages.po
 *              help.po
 *         de/
 *              messages.po
 *              help.po
 *
 * To set the current language, call [setLanguage].
 *
 * To get a translation, use one of the [tr] functions. If the string isn't translated in the language and domain
 * combo, Edgar will return the messageId unmodified.
 */
object Edgar {

    private var defaultDomain: String = ""
    private var folder: Path = Paths.get("")

    private var initialised: Boolean = false
    private var language: String? = null
    private val cache = mutableMapOf<String, Map<String, String>>()

    /**
     * Initialises Edgar for use. This method must be called before any others in this class.
     *
     * After initialisation you will probably want to call [setLanguage] to set the translation to use; otherwise
     * the translation methods will just return the message IDs passed to them.
     */
    fun init(folder: Path, defaultDomain: String = "messages") {
        require(Files.isDirectory(folder)) { "[folder] must be a directory" }
        reset()
        this.folder = folder
        this.defaultDomain = defaultDomain
        this.initialised = true
    }

    /**
     * Sets the language that Edgar will use for future translations.
     *
     * @throws IllegalStateException if Edgar has not been initialised with the [init] method.
     * @throws IllegalArgumentException if the specified language doesn't exist
     */
    fun setLanguage(language: String) {
        check(initialised) { "Edgar must be initialised with the [init] method before use" }
        require(isLanguage(language)) { "Language $language is not available" }

        this.language = language
        this.cache.clear()
        this.loadDomain(defaultDomain)
    }

    /**
     * Returns a collection of all available languages.
     *
     * A language is considered to be available if a folder with its name exists in the translation root.
     *
     * Folders starting with a "." are ignored.
     *
     * @throws IllegalStateException if Edgar has not been initialised with the [init] method.
     */
    fun getLanguages(): Collection<String> {
        check(initialised) { "Edgar must be initialised with the [init] method before use" }
        return Files.newDirectoryStream(folder) { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .filterNot { it.startsWith('.') }
    }

    /**
     * Loads translations for the specified domain.
     *
     * @throws IllegalStateException if Edgar has not been initialised with the [init] method.
     * @throws IllegalArgumentException if the specified domain does not exist in the current language.
     */
    fun loadDomain(domain: String) {
        check(initialised) { "Edgar must be initialised with the [init] method before use" }
        require(isDomain(domain)) { "Domain $domain does not exist in language $language"}
        cache[domain] = PoFileReader(folder.resolve(language).resolve("$domain.po")).use {
            it.read()
        }
    }

    /**
     * Gets the translation for the given [messageId] in the current language and the default domain.
     *
     * If the translation is not found, returns the [messageId] unmodified.
     *
     * @throws IllegalStateException if Edgar has not been initialised with the [init] method.
     */
    fun tr(messageId: String) = tr(defaultDomain, messageId)

    /**
     * Gets the translation for the given [messageId] in the current language and the given [domain].
     *
     * The domain will be loaded if it is not already cached.
     *
     * If the translation is not found, returns the [messageId] unmodified.
     *
     * @throws IllegalStateException if Edgar has not been initialised with the [init] method.
     * @throws IllegalArgumentException if the specified domain does not exist in the current language.
     */
    fun tr(domain: String, messageId: String): String {
        check(initialised) { "Edgar must be initialised with the [init] method before use" }
        if (domain !in cache) { loadDomain(domain) }
        return cache[domain]?.get(messageId) ?: messageId
    }

    /**
     * Resets Edgar's state entirely. You must call [init] again before using any other methods.
     */
    fun reset() {
        defaultDomain = ""
        folder = Paths.get("")
        initialised = false
        language = null
        cache.clear()
    }

    /**
     * Determines if the given language corresponds to a valid folder within the root directory.
     */
    private fun isLanguage(language: String) = !language.startsWith('.') && Files.isDirectory(folder.resolve(language))

    /**
     * Determines if the given domain exists as a `.po` file within the current language's directory.
     */
    private fun isDomain(domain: String) = Files.isRegularFile(folder.resolve(language).resolve("$domain.po"))

}