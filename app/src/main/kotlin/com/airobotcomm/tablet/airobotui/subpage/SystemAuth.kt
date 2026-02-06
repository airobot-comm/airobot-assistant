package com.airobotcomm.tablet.airobotui.subpage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobotcomm.tablet.airobotui.framework.comp.ConfigTextField
import com.airobotcomm.tablet.airobotui.framework.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.airobotui.framework.theme.RobotTextSecondary
import com.airobotcomm.tablet.airobotui.viewmodel.RobotMainViewModel
import com.airobotcomm.tablet.system.model.ActiveInfo

@Composable
fun SystemAuth(
    viewModel: RobotMainViewModel = hiltViewModel()
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val isActivated by viewModel.isDeviceActivated.collectAsState()
    
    var productKey by remember(deviceInfo) { 
        mutableStateOf(deviceInfo.activation.productKey) 
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "设备基本信息 (不可修改)",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "设备 ID",
            value = deviceInfo.deviceId,
            onValueChange = {},
        )

        ConfigTextField(
            label = "MAC 地址",
            value = deviceInfo.macAddress,
            onValueChange = {},
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "设备激活与授权",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "产品激活密钥 (Product Key)",
            value = productKey,
            onValueChange = { productKey = it },
        )

        // Activation Status Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("激活状态:", color = RobotTextSecondary, fontSize = 14.sp)
            Text(
                if (isActivated) "已成功激活" else "尚未激活",
                color = if (isActivated) RobotPrimaryCyan else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.activateDevice(productKey) },
            modifier = Modifier.fillMaxWidth(),
            enabled = productKey.length >= 8,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActivated) Color.Gray else RobotPrimaryCyan
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isActivated) "已激活" else "立即激活设备", 
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
        }

        if (isActivated) {
            Text(
                "激活时间: ${deviceInfo.activation.time}",
                color = RobotTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
