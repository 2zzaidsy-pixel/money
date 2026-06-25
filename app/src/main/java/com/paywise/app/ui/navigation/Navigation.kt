package com.paywise.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.paywise.app.ui.screens.auth.AuthScreen
import com.paywise.app.ui.screens.onboarding.OnboardingScreen
import com.paywise.app.ui.screens.dashboard.DashboardScreen
import com.paywise.app.ui.screens.expenses.ExpensesScreen
import com.paywise.app.ui.screens.expenses.AddExpenseScreen
import com.paywise.app.ui.screens.budgets.BudgetsScreen
import com.paywise.app.ui.screens.reports.ReportsScreen
import com.paywise.app.ui.screens.goals.GoalsScreen
import com.paywise.app.ui.screens.goals.AddGoalScreen
import com.paywise.app.ui.screens.goals.GoalDetailScreen
import com.paywise.app.ui.screens.simulator.SimulatorScreen
import com.paywise.app.ui.screens.emergency.EmergencyFundScreen
import com.paywise.app.ui.screens.subscriptions.SubscriptionsScreen
import com.paywise.app.ui.screens.settings.SettingsScreen
import com.paywise.app.ui.screens.premium.PremiumScreen
import com.paywise.app.ui.screens.profile.ProfileScreen
import com.paywise.app.ui.screens.legal.PrivacyScreen
import com.paywise.app.ui.screens.legal.TermsScreen
import com.paywise.app.ui.screens.support.SupportScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object AddExpense : Screen("add_expense")
    object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "edit_expense/$expenseId"
    }
    object Budgets : Screen("budgets")
    object Reports : Screen("reports")
    object Goals : Screen("goals")
    object AddGoal : Screen("add_goal")
    object GoalDetail : Screen("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }
    object Simulator : Screen("simulator")
    object EmergencyFund : Screen("emergency_fund")
    object Subscriptions : Screen("subscriptions")
    object Settings : Screen("settings")
    object Premium : Screen("premium")
    object Profile : Screen("profile")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")
    object Support : Screen("support")
}

@Composable
fun PayWiseNavGraph(
    navController: NavHostController,
    startDestination: String,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Auth.route) {
            AuthScreen(onAuthSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                onNavigateToSimulator = { navController.navigate(Screen.Simulator.route) },
                onNavigateToEmergencyFund = { navController.navigate(Screen.EmergencyFund.route) },
                onNavigateToSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }
        composable(Screen.Expenses.route) {
            ExpensesScreen(
                onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                onEditExpense = { expenseId -> navController.navigate(Screen.EditExpense.createRoute(expenseId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                expenseId = null,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            AddExpenseScreen(
                expenseId = expenseId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Budgets.route) {
            BudgetsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Reports.route) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Goals.route) {
            GoalsScreen(
                onAddGoal = { navController.navigate(Screen.AddGoal.route) },
                onGoalClick = { goalId -> navController.navigate(Screen.GoalDetail.createRoute(goalId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddGoal.route) {
            AddGoalScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.GoalDetail.route,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
            GoalDetailScreen(
                goalId = goalId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Simulator.route) {
            SimulatorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.EmergencyFund.route) {
            EmergencyFundScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                darkTheme = darkTheme,
                onThemeChange = onThemeChange,
                onBack = { navController.popBackStack() },
                onPrivacy = { navController.navigate(Screen.Privacy.route) },
                onTerms = { navController.navigate(Screen.Terms.route) },
                onSupport = { navController.navigate(Screen.Support.route) },
                onPremium = { navController.navigate(Screen.Premium.route) },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Premium.route) {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Privacy.route) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Terms.route) {
            TermsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Support.route) {
            SupportScreen(onBack = { navController.popBackStack() })
        }
    }
}
