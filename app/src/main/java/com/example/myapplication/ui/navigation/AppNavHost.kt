package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.detail.ProductDetailScreen
import com.example.myapplication.ui.list.ProductListScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProductListRoute

@Serializable
data class ProductDetailRoute(val productId: Long) {
    companion object {
        /** Must match the property name above; type-safe nav stores args under it. */
        const val ARG_PRODUCT_ID = "productId"
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ProductListRoute,
        modifier = modifier,
    ) {
        composable<ProductListRoute> {
            ProductListScreen(
                onProductClick = { id -> navController.navigate(ProductDetailRoute(id)) },
            )
        }
        composable<ProductDetailRoute> {
            ProductDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
