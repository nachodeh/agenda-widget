package com.flowmosaic.calendar.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.flowmosaic.calendar.activity.PermissionsActivity

@Composable
fun LaunchWidgetConfigButton(navHostController: NavHostController, id: Int, text: String) {
    val context = LocalContext.current
    Button(
        onClick = {
            if (hasCalendarPermission(context)) {
                navHostController.navigate("Widgets/$id")
            } else {
                val intent = Intent(context, PermissionsActivity::class.java)
                context.startActivity(intent)
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun hasCalendarPermission(context: Context): Boolean {
    val readCalendarPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR
    )
    val writeCalendarPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_CALENDAR
    )
    return readCalendarPermission == PackageManager.PERMISSION_GRANTED &&
            writeCalendarPermission == PackageManager.PERMISSION_GRANTED
}