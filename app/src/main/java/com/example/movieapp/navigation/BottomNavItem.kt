package com.example.movieapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem(NavRoutes.HOME, "Trang chủ", Icons.Default.Home)
    data object Search : BottomNavItem(NavRoutes.SEARCH, "Tìm kiếm", Icons.Default.Search)
    data object Favorites : BottomNavItem(NavRoutes.FAVORITES, "Yêu thích", Icons.Default.Favorite)
    data object Profile : BottomNavItem(NavRoutes.PROFILE_LIST, "Hồ sơ", Icons.Default.Person)
}
