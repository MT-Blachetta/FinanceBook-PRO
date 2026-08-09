package com.financebook.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.financebook.android.data.auth.TokenStore
import com.financebook.android.data.repository.FinanceRepository
import com.financebook.android.ui.categories.CategoriesScreen
import com.financebook.android.ui.categorytypes.CategoryTypesScreen
import com.financebook.android.ui.login.LoginScreen
import com.financebook.android.ui.payment.PaymentEditScreen
import com.financebook.android.ui.statistics.StatisticsScreen
import com.financebook.android.ui.summary.SummaryScreen

/** Route identifiers, mirroring the web app's pages. */
object Routes {
    const val LOGIN = "login"
    const val SUMMARY = "summary"
    const val ADD_PAYMENT = "payment/new"
    const val EDIT_PAYMENT = "payment/{itemId}/edit"
    const val CATEGORIES = "categories"
    const val CATEGORY_TYPES = "category-types"
    const val STATISTICS = "statistics"

    fun editPayment(itemId: Long) = "payment/$itemId/edit"
}

@Composable
fun FinanceBookNavHost(
    repository: FinanceRepository,
    tokenStore: TokenStore,
    navController: NavHostController = rememberNavController(),
) {
    val token by tokenStore.tokenFlow.collectAsState()

    // Whenever the token disappears (logout or 401), fall back to login.
    LaunchedEffect(token) {
        if (token == null) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (token == null) Routes.LOGIN else Routes.SUMMARY,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                repository = repository,
                onLoggedIn = {
                    navController.navigate(Routes.SUMMARY) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SUMMARY) {
            SummaryScreen(
                repository = repository,
                onAddPayment = { navController.navigate(Routes.ADD_PAYMENT) },
                onEditPayment = { id -> navController.navigate(Routes.editPayment(id)) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                onOpenCategoryTypes = { navController.navigate(Routes.CATEGORY_TYPES) },
                onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
            )
        }

        composable(Routes.ADD_PAYMENT) {
            PaymentEditScreen(
                repository = repository,
                itemId = null,
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.EDIT_PAYMENT,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { backStackEntry ->
            PaymentEditScreen(
                repository = repository,
                itemId = backStackEntry.arguments?.getLong("itemId"),
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CATEGORY_TYPES) {
            CategoryTypesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
