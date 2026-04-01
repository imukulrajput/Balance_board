package com.ripplehealthcare.frst.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ripplehealthcare.frst.ui.theme.PrimaryColor
import com.ripplehealthcare.frst.ui.theme.TextBlack
import com.ripplehealthcare.frst.ui.theme.WhiteColor

// Data class to represent each item in the bottom navigation
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigation(navController: NavController, doctorId: String) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, "doctorDashboard/$doctorId"),
        BottomNavItem("Profile",Icons.Default.AccountCircle, "doctorProfile")
    )

    NavigationBar(
        containerColor = WhiteColor
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val isSelected = when (item.title) {
                "Home" -> currentDestination?.route == "doctorDashboard/{doctorId}"
                "Profile" -> currentDestination?.route == "doctorProfile"
                else -> false
            }
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                colors = NavigationBarItemColors(
                    selectedIconColor = PrimaryColor,
                    selectedTextColor = PrimaryColor,
                    selectedIndicatorColor = Color.Transparent,
                    unselectedIconColor = TextBlack,
                    unselectedTextColor = TextBlack,
                    disabledIconColor = Color.Gray,
                    disabledTextColor = Color.Gray
                ),
                onClick = {
                    navController.navigate(item.route) {
                        // Avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}