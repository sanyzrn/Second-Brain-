package ir.dbsgraphic.secondbrain.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagsCodecTest {

    @Test
    fun `encode trims, drops blanks, and de-duplicates`() {
        val json = TagsCodec.encode(listOf(" خانه ", "خانه", "", "فوری"))
        assertEquals(listOf("خانه", "فوری"), TagsCodec.decode(json))
    }

    @Test
    fun `round trips a list`() {
        val tags = listOf("کار", "مهم", "۱۴۰۵")
        assertEquals(tags, TagsCodec.decode(TagsCodec.encode(tags)))
    }

    @Test
    fun `decode of garbage yields empty list`() {
        assertEquals(emptyList<String>(), TagsCodec.decode("not json"))
        assertEquals(emptyList<String>(), TagsCodec.decode(""))
    }
}
