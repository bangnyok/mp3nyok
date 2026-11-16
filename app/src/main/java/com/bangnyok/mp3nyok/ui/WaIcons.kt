package com.bangnyok.mp3nyok.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Transport icons gaya Winamp klasik (terlukis sebagai vector, tanpa asset).
 */
object WaIcons {

    private val ink = SolidColor(Color(0xFFD9D9D9))

    val Prev: ImageVector = ImageVector.Builder(
        name = "WaPrev", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = ink) {
        moveTo(4f, 6f); lineTo(6f, 6f); lineTo(6f, 18f); lineTo(4f, 18f); close()
        moveTo(13f, 6f); lineTo(13f, 18f); lineTo(7.5f, 12f); close()
        moveTo(19f, 6f); lineTo(19f, 18f); lineTo(13.5f, 12f); close()
    }.build()

    val Next: ImageVector = ImageVector.Builder(
        name = "WaNext", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = ink) {
        moveTo(18f, 6f); lineTo(20f, 6f); lineTo(20f, 18f); lineTo(18f, 18f); close()
        moveTo(11f, 6f); lineTo(11f, 18f); lineTo(16.5f, 12f); close()
        moveTo(5f, 6f); lineTo(5f, 18f); lineTo(10.5f, 12f); close()
    }.build()

    val Play: ImageVector = ImageVector.Builder(
        name = "WaPlay", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = ink) {
        moveTo(8f, 5f); lineTo(8f, 19f); lineTo(19f, 12f); close()
    }.build()

    val Pause: ImageVector = ImageVector.Builder(
        name = "WaPause", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = ink) {
        moveTo(7f, 6f); lineTo(10f, 6f); lineTo(10f, 18f); lineTo(7f, 18f); close()
        moveTo(14f, 6f); lineTo(17f, 6f); lineTo(17f, 18f); lineTo(14f, 18f); close()
    }.build()

    val Stop: ImageVector = ImageVector.Builder(
        name = "WaStop", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = ink) {
        moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close()
    }.build()

    val Shuffle: ImageVector = ImageVector.Builder(
        name = "WaShuffle", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = ink, strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 7f); lineTo(8.5f, 7f); lineTo(17f, 17f); lineTo(20f, 17f)
        moveTo(4f, 17f); lineTo(8.5f, 17f); lineTo(17f, 7f); lineTo(20f, 7f)
    }.path(fill = ink) {
        moveTo(18.5f, 4.6f); lineTo(21.5f, 7f); lineTo(18.5f, 9.4f); close()
        moveTo(18.5f, 14.6f); lineTo(21.5f, 17f); lineTo(18.5f, 19.4f); close()
    }.build()

    val Repeat: ImageVector = ImageVector.Builder(
        name = "WaRepeat", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(
        stroke = ink, strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(5f, 11.5f); lineTo(5f, 10f)
        curveTo(5f, 7.2f, 7.2f, 5f, 10f, 5f)
        lineTo(16f, 5f)
        moveTo(19f, 12.5f); lineTo(19f, 14f)
        curveTo(19f, 16.8f, 16.8f, 19f, 14f, 19f)
        lineTo(8f, 19f)
    }.path(fill = ink) {
        moveTo(15f, 2.6f); lineTo(18.4f, 5f); lineTo(15f, 7.4f); close()
        moveTo(9f, 16.6f); lineTo(5.6f, 19f); lineTo(9f, 21.4f); close()
    }.build()

    val Gear: ImageVector = ImageVector.Builder(
        name = "WaGear", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = ink, pathFillType = PathFillType.EvenOdd) {
        // cincin luar
        moveTo(18.2f, 12f)
        arcToRelative(6.2f, 6.2f, 0f, true, false, -12.4f, 0f)
        arcToRelative(6.2f, 6.2f, 0f, true, false, 12.4f, 0f)
        close()
        // lubang tengah
        moveTo(15.4f, 12f)
        arcToRelative(3.4f, 3.4f, 0f, true, false, -6.8f, 0f)
        arcToRelative(3.4f, 3.4f, 0f, true, false, 6.8f, 0f)
        close()
    }.path(fill = ink) {
        // gigi: 4 petak + 4 wajik pepenjuru
        moveTo(10.8f, 3.4f); lineTo(13.2f, 3.4f); lineTo(13.2f, 7f); lineTo(10.8f, 7f); close()
        moveTo(10.8f, 17f); lineTo(13.2f, 17f); lineTo(13.2f, 20.6f); lineTo(10.8f, 20.6f); close()
        moveTo(3.4f, 10.8f); lineTo(7f, 10.8f); lineTo(7f, 13.2f); lineTo(3.4f, 13.2f); close()
        moveTo(17f, 10.8f); lineTo(20.6f, 10.8f); lineTo(20.6f, 13.2f); lineTo(17f, 13.2f); close()
        moveTo(16.9f, 5.7f); lineTo(18.3f, 7.1f); lineTo(16.9f, 8.5f); lineTo(15.5f, 7.1f); close()
        moveTo(16.9f, 15.5f); lineTo(18.3f, 16.9f); lineTo(16.9f, 18.3f); lineTo(15.5f, 16.9f); close()
        moveTo(7.1f, 15.5f); lineTo(8.5f, 16.9f); lineTo(7.1f, 18.3f); lineTo(5.7f, 16.9f); close()
        moveTo(7.1f, 5.7f); lineTo(8.5f, 7.1f); lineTo(7.1f, 8.5f); lineTo(5.7f, 7.1f); close()
    }.build()
}
