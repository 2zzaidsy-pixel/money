package com.paywise.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paywise.app.domain.model.FinancialHealthLevel
import com.paywise.app.ui.theme.*

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == PrimaryDark
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark)
                            listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
                        else
                            listOf(Color(0x1A000000), Color(0x0A000000))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 4.dp,
        tonalElevation = 1.dp
    ) {
        content()
    }
}

@Composable
fun AnimatedCircularProgress(
    percentage: Float,
    size: Dp = 120.dp,
    strokeWidth: Dp = 10.dp,
    progressColor: Color = PrimaryGreen,
    trackColor: Color = Color(0xFF2D333B),
    label: String = "",
    value: String = ""
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(percentage) {
        animatedProgress.animateTo(
            targetValue = percentage.coerceIn(0f, 1f),
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val radius = (size.toPx() - strokeWidth.toPx()) / 2
            val topLeft = Offset(
                (size.toPx() - radius * 2) / 2,
                (size.toPx() - radius * 2) / 2
            )

            drawCircle(
                color = trackColor,
                radius = radius,
                topLeft = topLeft,
                style = stroke
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun HealthScoreGauge(
    score: Int,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp
) {
    val level = when {
        score >= 80 -> FinancialHealthLevel.EXCELLENT
        score >= 60 -> FinancialHealthLevel.GOOD
        score >= 40 -> FinancialHealthLevel.WARNING
        else -> FinancialHealthLevel.RISK
    }

    val color = when (level) {
        FinancialHealthLevel.EXCELLENT -> PrimaryGreen
        FinancialHealthLevel.GOOD -> AccentBlue
        FinancialHealthLevel.WARNING -> AccentOrange
        FinancialHealthLevel.RISK -> AccentRed
    }

    val levelText = when (level) {
        FinancialHealthLevel.EXCELLENT -> "Excellent"
        FinancialHealthLevel.GOOD -> "Good"
        FinancialHealthLevel.WARNING -> "Warning"
        FinancialHealthLevel.RISK -> "Risk"
    }

    val animatedScore = remember { Animatable(0f) }

    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = tween(2000, easing = FastOutSlowInEasing)
        )
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val radius = (size.toPx() - strokeWidth.toPx()) / 2
            val topLeft = Offset(
                (size.toPx() - radius * 2) / 2,
                (size.toPx() - radius * 2) / 2
            )

            val sweepAngle = 360f * (animatedScore.value / 100f)

            // Track
            drawArc(
                color = Color(0xFF2D333B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = stroke.copy(width = strokeWidth.toPx() + 2.dp.toPx())
            )

            // Progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.value.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = levelText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun SalaryProgressBar(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(percentage) {
        animatedProgress.animateTo(
            targetValue = percentage,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    val barColor = when {
        percentage > 0.8f -> AccentRed
        percentage > 0.6f -> AccentOrange
        else -> PrimaryGreen
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2D333B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.value.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.7f))
                        )
                    )
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: @Composable () -> Unit = {},
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
    height: Dp = 20.dp
) {
    val shimmerColors = listOf(
        Color(0xFF1C2333).copy(alpha = 0.6f),
        Color(0xFF1C2333).copy(alpha = 0.2f),
        Color(0xFF1C2333).copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200, 0f),
        end = Offset(translateAnim.value, 0f)
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    ModernCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerEffect(width = 120.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerEffect(width = 200.dp, height = 32.dp)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerEffect(width = 150.dp, height = 12.dp)
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Home") },
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Receipt, contentDescription = "Expenses") },
            label = { Text("Expenses") },
            selected = currentRoute == "expenses",
            onClick = { onNavigate("expenses") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
            label = { Text("Reports") },
            selected = currentRoute == "reports",
            onClick = { onNavigate("reports") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = { onNavigate("settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f)
            )
        )
    }
}
