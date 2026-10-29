package com.zts.sanshi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zts.sanshi.domain.RepeatKind
import com.zts.sanshi.domain.RepeatRule
import com.zts.sanshi.ui.theme.Blue
import com.zts.sanshi.ui.theme.Border
import com.zts.sanshi.ui.theme.DisabledFill
import com.zts.sanshi.ui.theme.Ink
import com.zts.sanshi.ui.theme.InkDisabled
import com.zts.sanshi.ui.theme.InkMuted

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorSheet(
    request: EditorRequest,
    onDismiss: () -> Unit,
    onSave: (String, RepeatRule) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    var draft by rememberSaveable(request.taskId, request.poolId, request.initialText) {
        mutableStateOf(request.initialText)
    }
    var kind by rememberSaveable(request.taskId, request.poolId, request.initialRepeat) {
        mutableStateOf(request.initialRepeat.kind.name)
    }
    var weekDays by remember(request.taskId, request.poolId, request.initialRepeat) {
        mutableStateOf(request.initialRepeat.weekDays)
    }
    var weekHint by remember { mutableStateOf(false) }
    val repeat = RepeatRule(
        kind = RepeatKind.valueOf(kind),
        weekDays = if (RepeatKind.valueOf(kind) == RepeatKind.Weekly) weekDays else emptySet(),
    )
    val canSave = draft.isNotBlank() && !(repeat.kind == RepeatKind.Weekly && repeat.weekDays.isEmpty())
    LaunchedEffect(request.taskId, request.poolId) {
        focusRequester.requestFocus()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = Ink,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = if (request.forPool) Copy.PoolPlaceholder else Copy.Placeholder,
                        color = InkDisabled,
                        fontSize = 16.sp,
                    )
                },
                textStyle = TextStyle(
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 1,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = Border,
                    cursorColor = Blue,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )
            Spacer(modifier = Modifier.padding(top = 16.dp))
            Text(
                text = Copy.Repeat,
                color = InkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepeatChip(Copy.RepeatNone, kind == RepeatKind.None.name) { kind = RepeatKind.None.name; weekHint = false }
                RepeatChip(Copy.RepeatDaily, kind == RepeatKind.Daily.name) { kind = RepeatKind.Daily.name; weekHint = false }
                RepeatChip(Copy.RepeatWeekdays, kind == RepeatKind.Weekdays.name) { kind = RepeatKind.Weekdays.name; weekHint = false }
                RepeatChip(Copy.RepeatWeekly, kind == RepeatKind.Weekly.name) { kind = RepeatKind.Weekly.name }
            }
            if (kind == RepeatKind.Weekly.name) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日").forEach { (d, label) ->
                        RepeatChip(label, d in weekDays) {
                            weekDays = if (d in weekDays) weekDays - d else weekDays + d
                            weekHint = false
                        }
                    }
                }
                if (weekHint || weekDays.isEmpty()) {
                    Text(
                        text = Copy.PickWeekday,
                        color = InkMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(Copy.Cancel, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (repeat.kind == RepeatKind.Weekly && repeat.weekDays.isEmpty()) {
                            weekHint = true
                        } else {
                            onSave(draft, repeat)
                        }
                    },
                    enabled = draft.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue,
                        contentColor = Color.White,
                        disabledContainerColor = DisabledFill,
                        disabledContentColor = InkDisabled,
                    ),
                ) {
                    Text(Copy.Save, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RepeatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        },
        border = BorderStroke(1.dp, if (selected) Blue else Border),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = Ink,
            selectedContainerColor = Color.White,
            selectedLabelColor = Blue,
        ),
    )
}
