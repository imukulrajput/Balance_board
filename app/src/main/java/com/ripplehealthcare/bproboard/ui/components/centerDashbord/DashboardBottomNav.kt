package com.ripplehealthcare.bproboard.ui.components.centerDashbord

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ripplehealthcare.bproboard.ui.theme.PrimaryColor
import com.ripplehealthcare.bproboard.ui.theme.TextBlack
import com.ripplehealthcare.bproboard.ui.theme.WhiteColor

sealed class BottomNavItem(var title: String, var icon: ImageVector, var route: String) {
    object Home : BottomNavItem("Home", Icons.Default.Home, "home")
    object Patients : BottomNavItem("Patients", Icons.Default.People, "patients")
    object Billing : BottomNavItem("Billing", Icons.Default.AccountBalanceWallet, "billing")
    object Profile : BottomNavItem("Profile", Icons.Default.Person, "profile")
}

@Composable
fun DashboardBottomNav(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
//        BottomNavItem.Patients,
//        BottomNavItem.Billing,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = WhiteColor
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = PrimaryColor,
                    selectedTextColor = PrimaryColor,
                    selectedIndicatorColor = Color.Transparent,
                    unselectedIconColor = TextBlack,
                    unselectedTextColor = TextBlack,
                    disabledIconColor = Color.Gray,
                    disabledTextColor = Color.Gray
                ),
            )
        }
    }
}