package ir.dbsgraphic.secondbrain.core.data

/** The triage types an item can take. [value] is stored; [label] is the voice. */
enum class ItemType(val value: String, val label: String) {
    NOTE("note", "یادداشت"),
    TASK("task", "کار"),
    IDEA("idea", "ایده"),
    DOC("doc", "سند"),
    ;

    companion object {
        fun fromValue(value: String?): ItemType? = entries.find { it.value == value }
    }
}
