package com.flowmosaic.calendar.activity

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.flowmosaic.calendar.ui.theme.getPrimaryColor
import com.flowmosaic.calendar.widget.AgendaWidget

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            CalendarWidgetTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val defaultNavBarColor = MaterialTheme.colorScheme.background.toArgb()
                    val primaryColor = getPrimaryColor()
                    val navBarColor = remember { mutableIntStateOf(defaultNavBarColor) }
                    val renderHeader = remember { mutableStateOf(false) }
                    val headerSubtitle = remember { mutableStateOf("") }

                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect { backStackEntry ->
                            val widgetId = backStackEntry.arguments?.getInt("widgetId")
                            headerSubtitle.value = when (backStackEntry.destination.route) {
                                "widgets_list" -> getString(R.string.active_widgets)
                                "widget_config/{widgetId}" -> if (widgetId == 0) getString(R.string.prefs_title_editing_default_config) else ""
                                else -> ""
                            }
                            when (backStackEntry.destination.route) {
                                "onboard" -> {
                                    navBarColor.intValue = primaryColor.toArgb()
                                    renderHeader.value = false
                                }

                                else -> {
                                    navBarColor.intValue = defaultNavBarColor
                                    renderHeader.value = true
                                }
                            }
                        }
                    }

                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.light(
                            navBarColor.intValue, navBarColor.intValue
                        ),
                        statusBarStyle = SystemBarStyle.light(
                            primaryColor.toArgb(),
                            primaryColor.toArgb()
                        )
                    )

                    Column {
                        if (renderHeader.value) {
                            Header(subtitle = headerSubtitle.value)
                        }
                        NavHost(
                            navController = navController,
                            startDestination = if (showOnboarding()) "onboard" else "widgets_list"
                        ) {
                            composable("onboard") {
                                OnboardingScreen(onboardingPages(), onFinish = {
                                    AgendaWidgetPrefs.setOnboardingDone(applicationContext, true)
                                    navController.popBackStack()
                                    navController.navigate("widgets_list")
                                })
                            }
                            composable("widgets_list") {
                                WidgetsListView(onNavigate = { widgetId ->
                                    navController.navigate("widget_config/$widgetId")
                                })
                            }
                            composable(
                                "widget_config/{widgetId}",
                                arguments = listOf(navArgument("widgetId") {
                                    type = NavType.IntType
                                })
                            ) { backStackEntry ->
                                PreferencesScreen(appWidgetId = backStackEntry.arguments?.getInt("widgetId")!!)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun onboardingPages(): List<OnboardingPage> {
        return listOf(
            OnboardingPage(
                imageRes = R.drawable.onboard_0,
                text = applicationContext.getString(R.string.onboarding_0)
            ),
            OnboardingPage(
                imageRes = R.drawable.onboard_1,
                text = applicationContext.getString(R.string.onboarding_1)
            ),
            OnboardingPage(
                imageRes = R.drawable.onboard_2,
                text = applicationContext.getString(R.string.onboarding_2)
            ),
            OnboardingPage(
                imageRes = R.drawable.onboard_3,
                text = applicationContext.getString(R.string.onboarding_3)
            )
        )
    }

    private fun showOnboarding(): Boolean {
        val widgetIds = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(
                ComponentName(applicationContext, AgendaWidget::class.java)
            )
        return !(widgetIds.isNotEmpty() || AgendaWidgetPrefs.getOnboardingDone(applicationContext))
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

}

