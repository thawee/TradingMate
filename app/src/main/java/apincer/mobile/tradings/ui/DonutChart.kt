package apincer.mobile.tradings.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    centerText: String = "",
    centerSubText: String = ""
) {
    val total = values.sum()
    val proportions = if (total > 0f) values.map { it / total } else values.map { 1f / values.size }
    
    val sweepAngles = proportions.map { 360f * it }
    
    // Animation for drawing the chart
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(values) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            
            // Ensure we always draw a perfect circle even if the container is stretched
            val minDimension = minOf(size.width, size.height)
            val strokeWidth = minDimension * 0.15f // Responsive stroke width (15% of size)
            
            val drawSize = Size(minDimension - strokeWidth, minDimension - strokeWidth)
            val topLeft = Offset(
                x = (size.width - minDimension + strokeWidth) / 2f,
                y = (size.height - minDimension + strokeWidth) / 2f
            )
            
            // Draw background track
            drawArc(
                color = Color.DarkGray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = drawSize,
                style = Stroke(width = strokeWidth)
            )

            for (i in sweepAngles.indices) {
                val sweep = sweepAngles[i] * animationProgress.value
                val color = colors.getOrElse(i) { Color.Gray }
                
                if (sweep > 0) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = drawSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                startAngle += sweepAngles[i] // Always increment by the final sweep so next arc starts correctly
            }
        }
        
        if (centerText.isNotEmpty() || centerSubText.isNotEmpty()) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (centerText.isNotEmpty()) {
                    Text(
                        text = centerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (centerSubText.isNotEmpty()) {
                    Text(
                        text = centerSubText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
