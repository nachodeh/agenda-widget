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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.Header
import com.flowmosaic.calendar.ui.screens.OnboardingPage
import com.flowmosaic.calendar.ui.screens.OnboardingScreen
import com.flowmosaic.calendar.ui.screens.PreferencesScreen
import com.flowmosaic.calendar.ui.screens.WidgetsListView
import com.flowmosaic.calendar.ui.theme.CalendarWidgetTheme
import com.flowmosaic.calendar.ui.theme.getPrimaryColor
import com.flowmosaic.calendar.widget.AgendaWidget

enum class Screen {
    ONBOARD,
    WIDGETS_LIST,
    WIDGET_CONFIG,
}

enum class NavigationParams {
    WIDGET_ID,
    WIDGET_INDEX,
}

sealed class NavigationItem(val route: String) {
    data object Onboard : NavigationItem(Screen.ONBOARD.name)
    data object WidgetsList : NavigationItem(Screen.WIDGETS_LIST.name)
    data object WidgetConfig : NavigationItem(Screen.WIDGET_CONFIG.name)
    data object WidgetConfigWithParams :
        NavigationItem("${Screen.WIDGET_CONFIG.name}/{${NavigationParams.WIDGET_ID.name}}/{${NavigationParams.WIDGET_INDEX.name}}")
}

class MainActivity : ComponentActivity() {

    private val logger by lazy { AgendaWidgetLogger(applicationContext) }
    private val prefs by lazy { AgendaWidgetPrefs(applicationContext) }

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
                            headerSubtitle.value = getHeaderSubtitle(backStackEntry)
                            renderHeader.value = when (backStackEntry.destination.route) {
                                NavigationItem.Onboard.route -> false
                                else -> true
                            }
                            navBarColor.intValue = when (backStackEntry.destination.route) {
                                NavigationItem.Onboard.route -> primaryColor.toArgb()
                                else -> defaultNavBarColor
                            }
                            logger.logNavigationEvent(backStackEntry.destination.route)
                        }
                    }

                    enableEdgeToEdge(
                        navigationBarStyle = SystemBarStyle.light(
                            navBarColor.intValue, navBarColor.intValue
                        ),
                        statusBarStyle = SystemBarStyle.dark(primaryColor.toArgb())
                    )

                    Column {
                        if (renderHeader.value) {
                            Header(subtitle = headerSubtitle.value)
                        }
                        AgendaWidgetNavHost(navController)
                    }
                }
            }
        }
        logger.logActivityStartedEvent(AgendaWidgetLogger.Activity.MAIN_ACTIVITY)
    }

    private fun getHeaderSubtitle(backStackEntry: NavBackStackEntry): String {
        val widgetId = backStackEntry.arguments?.getInt(NavigationParams.WIDGET_ID.name)
        val widgetIndex = backStackEntry.arguments?.getInt(NavigationParams.WIDGET_INDEX.name)
        return when (backStackEntry.destination.route) {
            NavigationItem.WidgetsList.route -> getString(R.string.active_widgets)
            NavigationItem.WidgetConfigWithParams.route ->
                if (widgetId == 0)
                    getString(R.string.prefs_title_editing_default_config)
                else "Widget $widgetIndex"

            else -> ""
        }
    }

    @Composable
    fun AgendaWidgetNavHost(
        navController: NavHostController,
    ) {
        NavHost(
            navController = navController,
            startDestination = if (showOnboard()) NavigationItem.Onboard.route else NavigationItem.WidgetsList.route
        ) {
            composable(NavigationItem.Onboard.route) {
                OnboardingScreen(onboardPages(), onFinish = { skipped ->
                    prefs.setOnboardingDone(true)
                    navController.popBackStack()
                    navController.navigate(NavigationItem.WidgetsList.route)
                    logger.logOnboardingCompleteEvent(skipped)
                })
            }
            composable(NavigationItem.WidgetsList.route) {
                WidgetsListView(onNavigate = { widgetId, widgetIndex ->
                    navController.navigate("${NavigationItem.WidgetConfig.route}/$widgetId/$widgetIndex")
                })
            }
            composable(NavigationItem.WidgetConfigWithParams.route,
                arguments = listOf(
                    navArgument(NavigationParams.WIDGET_ID.name) {
                        type = NavType.IntType
                    }, navArgument(NavigationParams.WIDGET_INDEX.name) {
                        type = NavType.IntType
                    })
            ) { backStackEntry ->
                PreferencesScreen(
                    appWidgetId = backStackEntry.arguments?.getInt(
                        NavigationParams.WIDGET_ID.name
                    )!!
                )
            }
        }
    }

    @Composable
    private fun onboardPages(): List<OnboardingPage> {
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

    private fun showOnboard(): Boolean {
        val widgetIds = AppWidgetManager.getInstance(applicationContext)
            .getAppWidgetIds(
                ComponentName(applicationContext, AgendaWidget::class.java)
            )
        return !(widgetIds.isNotEmpty() || prefs.getOnboardingDone())
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

}

