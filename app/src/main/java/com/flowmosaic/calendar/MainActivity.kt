package com.flowmosaic.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowmosaic.calendar.ui.Header
import com.flowmosaic.calendar.ui.screens.PreferencesScreen
import com.flowmosaic.calendar.ui.screens.WidgetsListView
import com.flowmosaic.calendar.ui.theme.CalendarWidgetTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            CalendarWidgetTheme {
                enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(
                    MaterialTheme.colorScheme.background.toArgb(),
                    MaterialTheme.colorScheme.background.toArgb()
                ))
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val navController = rememberNavController()
                    val headerSubtitle = remember { mutableStateOf("") }

                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect { backStackEntry ->
                            val widgetId = backStackEntry.arguments?.getInt("widgetId")
                            headerSubtitle.value = when (backStackEntry.destination.route) {
                                "Home" -> getString(R.string.active_widgets)
                                "Widgets/{widgetId}" -> if (widgetId == 0) getString(R.string.prefs_title_editing_default_config) else ""
                                else -> ""
                            }
                        }
                    }

                    Column {
                        Header(subtitle = headerSubtitle.value)
                        NavHost(navController = navController, startDestination = "Home") {
                            composable("Home") {
                                WidgetsListView(navController)
                            }
                            composable(
                                "Widgets/{widgetId}",
                                arguments = listOf(navArgument("widgetId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                PreferencesScreen(appWidgetId = backStackEntry.arguments?.getInt("widgetId")!!)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

}

