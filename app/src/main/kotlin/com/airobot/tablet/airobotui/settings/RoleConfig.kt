package com.airobot.tablet.airobotui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.comp.ConfigTextField
import com.airobot.framework.theme.RobotTheme

/**
 * 瑙掕壊绠＄悊閰嶇疆椤甸潰
 * 灞曠ず褰撳墠瑙掕壊鐨勫舰璞℃ā鍨嬨€佽闊虫ā鍨嬨€佸敜閱掕瘝绛夊浐瀹氶厤缃?
 */
@Composable
fun RoleConfig() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 鈹€鈹€ 褰㈣薄妯″瀷 鈹€鈹€
        Text(
            "形象模型",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "当前形象引擎",
            value = "Rive动画",
            onValueChange = {},
            readOnly = true
        )

        // 鈹€鈹€ 璇煶妯″瀷 鈹€鈹€
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "语音模型",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "当前语音模型",
            value = "火山模型",
            onValueChange = {},
            readOnly = true
        )

        // 鈹€鈹€ 鍞ら啋璇?鈹€鈹€
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "唤醒词",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "唤醒词列表",
            value = "小叶，小宁",
            onValueChange = {},
            readOnly = true
        )

        // 鈹€鈹€ 搴曢儴鎻愮ず 鈹€鈹€
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RobotTheme.colors.accent.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "提示",
                    tint = RobotTheme.colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "角色模型可修改功能已在新版本计划中，敬请期待。",
                    color = RobotTheme.colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

