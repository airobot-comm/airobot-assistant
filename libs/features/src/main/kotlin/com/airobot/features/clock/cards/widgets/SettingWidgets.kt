package com.airobot.features.clock.cards.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * Standardized inline text field for list item settings.
 * Uses BasicTextField for ultra-compact layout with tight baseline underline.
 */
@Composable
fun AiSettingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    accentColor: Color,
    fontSize: TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    isCenter: Boolean = false,
    showUnderline: Boolean = true
) {
    val underlineModifier = if (showUnderline) {
        Modifier
            .drawBehind {
                val y = size.height - 2.dp.toPx()
                drawLine(
                    color = accentColor.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(bottom = 2.dp)
    } else {
        Modifier
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(underlineModifier),
        textStyle = LocalTextStyle.current.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = RobotTheme.colors.textPrimary,
            textAlign = if (isCenter) TextAlign.Center else TextAlign.Start
        ),
        singleLine = true,
        cursorBrush = SolidColor(accentColor),
        decorationBox = { innerTextField ->
            Box(contentAlignment = if (isCenter) Alignment.Center else Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = RobotTheme.colors.textMuted.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * Standardized generic dropdown selector.
 * Ultra-tight layout merging label and value into a single component.
 */
@Composable
fun <T> AiSettingDropdown(
    label: String,
    selectedValueLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = RobotTheme.colors.textMuted
        )
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = selectedValueLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RobotTheme.colors.textPrimary
                )
                Text(
                    text = "▾",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RobotTheme.colors.textMuted
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemLabel(item)) },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Standardized switch toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = RobotTheme.isDark
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RobotTheme.colors.cardBg,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = RobotTheme.colors.cardBg,
                uncheckedTrackColor = RobotTheme.colors.textMuted.copy(alpha = 0.3f)
            ),
            modifier = modifier.height(24.dp)
        )
    }
}


/**
 * Standardized segmented button group.
 *
 * @param compact When true, renders as small inline pills (matches prototype's inline voice mode chips).
 *                Compact chips: 24dp height, smaller font, no border for unselected, filled for selected.
 */
@Composable
fun AiSettingSegmentedButton(
    options: List<Pair<String, String>>, // Pair<Id, Label>
    selectedId: String,
    onSelect: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (id, label) ->
            val isSelected = selectedId == id
            Button(
                onClick = { onSelect(id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = if (compact) 0.15f else 0.2f)
                    else Color.Transparent,
                    contentColor = if (isSelected) accentColor else RobotTheme.colors.textMuted
                ),
                modifier = if (compact) Modifier.height(24.dp)
                else Modifier
                    .weight(1f)
                    .height(32.dp),
                contentPadding = PaddingValues(
                    horizontal = if (compact) 8.dp else 0.dp,
                    vertical = 0.dp
                ),
                shape = RoundedCornerShape(if (compact) 12.dp else 8.dp),
                border = if (compact && !isSelected) null
                else BorderStroke(
                    1.dp,
                    if (isSelected) accentColor.copy(alpha = 0.4f)
                    else RobotTheme.colors.textMuted.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = label,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Standardized settings gear button.
 * Active state: solid accent fill with white icon (matches prototype's bg-orange-600 text-white).
 */
@Composable
fun AiSettingGearButton(
    isEditing: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    icon: ImageVector
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isEditing) accentColor else Color.Transparent
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "settings",
            modifier = Modifier.size(18.dp),
            tint = if (isEditing) Color.White else RobotTheme.colors.textMuted
        )
    }
}

/**
 * Custom toggle button (like "Require Name")
 */
@Composable
fun AiSettingToggleButton(
    label: String,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    activeColor: Color = Color(0xFF818CF8),
    activeBgColor: Color = Color(0xFF6366F1),
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onToggle(!selected) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) activeBgColor.copy(alpha = 0.15f) else Color.Transparent,
            contentColor = if (selected) activeColor else RobotTheme.colors.textMuted
        ),
        modifier = modifier.height(30.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) activeBgColor.copy(alpha = 0.4f) else RobotTheme.colors.textMuted.copy(
                alpha = 0.2f
            )
        )
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
