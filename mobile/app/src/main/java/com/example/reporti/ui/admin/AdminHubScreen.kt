package com.example.reporti.ui.admin

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import com.example.reporti.data.AuthViewModel

enum class AdminPage { Dashboard, Users, Agents, Categories, Assignments }

@Composable
fun AdminHubScreen(
    bottomContentPadding: Dp,
    authVm: AuthViewModel,
) {
    var page by remember { mutableStateOf(AdminPage.Dashboard) }

    when (page) {
        AdminPage.Dashboard -> AdminDashboardScreen(
            bottomContentPadding = bottomContentPadding,
            authVm = authVm,
            onNavigate = { page = it },
        )
        AdminPage.Users -> AdminUsersScreen(
            title = "Users",
            filterRole = null,
            bottomContentPadding = bottomContentPadding,
            onBack = { page = AdminPage.Dashboard },
        )
        AdminPage.Agents -> AdminUsersScreen(
            title = "Agents",
            filterRole = "agent",
            bottomContentPadding = bottomContentPadding,
            onBack = { page = AdminPage.Dashboard },
        )
        AdminPage.Categories -> AdminCategoriesScreen(
            bottomContentPadding = bottomContentPadding,
            onBack = { page = AdminPage.Dashboard },
        )
        AdminPage.Assignments -> AdminAssignmentsScreen(
            bottomContentPadding = bottomContentPadding,
            onBack = { page = AdminPage.Dashboard },
        )
    }
}
