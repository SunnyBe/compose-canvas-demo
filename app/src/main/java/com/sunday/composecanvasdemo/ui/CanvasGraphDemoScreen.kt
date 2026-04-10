package com.sunday.composecanvasdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.hypot

private data class NodeUiModel(
    val id: Int,
    val position: Offset,
)

@Composable
fun CanvasGraphDemoScreen(modifier: Modifier = Modifier) {
    val nodes = remember { mutableStateListOf<NodeUiModel>() }
    var nextId by remember { mutableIntStateOf(0) }
    var selectedNodeId by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1020),
                        Color(0xFF111A33),
                    ),
                ),
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { touchPoint ->
                        val touchedNode = nodes.findNearestNode(
                            point = touchPoint,
                            maxDistance = 80f,
                        )

                        if (touchedNode != null) {
                            selectedNodeId = touchedNode.id
                        } else {
                            nodes.add(
                                NodeUiModel(
                                    id = nextId++,
                                    position = touchPoint,
                                ),
                            )
                            selectedNodeId = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val currentSelectedId = selectedNodeId ?: return@detectDragGestures
                        val index = nodes.indexOfFirst { it.id == currentSelectedId }
                        if (index == -1) return@detectDragGestures

                        val currentNode = nodes[index]
                        nodes[index] = currentNode.copy(
                            position = currentNode.position + dragAmount,
                        )
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val connectionDistance = 220f

            for (i in 0 until nodes.size) {
                for (j in i + 1 until nodes.size) {
                    val first = nodes[i]
                    val second = nodes[j]
                    val distance = first.position.distanceTo(second.position)

                    if (distance <= connectionDistance) {
                        val alpha = 1f - (distance / connectionDistance)
                        drawLine(
                            color = Color(0xFF7CC7FF).copy(alpha = alpha * 0.9f),
                            start = first.position,
                            end = second.position,
                            strokeWidth = 4f,
                        )
                    }
                }
            }

            nodes.forEach { node ->
                val isSelected = node.id == selectedNodeId

                drawCircle(
                    color = Color(0xFF7CC7FF).copy(alpha = 0.18f),
                    radius = if (isSelected) 42f else 34f,
                    center = node.position,
                )

                drawCircle(
                    color = Color(0xFF4DA3FF),
                    radius = 20f,
                    center = node.position,
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 8f,
                    center = node.position,
                )

                if (isSelected) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = 30f,
                        center = node.position,
                        style = Stroke(width = 3f),
                    )
                }
            }
        }

        BasicText(
            text = "Tap empty space to add a node. Drag a node to move it.",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            style = TextStyle(
                color = Color.White.copy(alpha = 0.9f),
            ),
        )

        BasicText(
            text = "Nodes: ${nodes.size}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            style = TextStyle(
                color = Color.White.copy(alpha = 0.85f),
            ),
        )
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