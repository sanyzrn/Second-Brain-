package ir.dbsgraphic.secondbrain.feature.medicine

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
import androidx.compose.runtime.mutableIntStateOf
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
fun MedicineRoute(
    onOpenItem: (String) -> Unit,
    viewModel: MedicineViewModel = hiltViewModel(),
) {
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    MedicineScreen(
        medicines = medicines,
        onLogDose = viewModel::logDose,
        onRefill = { id -> viewModel.refill(id, REFILL_AMOUNT) },
        onCreate = viewModel::create,
        onOpenItem = onOpenItem,
    )
}

/** A refill tops up a fixed batch — keeps the row action one tap. */
private const val REFILL_AMOUNT = 30

@Composable
fun MedicineScreen(
    medicines: List<MedicineUi>,
    onLogDose: (String) -> Unit,
    onRefill: (String) -> Unit,
    onCreate: (String, String, Int, Int, Int) -> Unit,
    onOpenItem: (String) -> Unit,
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
        Box(modifier = Modifier.weight(1f)) {
            if (medicines.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    SbText(text = "هنوز دارویی ثبت نشده.", style = type.title)
                    Spacer(Modifier.height(space.sm))
                    SbText(
                        text = "داروهایت را با برنامه‌ی مصرف اضافه کن تا سر وقت یادآوری شوند.",
                        style = type.body,
                        color = colors.muted,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = medicines, key = { it.item.id }) { med ->
                        MedicineRow(
                            ui = med,
                            onLogDose = { onLogDose(med.item.id) },
                            onRefill = { onRefill(med.item.id) },
                            onOpen = { onOpenItem(med.item.id) },
                        )
                        SbHairline()
                    }
                }
            }
        }

        if (showAdd) {
            AddSheet(
                onDismiss = { showAdd = false },
                onCreate = { name, dosage, dosesPerDay, stock, refillAt ->
                    onCreate(name, dosage, dosesPerDay, stock, refillAt)
                    showAdd = false
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
                SbText(text = "افزودن دارو", style = type.label, color = colors.onAccent)
            }
        }
        Spacer(Modifier.height(space.md))
    }
}

@Composable
private fun MedicineRow(ui: MedicineUi, onLogDose: () -> Unit, onRefill: () -> Unit, onOpen: () -> Unit) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
            SbText(text = ui.item.content, style = type.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (ui.details.dosage.isNotBlank()) {
                Spacer(Modifier.height(space.xs))
                SbText(text = ui.details.dosage, style = type.body, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ui.nextDose?.let { next ->
                    SbText(text = "دوز بعدی ${JalaliDate.formatTime(next)}", style = type.monoSmall, color = colors.accent)
                    Spacer(Modifier.width(space.sm))
                }
                SbText(
                    text = "${ui.details.stock.toString().toPersianDigits()} دوز مانده",
                    style = type.monoSmall,
                    color = if (ui.details.needsRefill) colors.accentSecondary else colors.muted,
                )
                if (ui.streak > 0) {
                    Spacer(Modifier.width(space.sm))
                    SbText(text = "${ui.streak.toString().toPersianDigits()} روز", style = type.monoSmall, color = colors.muted)
                }
            }
            if (ui.details.needsRefill) {
                Spacer(Modifier.height(space.xs))
                Box(
                    modifier = Modifier
                        .clip(SecondBrainTheme.shapes.small)
                        .background(colors.accentSecondary)
                        .clickable(onClick = onRefill)
                        .padding(horizontal = space.md, vertical = space.xs),
                ) {
                    SbText(
                        text = if (ui.details.isOut) "تمام شد — تمدید" else "نیاز به تمدید",
                        style = type.monoSmall,
                        color = colors.onAccent,
                    )
                }
            }
        }

        Spacer(Modifier.width(space.md))
        val canTake = !ui.takenToday
        Box(
            modifier = Modifier
                .clip(SecondBrainTheme.shapes.medium)
                .background(if (canTake) colors.accent else colors.hairline)
                .clickable(enabled = canTake, onClick = onLogDose)
                .padding(horizontal = space.lg, vertical = space.sm),
        ) {
            SbText(
                text = if (ui.takenToday) "ثبت شد" else "مصرف کردم",
                style = type.label,
                color = if (canTake) colors.onAccent else colors.muted,
            )
        }
    }
}

@Composable
private fun AddSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, Int, Int) -> Unit,
) {
    val colors = SecondBrainTheme.colors
    val type = SecondBrainTheme.type
    val space = SecondBrainTheme.spacing

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var dosesPerDay by remember { mutableIntStateOf(1) }
    var stock by remember { mutableStateOf("") }
    var refillAt by remember { mutableStateOf("5") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SecondBrainTheme.shapes.large)
            .background(colors.surface)
            .padding(space.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SbText(text = "داروی جدید", style = type.bodyLarge)
            Spacer(Modifier.weight(1f))
            SbText(
                text = "بستن",
                style = type.label,
                color = colors.muted,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
        }
        Spacer(Modifier.height(space.md))

        Field(value = name, onValueChange = { name = it }, placeholder = "نام دارو…")
        Spacer(Modifier.height(space.sm))
        Field(value = dosage, onValueChange = { dosage = it }, placeholder = "مقدار مصرف (مثلاً ۱ قرص بعد از غذا)")

        Spacer(Modifier.height(space.md))
        SbText(text = "دفعات در روز", style = type.caption, color = colors.muted)
        Spacer(Modifier.height(space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            (1..4).forEach { count ->
                Toggle(
                    label = count.toString().toPersianDigits(),
                    selected = dosesPerDay == count,
                    onClick = { dosesPerDay = count },
                )
            }
        }

        Spacer(Modifier.height(space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            Field(
                value = stock,
                onValueChange = { stock = it.filter { c -> c.isDigit() } },
                placeholder = "موجودی (دوز)",
                numeric = true,
                modifier = Modifier.weight(1f),
            )
            Field(
                value = refillAt,
                onValueChange = { refillAt = it.filter { c -> c.isDigit() } },
                placeholder = "هشدار تمدید در",
                numeric = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(space.lg))
        val canAdd = name.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SecondBrainTheme.shapes.medium)
                .background(if (canAdd) colors.accent else colors.hairline)
                .clickable(enabled = canAdd) {
                    onCreate(
                        name.trim(),
                        dosage.trim(),
                        dosesPerDay,
                        stock.toIntOrNull() ?: 0,
                        refillAt.toIntOrNull() ?: 5,
                    )
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
            .padding(horizontal = space.lg, vertical = space.sm),
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
