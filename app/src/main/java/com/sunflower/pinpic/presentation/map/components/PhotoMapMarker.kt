package com.sunflower.pinpic.presentation.map.components

import android.util.Log
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.sunflower.pinpic.domain.map.ClusterNode
import com.sunflower.pinpic.presentation.map.MapViewModel

// Circle "head" of the pin
private val HEAD_SIZE = 40.dp

// How far the tail tapers down below the head, to a sharp point
private val TAIL_LENGTH = 8.dp

// Extra room below the tip for the static ring decoration
private val RING_PADDING = 12.dp

private val MARKER_HEIGHT = HEAD_SIZE + TAIL_LENGTH + RING_PADDING

// Where the actual GPS point (pin tip) sits, as a fraction of MARKER_HEIGHT,
// so the marker anchor lines up with the visual tip and not the box bottom.
private val TIP_FRACTION = (HEAD_SIZE + TAIL_LENGTH) / MARKER_HEIGHT

@Composable
fun PhotoMapMarker(
    node: ClusterNode,
    viewModel: MapViewModel,
    onClick: () -> Unit,
    color: Color
) {
    Log.d("PhotoMapMarker", node.thumbnailUrl.toString())
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(node.thumbnailUrl)
            .size(152)
            .crossfade(false)
            .allowHardware(false)
            .build()
    )
    val markerState = remember(node.id) {
        MarkerState(position = LatLng(node.lat, node.lon))
    }

    MarkerComposable(
        keys = arrayOf<Any>(
            node.id,
            node.thumbnailUrl ?: "",
            painter.state is AsyncImagePainter.State.Success,
            node.isCluster
        ),
        state = markerState,
        onClick = {
            onClick()
            true
        },
        anchor = Offset(0.5f, TIP_FRACTION)
    ) {
//        CanvasPinMarkerContent(
        PinMarkerContent(
            painter = painter,
            isCluster = node.isCluster,
            count = node.count,
            color = color
        )

    }
}

