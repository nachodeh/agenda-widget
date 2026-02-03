package com.flowmosaic.calendar.activity

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
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
    private val isBatteryOptimizationDisabled = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        // Check battery optimization status
        checkBatteryOptimization()

        // Detect system dark mode
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        // Enable edge-to-edge before setContent for SDK 35+ compatibility
        // Status bar: force dark style (light icons on dark background)
        // Navigation bar: adapt icons based on theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (isDarkMode) {
                // Dark mode: light (white) icons for dark background
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            } else {
                // Light mode: dark (black) icons for light background
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            }
        )

        setContent {
            CalendarWidgetTheme {
                val primaryColor = getPrimaryColor()
                val backgroundColor = MaterialTheme.colorScheme.background
                val renderHeader = remember { mutableStateOf(false) }
                val headerSubtitle = remember { mutableStateOf("") }
                val isOnboarding = remember { mutableStateOf(false) }

                val navController = rememberNavController()
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        headerSubtitle.value = getHeaderSubtitle(backStackEntry)
                        val onOnboard = backStackEntry.destination.route == NavigationItem.Onboard.route
                        renderHeader.value = !onOnboard
                        isOnboarding.value = onOnboard
                        logger.logNavigationEvent(backStackEntry.destination.route)
                    }
                }

                // Determine colors for system bar backgrounds
                val statusBarBackgroundColor = primaryColor
                val navBarBackgroundColor = if (isOnboarding.value) primaryColor else backgroundColor

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    // Status bar background - extends behind status bar with primary color
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(statusBarBackgroundColor)
                    )

                    // Navigation bar background - extends behind navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(navBarBackgroundColor)
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                    )

                    // Main content with safe drawing padding
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        color = backgroundColor
                    ) {
                        Column {
                            if (renderHeader.value) {
                                Header(subtitle = headerSubtitle.value)
                            }
                            // Battery optimization warning banner
                            if (!isBatteryOptimizationDisabled.value && !isOnboarding.value) {
                                BatteryOptimizationBanner(
                                    onClick = { requestBatteryOptimizationExemption() }
                                )
                            }
                            AgendaWidgetNavHost(navController)
                        }
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

    override fun onResume() {
        super.onResume()
        // Re-check battery optimization when returning to the app
        checkBatteryOptimization()
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptimizationDisabled.value = powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimizationExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    @Composable
    private fun BatteryOptimizationBanner(onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = getString(R.string.battery_optimization_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

}

