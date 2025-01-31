package composables

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.minus
import java.lang.Math.pow
import kotlin.math.*

@Composable
fun PiChart(
    modifier: Modifier = Modifier,
    values: Array<Float>,
    colors: Array<Color>
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier){
        val angles = ArrayList<Float>()
        val textSizes = ArrayList<IntSize>()
        var angle = 0f
        val sortedValues = values.sorted()
        for ((i, item) in sortedValues.withIndex()) {
            val textLayoutResult = textMeasurer.measure(text = AnnotatedString(item.toString()))
            val textSize = textLayoutResult.size
            textSizes.add(textSize)
            val sweep = 360 * findPercentageOf(values, item)
            drawArc(
                color = colors[i%colors.size],
                startAngle = angle,
                sweepAngle = sweep,
                useCenter = true
            )
            angles.add(angle)
            angle += sweep

        }
        val offsets = getTextPositions(angles, size.maxDimension/2, center, textSizes)
        for ((i, offset) in offsets.withIndex()) {
            if (offset != null)
                drawText(textMeasurer, sortedValues[i].toString(), topLeft = offset)
        }
    }
}

fun getTextPositions(angles: ArrayList<Float>, radius: Float, center: Offset, textSizes: ArrayList<IntSize>) : ArrayList<Offset?> {
    val offsets = ArrayList<Offset?>(angles.size)
    for ((i, angle) in angles.withIndex()) {
        offsets.add(null)
        val textSize = textSizes[i]
        var nextAngle = angles[(i+1)%angles.size]
        if (nextAngle == 0f)
            nextAngle = 360f
        val sweep = nextAngle - angle
        var midAngle = angle + (sweep)/2
        if (sweep < 10)
            continue
        var pointA : Array<Double>
        var pointB : Array<Double>
        if (sweep == 360f) {
            offsets[i] = center - (Offset(textSize.width.toFloat(), textSize.height.toFloat()) / 2F)
            continue
        }
        if (sweep > 90) {
            pointA = arrayOf(
                (cos((midAngle - 45) * (PI / 180.0)) + 1) * radius,
                (sin((midAngle - 45) * (PI / 180.0)) + 1) * radius
            )
            pointB = arrayOf(
                (cos((midAngle + 45) * (PI / 180.0)) + 1) * radius,
                (sin((midAngle + 45) * (PI / 180.0)) + 1) * radius
            )
        } else {
            pointA = arrayOf(
                (cos((angle) * (PI / 180.0)) + 1) * radius,
                (sin((angle) * (PI / 180.0)) + 1) * radius
            )
            pointB = arrayOf(
                (cos((nextAngle) * (PI / 180.0)) + 1) * radius,
                (sin((nextAngle) * (PI / 180.0)) + 1) * radius
            )
        }
        val pointBOffset = arrayOf(pointB[0] - center.x, pointB[1] - center.y)
        val a = sqrt(pointBOffset[0].pow(2) + pointBOffset[1].pow(2))
        val pointCOffset = arrayOf(pointA[0] - center.x, pointA[1] - center.y)
        val b = sqrt(pointCOffset[0].pow(2) + pointCOffset[1].pow(2))
        val pointAOffset = arrayOf(pointA[0] - pointB[0], pointA[1] - pointB[1])
        val c = sqrt(pointAOffset[0].pow(2) + pointAOffset[1].pow(2))

        val finalPoint = arrayOf(
            (a * pointA[0] + b * pointB[0] + c * center.x) / (a + b + c),
            (a * pointA[1] + b * pointB[1] + c * center.y) / (a + b + c),
        )

        offsets[i] = (Offset(
                (finalPoint[0] - textSize.width / 2).toFloat(),
                (finalPoint[1] - textSize.height / 2).toFloat()
            )
        )
    }
    return offsets
}

@Composable
fun PiChart(
    modifier: Modifier = Modifier,
    values: Array<Double>,
    colors: Array<Color>
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier){
        val angles = ArrayList<Float>()
        val textSizes = ArrayList<IntSize>()
        var angle = 0f
        val sortedValues = values.sorted()
        for ((i, item) in sortedValues.withIndex()) {
            val textLayoutResult = textMeasurer.measure(text = AnnotatedString(item.toString()))
            val textSize = textLayoutResult.size
            textSizes.add(textSize)
            val sweep = 360 * findPercentageOf(values, item.toFloat())
            drawArc(
                color = colors[i%colors.size],
                startAngle = angle,
                sweepAngle = sweep,
                useCenter = true
            )
            angle += sweep
        }
        val offsets = getTextPositions(angles, size.maxDimension/2, center, textSizes)
        for ((i, offset) in offsets.withIndex()) {
            if (offset != null)
                drawText(textMeasurer, sortedValues[i].toString(), topLeft = offset)
        }
    }
}

@Composable
fun PiChart(
    modifier: Modifier = Modifier,
    values: Array<Int>,
    colors: Array<Color>
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier){
        val angles = ArrayList<Float>()
        val textSizes = ArrayList<IntSize>()
        var angle = 0f
        val sortedValues = values.sorted()
        for ((i, item) in sortedValues.withIndex()) {
            val textLayoutResult = textMeasurer.measure(text = AnnotatedString(item.toString()))
            val textSize = textLayoutResult.size
            textSizes.add(textSize)
            val sweep = 360 * findPercentageOf(values , item.toFloat())
            drawArc(
                color = colors[i%colors.size],
                startAngle = angle,
                sweepAngle = sweep,
                useCenter = true
            )
            angle += sweep
        }
        val offsets = getTextPositions(angles, size.maxDimension/2, center, textSizes)
        for ((i, offset) in offsets.withIndex()) {
            if (offset != null)
                drawText(textMeasurer, sortedValues[i].toString(), topLeft = offset)
        }
    }
}

private fun findPercentageOf(array: Array<Float>, i: Float) : Float {
    var total = 0f
    for (item in array) {
        total += item
    }
    return i / total
}

private fun findPercentageOf(array: Array<Double>, i: Float) : Float {
    var total = 0f
    for (item in array) {
        total += item.toFloat()
    }
    return i / total
}

private fun findPercentageOf(array: Array<Int>, i: Float) : Float {
    var total = 0f
    for (item in array) {
        total += item
    }
    return i / total
}
