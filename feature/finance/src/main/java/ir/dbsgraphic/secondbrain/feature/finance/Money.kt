package ir.dbsgraphic.secondbrain.feature.finance

import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

/** Format an amount of Tomans with thousands separators and Persian digits. */
internal fun formatToman(amount: Long): String {
    val grouped = buildString {
        val digits = amount.toString()
        val len = digits.length
        for (i in 0 until len) {
            if (i > 0 && (len - i) % 3 == 0) append('٬')
            append(digits[i])
        }
    }.toPersianDigits()
    return "$grouped تومان"
}
