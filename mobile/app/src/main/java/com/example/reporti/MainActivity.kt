package com.example.reporti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.data.AuthViewModel
import com.example.reporti.ui.ReportFormScreen
import com.example.reporti.ui.admin.AdminHubScreen
import com.example.reporti.ui.assignments.AssignmentScreen
import com.example.reporti.ui.assignments.AssignmentViewModel
import com.example.reporti.ui.auth.AuthGate
import com.example.reporti.ui.auth.AccountScreen
import com.example.reporti.ui.home.HomeScreen
import com.example.reporti.ui.home.HomeViewModel
import com.example.reporti.ui.notifications.NotificationViewModel
import com.example.reporti.ui.notifications.NotificationsScreen
import com.example.reporti.ui.theme.ReportiTheme
import com.example.reporti.ui.map.MapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReportiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AuthGate { authVm -> MainScreen(authVm = authVm) }
                }
            }
        }
    }
}

enum class Screen(val title: String, @DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_home),
    Report("Report", R.drawable.ic_edit),
    Admin("Admin", R.drawable.ic_person),
    Assignments("Assignments", R.drawable.ic_notifications),
    Map("Map", R.drawable.ic_map),
    Notifications("Notifications", R.drawable.ic_notifications),
    Profile("Profile", R.drawable.ic_person),
}

@Composable
fun MainScreen(authVm: AuthViewModel) {
    val currentUser = authVm.state.user
    val role = currentUser?.role ?: "citizen"

    val visibleScreens = remember(role) {
        Screen.entries.filter { screen ->
            when (screen) {
                Screen.Assignments -> role == "agent" || role == "sector_manager"
                Screen.Admin -> role == "admin"
                else -> true
            }
        }
    }

    var selectedTab by remember { mutableStateOf(Screen.Home) }
    LaunchedEffect(visibleScreens) {
        if (selectedTab !in visibleScreens) selectedTab = Screen.Home
    }

    val assignmentVm: AssignmentViewModel = viewModel()
    LaunchedEffect(currentUser?.id) { assignmentVm.reset() }

    val notificationVm: NotificationViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()
    val notifState by notificationVm.state.collectAsState()

    var navBarHeightPx by remember { mutableIntStateOf(0) }
    val navBarHeight = with(LocalDensity.current) { navBarHeightPx.toDp() }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        when (selectedTab) {
            Screen.Home -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                HomeScreen(vm = homeVm, bottomContentPadding = navBarHeight + 16.dp)
            }
            Screen.Notifications -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                NotificationsScreen(notificationVm)
            }
            Screen.Report -> ReportFormScreen(
                modifier = Modifier.statusBarsPadding(),
                bottomContentPadding = navBarHeight + 16.dp,
            )
            Screen.Assignments -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                AssignmentScreen(vm = assignmentVm, bottomContentPadding = navBarHeight + 16.dp)
            }
            Screen.Admin -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                AdminHubScreen(
                    bottomContentPadding = navBarHeight + 16.dp,
                    authVm = authVm,
                )
            }
            Screen.Map -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                MapScreen(bottomContentPadding = navBarHeight + 16.dp)
            }
            Screen.Profile -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                AccountScreen(vm = authVm, bottomContentPadding = navBarHeight + 16.dp)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { navBarHeightPx = it.height }
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            shape = RoundedCornerShape(36.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                visibleScreens.forEach { screen ->
                    val isSelected = screen == selectedTab
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    val badge = if (screen == Screen.Notifications) notifState.unreadCount else 0L

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTab = screen }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(0.12f) else Color.Transparent,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BadgedBox(badge = {
                                if (badge > 0) Badge { Text(if (badge > 99) "99+" else badge.toString()) }
                            }) {
                                Icon(
                                    painter = painterResource(id = screen.icon),
                                    contentDescription = screen.title,
                                    tint = color,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(screen.title, color = color, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