@Composable
fun PinMarkerContent(
    painter: AsyncImagePainter,
    isCluster: Boolean,
    count: Int,
    hasPhoto: Boolean = true,
    color: Color
) {
    Box(
        modifier = Modifier.size(width = HEAD_SIZE, height = MARKER_HEIGHT),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val headDiameter = HEAD_SIZE.toPx()
            val headRadius = headDiameter / 2f
            val centerX = headDiameter / 2f
            val centerY = headRadius // center of the circular head
            val tipY = headDiameter + TAIL_LENGTH.toPx() // sharp point of the pin
//            val green = Color(0xFF1EA053)

            // Static concentric rings around the tip (no animation)
            val ringCenter = Offset(centerX, tipY)
            drawCircle(
                color = color.copy(alpha = 0.45f),
                radius = 4.dp.toPx(),
                center = ringCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 8.dp.toPx(),
                center = ringCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 12.dp.toPx(),
                center = ringCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // Tail: a triangle whose base is embedded inside the circle head
            // (so there's no seam) and whose apex is the sharp bottom tip.
            val tailHalfWidth = headRadius * 0.85f
            val tailPath = Path().apply {
                moveTo(centerX - tailHalfWidth, centerY)
                lineTo(centerX + tailHalfWidth, centerY)
                lineTo(centerX, tipY)
                close()
            }
            drawPath(path = tailPath, color = color)

            // Circular head, drawn on top so the triangle-circle join is clean
            drawCircle(
                color = color,
                radius = headRadius,
                center = Offset(centerX, centerY)
            )

            // Small solid dot marking the exact tip / GPS point
            drawCircle(
                color = color,
                radius = 2.5.dp.toPx(),
                center = ringCenter
            )

            // Inner dark circle background for the photo
            drawCircle(
                color = color,
                radius = headRadius - 4.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Dummy photo",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        if (hasPhoto) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }

    }
}

@Composable
fun PinMarkerContent2(
    painter: AsyncImagePainter,
    isCluster: Boolean,
    count: Int,
    hasPhoto: Boolean = true,
    color: Color
) {
    Box(
        modifier = Modifier.size(width = HEAD_SIZE, height = MARKER_HEIGHT),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val headDiameter = HEAD_SIZE.toPx()
            val headRadius = headDiameter / 2f
            val centerX = headDiameter / 2f
            val centerY = headRadius // center of the circular head
            val tipY = headDiameter + TAIL_LENGTH.toPx() // sharp point of the pin
//            val green = Color(0xFF1EA053)

            // Static concentric rings around the tip (no animation)
            val ringCenter = Offset(centerX, tipY)
            drawCircle(
                color = color.copy(alpha = 0.45f),
                radius = 4.dp.toPx(),
                center = ringCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 8.dp.toPx(),
                center = ringCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 12.dp.toPx(),
                center = ringCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // Tail: a triangle whose base is embedded inside the circle head
            // (so there's no seam) and whose apex is the sharp bottom tip.
            val tailHalfWidth = headRadius * 0.85f
            val tailPath = Path().apply {
                moveTo(centerX - tailHalfWidth, centerY)
                lineTo(centerX + tailHalfWidth, centerY)
                lineTo(centerX, tipY)
                close()
            }
            drawPath(path = tailPath, color = color)

            // Circular head, drawn on top so the triangle-circle join is clean
            drawCircle(
                color = color,
                radius = headRadius,
                center = Offset(centerX, centerY)
            )

            // Draw the circular photo or a blank inner.
            val innerRadius = (size.width / 2) - with(density) {
                3.dp.toPx()
            }

            if (!isCluster && painter.state is AsyncImagePainter.State.Success) {
                clipPath(Path().apply {
                    addOval(
                        Rect(
                            center = Offset(size.width / 2, size.width / 2),
                            radius = innerRadius
                        )
                    )
                }) {
                    with(painter) {
                        draw(
                            size = Size(innerRadius * 2, innerRadius * 2),
                            alpha = 1.0f,
                            colorFilter = null
                        )
                    }
                }
            } else {
                drawCircle(
                    color = Color.White,
                    radius = innerRadius,
                    center = Offset(size.width / 2, size.width / 2)
                )
            }
        }
    }
}

@Composable
fun CanvasPinMarkerContent(
    painter: AsyncImagePainter,
    isCluster: Boolean,
    count: Int,
    color: Color
) {
    val density = LocalDensity.current
    val headSizePx = with(density) { HEAD_SIZE.toPx() }
    val tailLengthPx = with(density) { TAIL_LENGTH.toPx() }

    // Define pulse animation (only when count > 1)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = headSizePx * 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .width(HEAD_SIZE)
            .height(MARKER_HEIGHT)
    ) {
        Canvas(
            modifier = Modifier
                .width(HEAD_SIZE)
                .height(HEAD_SIZE + TAIL_LENGTH)
        ) {
            // 1) Draw Pulse Effect if it's a cluster
            if (isCluster) {
                drawCircle(
                    color = color.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(size.width / 2, size.width / 2)
                )
            }

            // 2) Draw the main Teardrop shape
            val teardropPath = Path().apply {
                val radius = size.width / 2
                val centerX = size.width / 2
                val centerY = size.width / 2

                // Start from the bottom tip
                moveTo(centerX, size.height)

                // Draw curves up to the circular head
                val controlY = size.height - (tailLengthPx * 0.5f)
                quadraticTo(
                    centerX - (radius * 0.5f), controlY,
                    centerX - radius, centerY
                )

                // Draw the circular top (from left, up to top, down to right)
                arcTo(
                    rect = Rect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.width
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )

                // Curve back down to the tip
                quadraticTo(
                    centerX + (radius * 0.5f), controlY,
                    centerX, size.height
                )

                close()
            }

            // Fill the teardrop shape
            drawPath(
                path = teardropPath,
                color = color
            )

            // 3) Draw the circular photo or a blank inner circle
            val innerRadius = (size.width / 2) - with(density) { 3.dp.toPx() }

            if (!isCluster && painter.state is AsyncImagePainter.State.Success) {
                clipPath(
                    Path().apply {
                        addOval(
                            Rect(
                                center = Offset(size.width / 2, size.width / 2),
                                radius = innerRadius
                            )
                        )
                    }
                ) {
                    with(painter) {
                        draw(
                            size = Size(innerRadius * 2, innerRadius * 2),
                            alpha = 1.0f,
                            colorFilter = null
                        )
                    }
                }
            } else {
                // Blank inner circle placeholder
                drawCircle(
                    color = Color.White,
                    radius = innerRadius,
                    center = Offset(size.width / 2, size.width / 2)
                )
            }
        }

        // 4) Draw the Cluster Count Badge (if it's a cluster)
        if (isCluster) {
            Box(
                modifier = Modifier
                    .offset(y = 12.dp)
                    .background(Color.Transparent, shape = CircleShape)
            ) {
                Text(
                    text = count.toString(),
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
