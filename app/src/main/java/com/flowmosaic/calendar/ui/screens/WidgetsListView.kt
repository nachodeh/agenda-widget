package com.flowmosaic.calendar.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.widget.AgendaWidget

@Composable
fun WidgetsListView(onNavigate: (widgetId: Int) -> Unit) {
    val context = LocalContext.current

    val widgetIds = remember {
        mutableStateOf(
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))
        )
    }

    LifecycleResumeEffect(Unit) {
        widgetIds.value = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))

        onPauseOrDispose {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        widgetIds.value.forEachIndexed { index, id ->
            val idx = index + 1;
            LaunchWidgetConfigButton(id = id, text = "Widget $idx", onNavigate = onNavigate)
        }
        LaunchWidgetConfigButton(
            id = 0,
            text = context.getString(R.string.default_configuration),
            onNavigate = onNavigate
        )
    }
}