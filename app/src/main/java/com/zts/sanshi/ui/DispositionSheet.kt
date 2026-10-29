package com.zts.sanshi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.MAX_TASKS
import com.zts.sanshi.domain.Task
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Danger
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositionSheet(
    pending: List<Task>,
    onRollAll: () -> Unit,
    onDisposeEach: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    val overflow = pending.size > MAX_TASKS
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { false })
    ModalBottomSheet(
        onDismissRequest = { /* must choose an action */ },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(Copy.YesterdayTitle, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(Copy.YesterdaySubtitle, color = InkMuted, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                pending.forEach { task ->
                    Text(
                        text = task.text,
                        color = Ink,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(Copy.RollBatchHint, color = InkMuted, fontSize = 13.sp)
            if (overflow) {
                Spacer(Modifier.height(4.dp))
                Text(Copy.RollAllOverflowHint, color = InkMuted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRollAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White),
            ) {
                Text(
                    if (overflow) Copy.RollAllOverflow else Copy.RollAll,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDisposeEach,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(Copy.DisposeEach, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDeleteAll, modifier = Modifier.fillMaxWidth()) {
                Text(Copy.DeleteAll, color = Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
