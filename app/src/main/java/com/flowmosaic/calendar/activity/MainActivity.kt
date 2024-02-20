package com.flowmosaic.calendar.activity

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.Header
import com.flowmosaic.calendar.ui.screens.OnboardingPage
import com.flowmosaic.calendar.ui.screens.OnboardingScreen
import com.flowmosaic.calendar.ui.screens.PreferencesScreen
import com.flowmosaic.calendar.ui.screens.WidgetsListView
import com.flowmosaic.calendar.ui.theme.CalendarWidgetTheme
import com.flowmosaic.calendar.ui.theme.Primary
import com.flowmosaic.calendar.widget.AgendaWidget

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)





        setContent {
            CalendarWidgetTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val defaultNavBarColor = MaterialTheme.colorScheme.background.toArgb()
                    val onboardBgColor = MaterialTheme.colorScheme.inversePrimary.toArgb()
                    val navController = rememberNavController()
                    val headerSubtitle = remember { mutableStateOf("") }
                    val navBarColor = remember { mutableIntStateOf(defaultNavBarColor) }
                    val renderHeader = remember { mutableStateOf( false )}



                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect { backStackEntry ->
                            val widgetId = backStackEntry.arguments?.getInt("widgetId")
                            headerSubtitle.value = when (backStackEntry.destination.route) {
                                "Home" -> getString(R.string.active_widgets)
                                "Widgets/{widgetId}" -> if (widgetId == 0) getString(R.string.prefs_title_editing_default_config) else ""
                                else -> ""
                            }
                            navBarColor.intValue = when (backStackEntry.destination.route) {
                                "Onboard" -> onboardBgColor
                                else -> defaultNavBarColor
                            }
                            renderHeader.value = when (backStackEntry.destination.route) {
                                "Onboard" -> false
                                else -> true
                            }
                        }
                    }

                    val navBarStyle = SystemBarStyle.light(
                        navBarColor.intValue,
                        navBarColor.intValue

                    )
                    val statusBarStyle = SystemBarStyle.light(
                        onboardBgColor,
                        onboardBgColor
                    )

                    enableEdgeToEdge(navigationBarStyle = navBarStyle, statusBarStyle = statusBarStyle)

                    val startPage = if (showOnboarding()) "Onboard" else "Home"

                    Column {
                        if (renderHeader.value) {
                            Header(subtitle = headerSubtitle.value)
                        }
                        NavHost(navController = navController, startDestination = startPage) {
                            composable("Onboard") {
                                val onboardingPages = listOf(
                                    OnboardingPage(
                                        imageRes = R.drawable.onboard_1, // Replace with actual drawable resource IDs
                                        text = "1. To add Agenda Widget, long press on the home screen"
                                    ),
                                    OnboardingPage(
                                        imageRes = R.drawable.onboard_2,
                                        text = "2. Select Widgets from the home screen options menu"
                                    ),
                                    OnboardingPage(
                                        imageRes = R.drawable.onboard_3,
                                        text = "3. Drag Agenda Widget to your home screen. You're all set!"
                                    )
                                )
                                OnboardingScreen(onboardingPages, onFinish = {
                                    AgendaWidgetPrefs.setOnboardingDone(applicationContext, true)
                                    navController.popBackStack()
                                    navController.navigate("Home")
                                })
                            }
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

    private fun showOnboarding(): Boolean {
        val widgetIds = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(
                ComponentName(applicationContext, AgendaWidget::class.java))
        return !(widgetIds.isNotEmpty() || AgendaWidgetPrefs.getOnboardingDone(applicationContext))
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

}

