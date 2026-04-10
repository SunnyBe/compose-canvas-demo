package com.sunday.composecanvasdemo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

private data class NodeUiModel(
    val id: Int,
    val position: Offset,
)

private const val NODE_RADIUS = 22f
private const val NODE_GLOW_RADIUS = 44f
private const val NODE_INNER_DOT_RADIUS = 7f
private const val NODE_SELECTED_RING_RADIUS = 34f
private const val NODE_HIT_RADIUS = 44f
private const val CONNECTION_DISTANCE = 280f
private const val GRID_STEP = 120f

@Composable
fun CanvasGraphDemoScreen(modifier: Modifier = Modifier) {
    val nodes = remember { mutableStateListOf<NodeUiModel>() }
    var nextId by remember { mutableIntStateOf(0) }
    var selectedNodeId by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08101F),
                        Color(0xFF0F1C35),
                    ),
                ),
            )
            .pointerInput(canvasSize, nodes) {
                detectTapGestures { tapOffset ->
                    val tappedNode = nodes.findNearestNode(
                        point = tapOffset,
                        maxDistance = NODE_HIT_RADIUS,
                    )

                    if (tappedNode == null) {
                        val boundedPoint = tapOffset.clampToBounds(
                            width = canvasSize.width,
                            height = canvasSize.height,
                            padding = NODE_GLOW_RADIUS,
                        )

                        nodes.add(
                            NodeUiModel(
                                id = nextId++,
                                position = boundedPoint,
                            ),
                        )
                    }
                }
            }
            .pointerInput(canvasSize, nodes) {
                detectDragGestures(
                    onDragStart = { touchPoint ->
                        selectedNodeId = nodes.findNearestNode(
                            point = touchPoint,
                            maxDistance = NODE_HIT_RADIUS,
                        )?.id
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val currentSelectedId = selectedNodeId ?: return@detectDragGestures
                        val index = nodes.indexOfFirst { it.id == currentSelectedId }
                        if (index == -1) return@detectDragGestures

                        val currentNode = nodes[index]
                        val movedPosition = currentNode.position + dragAmount
                        val boundedPosition = movedPosition.clampToBounds(
                            width = canvasSize.width,
                            height = canvasSize.height,
                            padding = NODE_GLOW_RADIUS,
                        )

                        nodes[index] = currentNode.copy(position = boundedPosition)
                    },
                    onDragEnd = {
                        selectedNodeId = null
                    },
                    onDragCancel = {
                        selectedNodeId = null
                    },
                )
            },
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                canvasSize = size

                drawGrid()

                for (i in 0 until nodes.size) {
                    for (j in i + 1 until nodes.size) {
                        val first = nodes[i]
                        val second = nodes[j]
                        val distance = first.position.distanceTo(second.position)

                        if (distance <= CONNECTION_DISTANCE) {
                            val normalized = 1f - (distance / CONNECTION_DISTANCE)
                            val alpha = 0.18f + (normalized * 0.72f)

                            drawLine(
                                color = Color(0xFF7CC7FF).copy(alpha = alpha),
                                start = first.position,
                                end = second.position,
                                strokeWidth = 6f,
                            )
                        }
                    }
                }

                nodes.forEach { node ->
                    val isSelected = node.id == selectedNodeId

                    drawCircle(
                        color = Color(0xFF7CC7FF).copy(alpha = if (isSelected) 0.24f else 0.16f),
                        radius = if (isSelected) NODE_GLOW_RADIUS + 6f else NODE_GLOW_RADIUS,
                        center = node.position,
                        blendMode = BlendMode.SrcOver,
                    )

                    drawCircle(
                        color = Color(0xFF4DA3FF),
                        radius = NODE_RADIUS,
                        center = node.position,
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = NODE_INNER_DOT_RADIUS,
                        center = node.position,
                    )

                    if (isSelected) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.75f),
                            radius = NODE_SELECTED_RING_RADIUS,
                            center = node.position,
                            style = Stroke(width = 4f),
                        )
                    }
                }
            },
        )

        InfoChip(
            text = "Tap empty space to add a node\nDrag a node to move it",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 20.dp),
        )

        InfoChip(
            text = "Nodes: ${nodes.size}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.28f),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.96f),
            ),
        )
    }
}

private fun DrawScope.drawGrid() {
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
        x += GRID_STEP
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += GRID_STEP
    }
}

private fun List<NodeUiModel>.findNearestNode(
    point: Offset,
    maxDistance: Float,
): NodeUiModel? {
    return this
        .map { node -> node to node.position.distanceTo(point) }
        .filter { (_, distance) -> distance <= maxDistance }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

private fun Offset.distanceTo(other: Offset): Float {
    return hypot(x - other.x, y - other.y)
}

private fun Offset.clampToBounds(
    width: Float,
    height: Float,
    padding: Float,
): Offset {
    val safeX = x.coerceIn(padding, width - padding)
    val safeY = y.coerceIn(padding, height - padding)
    return Offset(safeX, safeY)
}