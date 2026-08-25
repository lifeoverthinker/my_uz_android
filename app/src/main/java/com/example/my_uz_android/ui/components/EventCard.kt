package com.example.my_uz_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.my_uz_android.data.models.EventEntity
import com.example.my_uz_android.ui.theme.extendedColors

// Karta pojedynczego wydarzenia na dashboardzie i liście
@Composable
fun EventCard(
    event: EventEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null
) {
    val cardBackgroundColor = backgroundColor ?: extendedColors.eventCardBackground
    val titleColor = MaterialTheme.colorScheme.onSurface
    val descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = titleColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = event.description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = descriptionColor
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
