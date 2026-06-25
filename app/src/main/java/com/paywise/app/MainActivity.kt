package com.paywise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.paywise.app.data.local.PreferencesManager
import com.paywise.app.ui.navigation.PayWiseNavGraph
import com.paywise.app.ui.navigation.Screen
import com.paywise.app.ui.theme.PayWiseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by preferencesManager.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
            val isOnboardingDone by preferencesManager.isOnboardingDone.collectAsStateWithLifecycle(initialValue = false)
            val isLoggedIn by preferencesManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
            var darkTheme by remember { mutableStateOf(false) }

            LaunchedEffect(isDarkMode) {
                darkTheme = isDarkMode
            }

            val startDestination = when {
                !isOnboardingDone -> Screen.Onboarding.route
                !isLoggedIn -> Screen.Auth.route
                else -> Screen.Dashboard.route
            }

            PayWiseTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    PayWiseNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        darkTheme = darkTheme,
                        onThemeChange = { darkTheme = it }
                    )
                }
            }
        }
    }
}
