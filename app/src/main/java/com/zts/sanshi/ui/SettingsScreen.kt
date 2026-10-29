package com.zts.sanshi.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zts.sanshi.domain.ReminderSettings
import com.zts.sanshi.ui.theme.Bg
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkDisabled
import com.zts.sanshi.ui.theme.InkMuted
import com.zts.sanshi.ui.theme.Surface as SurfaceTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    reminder: ReminderSettings,
    onBack: () -> Unit,
    onSetReminder: (ReminderSettings) -> Unit,
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var needPermissionHint by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            needPermissionHint = false
            onSetReminder(reminder.copy(enabled = true))
        } else {
            needPermissionHint = true
            onSetReminder(reminder.copy(enabled = false))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = Ink)
            }
            Text(Copy.Settings, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("提醒", color = InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(SurfaceTone, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Copy.Reminder, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val needNotif = Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            if (needNotif) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                needPermissionHint = false
                                onSetReminder(reminder.copy(enabled = true))
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val am = context.getSystemService(AlarmManager::class.java)
                                    if (am != null && !am.canScheduleExactAlarms()) {
                                        // soft: still schedule inexact
                                    }
                                }
                            }
                        } else {
                            needPermissionHint = false
                            onSetReminder(reminder.copy(enabled = false))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Blue,
                        checkedThumbColor = Color.White,
                    ),
                )
            }
            if (reminder.enabled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Copy.ReminderTime, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(
                        "%02d:%02d".format(reminder.hour, reminder.minute),
                        color = Blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (needPermissionHint) {
                Spacer(Modifier.height(8.dp))
                Text(
                    Copy.ReminderNeedPermission,
                    color = InkMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(Copy.ReminderLocal, color = InkMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("小组件", color = InkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(SurfaceTone, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(Copy.WidgetTitle, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(Copy.WidgetBody, color = InkMuted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Text(Copy.WidgetAppTitle, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(Copy.WidgetEmpty, color = InkMuted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(Copy.WidgetPreviewLabel, color = InkDisabled, fontSize = 11.sp)
            }
        }
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = reminder.hour, initialMinute = reminder.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onSetReminder(reminder.copy(hour = pickerState.hour, minute = pickerState.minute))
                    showPicker = false
                }) { Text(Copy.Save, color = Blue, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(Copy.Cancel, color = Ink, fontWeight = FontWeight.SemiBold)
                }
            },
            text = { TimePicker(state = pickerState) },
            containerColor = Color.White,
        )
    }
}
