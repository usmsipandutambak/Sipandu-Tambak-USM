package id.ac.usm.sipandutambak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.usm.sipandutambak.domain.model.DeviceStatus
import id.ac.usm.sipandutambak.ui.theme.*

@Composable
fun StatusBadge(
    status: DeviceStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        DeviceStatus.NORMAL -> Triple(StatusNormalBg, StatusNormal, "NORMAL")
        DeviceStatus.WASPADA -> Triple(StatusWaspadaBg, StatusWaspada, "WASPADA")
        DeviceStatus.BAHAYA -> Triple(StatusBahayaBg, StatusBahaya, "BAHAYA")
        DeviceStatus.OFFLINE -> Triple(StatusOfflineBg, StatusOffline, "OFFLINE")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}