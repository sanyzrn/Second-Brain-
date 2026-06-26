package ir.dbsgraphic.secondbrain.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianNormalizerTest {

    @Test
    fun `folds arabic letter variants to persian`() {
        // Arabic Yeh/Kaf → Persian Yeh/Kaf
        assertEquals("کیک", PersianNormalizer.normalize("كيك"))
        assertEquals("علی", PersianNormalizer.normalize("علي"))
    }

    @Test
    fun `removes zero-width non-joiner and tatweel`() {
        assertEquals("میخواهم", PersianNormalizer.normalize("می‌خواهم")) // ZWNJ removed
        assertEquals("سلام", PersianNormalizer.normalize("سلــام")) // tatweel removed
    }

    @Test
    fun `folds persian and arabic digits to ascii`() {
        assertEquals("123", PersianNormalizer.normalize("۱۲۳"))
        assertEquals("456", PersianNormalizer.normalize("٤٥٦"))
    }

    @Test
    fun `index and query expressions use the same folds`() {
        // The SQL expression must reference replace() so index matches query.
        val sql = PersianNormalizer.sqlExpression("content")
        assert(sql.contains("replace(")) { "expected nested replace() in: $sql" }
    }
}
