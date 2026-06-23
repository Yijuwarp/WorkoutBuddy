package com.example.workoutbuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutbuddy.data.Equipment
import com.example.workoutbuddy.theme.BluePrimary
import com.example.workoutbuddy.theme.TextDark
import androidx.compose.foundation.Image

// Shared equipment row list (icon, name, toggle) reused by Profile > Settings and the
// onboarding equipment step, so the two stay in lockstep with no duplicated UI.
// textColor defaults to the light-surface color used in Profile > Settings; onboarding's dark
// background passes Color.White instead.
@Composable
fun EquipmentPickerList(
    selected: Set<Equipment>,
    onToggle: (Equipment, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = TextDark
) {
    LazyColumn(modifier = modifier) {
        items(Equipment.entries.toList(), key = { it.id }) { equipment ->
            EquipmentRow(
                equipment = equipment,
                isOwned = equipment in selected,
                onToggle = { onToggle(equipment, it) },
                textColor = textColor
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun EquipmentPickerColumn(
    selected: Set<Equipment>,
    onToggle: (Equipment, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = TextDark
) {
    Column(modifier = modifier) {
        Equipment.entries.forEach { equipment ->
            EquipmentRow(
                equipment = equipment,
                isOwned = equipment in selected,
                onToggle = { onToggle(equipment, it) },
                textColor = textColor
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun EquipmentRow(
    equipment: Equipment,
    isOwned: Boolean,
    onToggle: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = equipment.iconRes),
            contentDescription = equipment.displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(4.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = equipment.displayName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isOwned,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = BluePrimary)
        )
    }
}
