package io.github.radixtrator.resync.i18n

/**
 * The German catalog: English source string → German. Everyday app German with a few r/ich_iel words (Lases,
 * Pfosten, Faden, Hochwähli, Edith); see faden/i18n/GLOSSARY-de.md. Split by area so no single map gets near the JVM's
 * method size limit; each area file keeps its entries in the order its screens show them.
 */
internal object German {
    val strings: Map<String, String> by lazy {
        HashMap<String, String>(2048).apply {
            putAll(germanSettings())
            putAll(germanPost())
            putAll(germanFeed())
            putAll(germanCompose())
            putAll(germanData())
        }
    }

    /** Keyed by the English plural ("{0} comments"); the value is German (singular, plural). */
    val plurals: Map<String, Pair<String, String>> by lazy {
        HashMap<String, Pair<String, String>>(256).apply {
            putAll(germanSettingsPlurals())
            putAll(germanPostPlurals())
            putAll(germanFeedPlurals())
            putAll(germanComposePlurals())
            putAll(germanDataPlurals())
        }
    }
}
