package ir.dbsgraphic.secondbrain.feature.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbHairline
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbText
import ir.dbsgraphic.secondbrain.core.designsystem.component.SbTextField
import ir.dbsgraphic.secondbrain.core.designsystem.theme.SecondBrainTheme
import ir.dbsgraphic.secondbrain.core.designsystem.util.JalaliDate
import ir.dbsgraphic.secondbrain.core.designsystem.util.toPersianDigits

@Composable
fun FinanceRoute(
    onOpenItem: (String) -> Unit,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinanceScreen(
        state = state,
        onPay = viewModel::pay,
        onOpenItem = onOpenItem,
        onAddExpense = viewModel::addExpense,
        onAddInstallment = viewModel::addInstallment,
    )
}

@Composable
fun FinanceScreen(
    state: FinanceState,
    onPay: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onAddExpense: (String, Long, Long?) -> Unit,
    onAddInstallment: (String, Long, Int, Int, Long?) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    var showAdd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(horizontal = space.xl),
    ) {
        // Summary: the one number that matters — what's still owed.
        SummaryCard(total = state.totalRemaining)
        Spacer(Modifier.height(space.lg))

        Box(modifier = Modifier.weight(1f)) {
            if (state.rows.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    SbText(text = "هنوز هزینه‌ای ثبت نشده.", style = type.title)
                    Spacer(Modifier.height(space.sm))
                    SbText(
                        text = "خرج‌ها و اقساطت را اینجا نگه دار تا سررسیدی از قلم نیفتد.",
                        style = type.body,
                        color = colors.muted,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.rows, key = { it.item.id }) { row ->
                        FinanceRow(
                            ui = row,
                            onPay = { onPay(row.item.id) },
                            onOpen = { onOpenItem(row.item.id) },
                        )
                        SbHairline()
                    }
                }
            }
        }

        if (showAdd) {
            AddSheet(
                onDismiss = { showAdd = false },
                onAddExpense = { title, amount, due ->
                    onAddExpense(title, amount, due); showAdd = false
                },
                onAddInstallment = { title, per, count, period, due ->
                    onAddInstallment(title, per, count, period, due); showAdd = false
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SecondBrainTheme.shapes.large)
                    .background(colors.accent)
                    .clickable { showAdd = true }
                    .padding(vertical = space.lg),
                contentAlignment = Alignment.Center,
            ) {
                SbText(text = "افزودن هزینه", style = type.label, color = colors.onAccent)
            }
        }
        Spacer(Modifier.height(space.md))
    }
}

@Composable
private fun SummaryCard(total: Long) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .padding(space.lg),
    ) {
        SbText(text = "مانده‌ی پرداختی", style = type.caption, color = colors.muted)
        Spacer(Modifier.height(space.xs))
        SbText(text = formatToman(total), style = type.title, color = colors.accent)
    }
}

@Composable
private fun FinanceRow(ui: FinanceUi, onPay: () -> Unit, onOpen: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
            SbText(
                text = ui.item.content,
                style = type.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SbText(
                    text = formatToman(ui.finance.remainingAmount),
                    style = type.mono,
                    color = if (ui.finance.isDone) colors.muted else colors.text,
                )
                if (ui.isInstallment) {
                    Spacer(Modifier.width(space.sm))
                    val paid = ui.finance.installmentsPaid.toString().toPersianDigits()
                    val all = ui.finance.installmentTotal.toString().toPersianDigits()
                    SbText(text = "قسط $paid از $all", style = type.monoSmall, color = colors.muted)
                }
            }
            ui.dueAt?.let { due ->
                Spacer(Modifier.height(space.xs))
                SbText(
                    text = "سررسید: ${JalaliDate.formatDate(due)}",
                    style = type.monoSmall,
                    color = colors.muted,
                )
            }
        }

        if (ui.finance.isDone) {
            SbText(text = "تسویه شد", style = type.label, color = colors.accent)
        } else {
            Box(
                modifier = Modifier
                    .clip(SecondBrainTheme.shapes.medium)
                    .background(colors.accent)
                    .clickable(onClick = onPay)
                    .padding(horizontal = space.lg, vertical = space.sm),
            ) {
                SbText(
                    text = if (ui.isInstallment) "پرداخت قسط" else "پرداخت شد",
                    style = type.label,
                    color = colors.onAccent,
                )
            }
        }
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private data class DuePreset(val label: String, val offsetDays: Int?)

