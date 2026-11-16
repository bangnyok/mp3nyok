package com.bangnyok.mp3nyok.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palet warna klasik Winamp. */
object Wa {
    val Bg = Color(0xFF1E1E1E)
    val BgDark = Color(0xFF121212)
    val Panel = Color(0xFF2B2B2B)
    val PanelHi = Color(0xFF454545)
    val Text = Color(0xFFC8C8C8)
    val TextDim = Color(0xFF8A8A8A)
    val Green = Color(0xFF00C800)
    val Yellow = Color(0xFFE8E800)
    val LedRed = Color(0xFFFF3B3B)
    val Orange = Color(0xFFFFA500)
    val SelectBlue = Color(0xFF0000A8)
    val Border = Color(0xFF4A4A4A)
}

@Composable
fun TitleBar(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Wa.Panel)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Wa.Text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
fun WaButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        active -> Color(0xFF3A2A00)
        enabled -> Wa.Panel
        else -> Color(0xFF242424)
    }
    val fg = when {
        active -> Wa.Orange
        enabled -> Wa.Text
        else -> Color(0xFF666666)
    }
    Box(
        modifier
            .border(1.dp, if (enabled) Wa.Border else Color(0xFF333333))
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun WaIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val bg = when {
        active -> Color(0xFF3A2A00)
        enabled -> Wa.Panel
        else -> Color(0xFF242424)
    }
    val tint = when {
        !enabled -> Color(0xFF666666)
        active -> Wa.Orange
        else -> Color(0xFFD9D9D9)
    }
    Box(
        modifier
            .border(1.dp, if (enabled) Wa.Border else Color(0xFF333333))
            .background(bg)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun WaDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            color = Wa.BgDark,
            border = BorderStroke(1.dp, Wa.Border),
            shape = RoundedCornerShape(3.dp)
        ) {
            Column(
                Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TitleBar(title)
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
fun darkFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF0E0E0E),
    unfocusedContainerColor = Color(0xFF0E0E0E),
    disabledContainerColor = Color(0xFF0E0E0E),
    focusedIndicatorColor = Wa.Border,
    unfocusedIndicatorColor = Color(0xFF333333),
    disabledIndicatorColor = Color(0xFF333333),
    cursorColor = Wa.Green,
    focusedTextColor = Wa.Green,
    unfocusedTextColor = Wa.Green
)

@Composable
fun waSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Wa.Green,
    activeTrackColor = Wa.Green,
    inactiveTrackColor = Color(0xFF3C3C3C)
)

fun fmtTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

fun fmtDur(sec: Double?): String {
    if (sec == null || sec <= 0.0) return "--:--"
    val s = sec.toLong()
    return "%d:%02d".format(s / 60, s % 60)
}
