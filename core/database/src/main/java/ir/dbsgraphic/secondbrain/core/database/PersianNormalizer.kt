package ir.dbsgraphic.secondbrain.core.database

/**
 * Persian text normalization for search — applied **identically** on the index
 * side (inside the FTS triggers, via [sqlExpression]) and the query side (via
 * [normalize] in Kotlin). Keeping a single source of truth here means the two
 * can never drift apart (design spine: same normalization on index and query).
 *
 * What it folds:
 *  - Arabic letter variants → Persian (ي→ی, ك→ک, ى→ی, ة→ه)
 *  - Zero-width non-joiner and tatweel → removed
 *  - Persian and Arabic-Indic digits → ASCII
 *
 * Case folding and diacritic/harakat stripping are handled by the FTS5
 * tokenizer (`unicode61 remove_diacritics 2`), which runs on both index and
 * query automatically — so they are intentionally not repeated here.
 */
object PersianNormalizer {

    /** (from, to) folds. Disjoint single chars, so order is irrelevant. */
    private val folds: List<Pair<String, String>> = buildList {
        add("ي" to "ی") // ي → ی
        add("ك" to "ک") // ك → ک
        add("ى" to "ی") // ى → ی
        add("ة" to "ه") // ة → ه
        add("‌" to "")       // ZWNJ → (removed)
        add("ـ" to "")       // tatweel → (removed)
        // Persian digits ۰..۹
        for (d in 0..9) add(('۰' + d).toString() to d.toString())
        // Arabic-Indic digits ٠..٩
        for (d in 0..9) add(('٠' + d).toString() to d.toString())
    }

    fun normalize(input: String): String {
        var out = input
        for ((from, to) in folds) out = out.replace(from, to)
        return out
    }

    /** Builds a nested SQLite `replace(...)` expression equivalent to [normalize]. */
    fun sqlExpression(columnExpr: String): String {
        var expr = columnExpr
        for ((from, to) in folds) {
            expr = "replace($expr, ${sqlLiteral(from)}, ${sqlLiteral(to)})"
        }
        return expr
    }

    private fun sqlLiteral(s: String): String = when (s) {
        "‌" -> "char(8204)" // ZWNJ — keep invisible chars out of SQL text
        "ـ" -> "char(1600)" // tatweel
        "" -> "''"
        else -> "'$s'"
    }
}
