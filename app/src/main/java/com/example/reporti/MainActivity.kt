package com.example.reporti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reporti.ui.home.HomeScreen
import com.example.reporti.ui.home.HomeViewModel
import com.example.reporti.ui.notifications.NotificationViewModel
import com.example.reporti.ui.notifications.NotificationsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MainScreen() } }
    }
}

enum class Screen(val title: String, @DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_home),
    Report("Report", R.drawable.ic_edit),
    Map("Map", R.drawable.ic_map),
    Notifications("Alertes", R.drawable.ic_notifications),
    Profile("Profile", R.drawable.ic_person)
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(Screen.Home) }

    val notificationVm: NotificationViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()
    val notifState by notificationVm.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (selectedTab) {
            Screen.Home -> HomeScreen(homeVm)
            Screen.Notifications -> NotificationsScreen(notificationVm)
            Screen.Report -> PlaceholderScreen("Report (à faire par l'équipe)")
            Screen.Map -> PlaceholderScreen("Map (à faire par l'équipe)")
            Screen.Profile -> PlaceholderScreen("Profile (bientôt disponible)")
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            shape = RoundedCornerShape(36.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Screen.entries.forEach { screen ->
                    val isSelected = screen == selectedTab
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    val badge = if (screen == Screen.Notifications) notifState.unreadCount else 0L

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTab = screen }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(0.12f)
                                    else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (badge > 0) {
                                        Badge { Text(if (badge > 99) "99+" else badge.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = screen.icon),
                                    contentDescription = screen.title,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(screen.title, color = color, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}