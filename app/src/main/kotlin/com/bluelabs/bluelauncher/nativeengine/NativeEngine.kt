package com.bluelabs.bluelauncher.nativeengine

/** Small native search and personalization scorer; Android package/UI work stays in Kotlin. */
object NativeEngine {
    /** True when the Rust library loaded; false after any load failure (also non-link errors). */
    val isAvailable: Boolean = try {
        System.loadLibrary("blue_launcher")
        true
    } catch (error: Throwable) {
        // Only UnsatisfiedLinkError was handled before: a static initialiser failure in
        // the generated JNI glue surfaced as ExceptionInInitializerError and crashed.
        false
    }

    private external fun rankApps(
        apps: String,
        query: String,
        favorites: String,
        recent: String,
    ): String?

    fun rank(
        apps: List<Pair<String, String>>,
        query: String,
        favorites: List<String>,
        recent: List<String>,
    ): List<String> {
        if (apps.isEmpty()) return emptyList()
        if (isAvailable) {
            val rows = buildString {
                apps.forEach { (packageName, label) ->
                    // The native side splits on tab and newline; a label containing either
                    // would corrupt the record and drop the app from the results.
                    append(sanitize(packageName)).append('\t').append(sanitize(label)).append('\n')
                }
            }
            val result = runCatching {
                rankApps(rows, query, favorites.joinToString("\n"), recent.joinToString("\n"))
            }.getOrNull()
            if (result != null) return result.lineSequence().filter(String::isNotBlank).toList()
        }

        val favoritePositions = favorites.withIndex().associate { it.value to it.index }
        val recentPositions = recent.withIndex().associate { it.value to it.index }
        return apps.asSequence()
            .mapNotNull { (packageName, label) ->
                val textScore = matchScore(label, query)
                if (query.isNotBlank() && textScore < 0) null
                else packageName to (textScore +
                    favoritePositions[packageName]?.let { 100 - it.coerceAtMost(99) }.orZero() +
                    recentPositions[packageName]?.let { 50 - it.coerceAtMost(49) }.orZero())
            }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .map { it.first }
            .toList()
    }

    private fun Int?.orZero(): Int = this ?: 0

    /** Keeps one record per line: tabs/newlines in a label would split it in the JNI payload. */
    private fun sanitize(value: String): String =
        value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

    private fun matchScore(label: String, query: String): Int {
        if (query.isBlank()) return 0
        val name = label.lowercase()
        val term = query.trim().lowercase()
        return when {
            name == term -> 1_000
            name.startsWith(term) -> 800 - (name.length - term.length).coerceAtMost(100)
            name.split(' ').any { it.startsWith(term) } -> 650
            name.contains(term) -> 500
            else -> -1
        }
    }
}
