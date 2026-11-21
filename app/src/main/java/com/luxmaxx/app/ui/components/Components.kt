package com.luxmaxx.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luxmaxx.app.ui.theme.Amber
import kotlin.math.roundToInt

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun MacroBar(label: String, value: Int, target: Int, unit: String, color: Color = Amber) {
    val frac = if (target > 0) value.toFloat() / target else 0f
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value / $target $unit", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { frac.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.replace(',', '.').filter { it.isDigit() || (decimal && it == '.') }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit, suffix: String = "") {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label)
            val rounded = (value * 2).roundToInt() / 2f
            val shown = if (rounded % 1f == 0f) rounded.roundToInt().toString() else "%.1f".format(rounded)
            Text("$shown$suffix", color = Amber, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** Простой линейный график без сторонних библиотек. */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier, color: Color = Amber) {
    if (values.size < 2) {
        Text("Нужно минимум 2 записи для графика", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        return
    }
    val min = values.min(); val max = values.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    Column(modifier) {
        Text("%.1f".format(max), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(120.dp).padding(vertical = 4.dp)) {
            val stepX = size.width / (values.size - 1)
            fun pt(i: Int) = Offset(i * stepX, (size.height * (1 - (values[i] - min) / span)).toFloat())
            val path = Path().apply {
                moveTo(pt(0).x, pt(0).y)
                for (i in 1 until values.size) lineTo(pt(i).x, pt(i).y)
            }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            for (i in values.indices) drawCircle(color, 3.5.dp.toPx(), pt(i))
        }
        Text("%.1f".format(min), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun Double.fmt(): String = if (this % 1.0 == 0.0) this.toInt().toString() else "%.1f".format(this)
