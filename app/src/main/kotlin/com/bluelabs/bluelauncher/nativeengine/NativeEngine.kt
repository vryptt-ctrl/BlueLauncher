package com.bluelabs.bluelauncher.nativeengine

/** Small native search and personalization scorer; Android package/UI work stays in Kotlin. */
object NativeEngine {
    val isAvailable: Boolean = try {
        System.loadLibrary("blue_launcher")
        true
    } catch (_: UnsatisfiedLinkError) {
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
                    append(packageName).append('\t').append(label).append('\n')
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
