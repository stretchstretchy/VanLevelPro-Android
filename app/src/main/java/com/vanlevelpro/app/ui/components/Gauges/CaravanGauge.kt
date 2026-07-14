package com.vanlevelpro.app.ui.components.gauges

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Path

@Composable
fun CaravanGauge(
    angle: Float,
    imageRes: Int,
    modifier: Modifier = Modifier,
    tolerance: Float = 0.5f
) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle.coerceIn(-30f, 30f),
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "Pointer"
    )

    val pointerColour by animateColorAsState(
        targetValue = when {
            kotlin.math.abs(angle) <= tolerance -> Color(0xFF4CAF50)
            kotlin.math.abs(angle) <= 2f -> Color(0xFFFFB300)
            else -> Color(0xFFE53935)
        },
        label = "PointerColour"
    )

    Box(

        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val radius = size.minDimension / 2f - 18.dp.toPx()

            drawCircle(
                color = Color(0xFF606060),
                radius = radius,
                style = Stroke(5.dp.toPx())
            )

            for (deg in -30..30 step 5) {


                val major = deg % 10 == 0
                val zeroMark = deg == 0

                val radians = Math.toRadians((deg * 2.0))

                val innerRadius = radius + 2.dp.toPx()

                val outerRadius =
                    radius + when {
                        zeroMark -> 24.dp.toPx()
                        major -> 18.dp.toPx()
                        else -> 10.dp.toPx()
                    }


                val inner = Offset(
                    center.x + cos(radians).toFloat() * innerRadius,
                    center.y + sin(radians).toFloat() * innerRadius
                )

                val outer = Offset(
                    center.x + cos(radians).toFloat() * outerRadius,
                    center.y + sin(radians).toFloat() * outerRadius
                )

                drawLine(
                    color = Color.DarkGray,
                    start = inner,
                    end = outer,
                    strokeWidth = when {
                        zeroMark -> 4.dp.toPx()
                        major -> 3.dp.toPx()
                        else -> 1.5.dp.toPx()
                    },
                    cap = StrokeCap.Round
                )

            }
            // Animated pointer

            val pointerAngle = Math.toRadians((animatedAngle * 2.0).toDouble())

            val pointerInner = Offset(
                center.x + cos(pointerAngle).toFloat() * (radius + 4.dp.toPx()),
                center.y + sin(pointerAngle).toFloat() * (radius + 4.dp.toPx())
            )

            val triangleLength = 20.dp.toPx()
            val triangleWidth = 7.dp.toPx()

            val angleRad = pointerAngle.toFloat()

// Base of the triangle
            val pointerBase = Offset(
                center.x + cos(pointerAngle).toFloat() * (radius + 10.dp.toPx()),
                center.y + sin(pointerAngle).toFloat() * (radius + 10.dp.toPx())
            )

// Tip of the triangle
            val tip = Offset(
                center.x + cos(pointerAngle).toFloat() * (radius + 30.dp.toPx()),
                center.y + sin(pointerAngle).toFloat() * (radius + 30.dp.toPx())
            )

// Draw the pointer line to the base only
            drawLine(
                color = pointerColour,
                start = pointerInner,
                end = pointerBase,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            val left = Offset(
                pointerBase.x - sin(angleRad) * triangleWidth,
                pointerBase.y + cos(angleRad) * triangleWidth
            )

            val right = Offset(
                pointerBase.x + sin(angleRad) * triangleWidth,
                pointerBase.y - cos(angleRad) * triangleWidth
            )

            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(left.x, left.y)
                    lineTo(right.x, right.y)
                    close()
                },
                color = pointerColour
            )
        }

            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = if (imageRes == com.vanlevelpro.app.R.drawable.caravan_side) {
                    Modifier.size(width = 230.dp, height = 190.dp)
                } else {
                    Modifier.size(200.dp)
                },
                contentScale = ContentScale.Fit
            )
        }
    }