private val duePresets = listOf(
    DuePreset("بدون تاریخ", null),
    DuePreset("امروز", 0),
    DuePreset("فردا", 1),
    DuePreset("یک هفته", 7),
    DuePreset("یک ماه", 30),
)

@Composable
private fun AddSheet(
    onDismiss: () -> Unit,
    onAddExpense: (String, Long, Long?) -> Unit,
    onAddInstallment: (String, Long, Int, Int, Long?) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    var installment by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("30") }
    var dueIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .padding(space.lg),
    ) {
        // Type toggle.
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            Toggle(label = "هزینه", selected = !installment) { installment = false }
            Toggle(label = "قسطی", selected = installment) { installment = true }
            Spacer(Modifier.weight(1f))
            SbText(
                text = "بستن",
                style = type.label,
                color = colors.muted,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
        Spacer(Modifier.height(space.md))

        Field(value = title, onValueChange = { title = it }, placeholder = "عنوان…")
        Spacer(Modifier.height(space.sm))
        Field(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() } },
            placeholder = if (installment) "مبلغ هر قسط (تومان)" else "مبلغ (تومان)",
            numeric = true,
        )

        if (installment) {
            Spacer(Modifier.height(space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                Field(
                    value = count,
                    onValueChange = { count = it.filter { c -> c.isDigit() } },
                    placeholder = "تعداد اقساط",
                    numeric = true,
                    modifier = Modifier.weight(1f),
                )
                Field(
                    value = period,
                    onValueChange = { period = it.filter { c -> c.isDigit() } },
                    placeholder = "هر چند روز",
                    numeric = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(space.md))
        SbText(text = if (installment) "اولین سررسید" else "سررسید", style = type.caption, color = colors.muted)
        Spacer(Modifier.height(space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            duePresets.forEachIndexed { index, preset ->
                Toggle(label = preset.label, selected = dueIndex == index) { dueIndex = index }
            }
        }

        Spacer(Modifier.height(space.lg))
        val amountValue = amount.toLongOrNull() ?: 0
        val countValue = count.toIntOrNull() ?: 0
        val periodValue = period.toIntOrNull() ?: 30
        val canAdd = title.isNotBlank() && amountValue > 0 &&
            (!installment || countValue >= 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SecondBrainTheme.shapes.medium)
                .background(if (canAdd) colors.accent else colors.hairline)
                .clickable(enabled = canAdd) {
                    val due = duePresets[dueIndex].offsetDays?.let {
                        System.currentTimeMillis() + it * DAY_MS
                    }
                    if (installment) {
                        onAddInstallment(title.trim(), amountValue, countValue, periodValue, due)
                    } else {
                        onAddExpense(title.trim(), amountValue, due)
                    }
                }
                .padding(vertical = space.md),
            contentAlignment = Alignment.Center,
        ) {
            SbText(
                text = "ثبت",
                style = type.label,
                color = if (canAdd) colors.onAccent else colors.muted,
            )
        }
    }
}

@Composable
private fun Toggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing
    Box(
        modifier = Modifier
            .clip(SecondBrainTheme.shapes.medium)
            .background(if (selected) colors.accent else colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = space.md, vertical = space.sm),
    ) {
        SbText(
            text = label,
            style = type.label,
            color = if (selected) colors.onAccent else colors.muted,
        )
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
) {
    val colors = SecondBrainTheme.colors
    val space = SecondBrainTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.medium)
            .background(colors.background)
            .padding(horizontal = space.md, vertical = space.md),
    ) {
        SbTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            singleLine = true,
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
        )
    }
}
