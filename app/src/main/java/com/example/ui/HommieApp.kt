package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HommieRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HommieApp() {
  var showRoleSwitcher by remember { mutableStateOf(false) }
  var showNotificationsSheet by remember { mutableStateOf(false) }

  // Customer Navigation State
  var customerSelectedTab by remember { mutableStateOf(0) }
  var selectedWorkerForProfile by remember { mutableStateOf<WorkerProfile?>(null) }
  var selectedWorkerForBooking by remember { mutableStateOf<WorkerProfile?>(null) }
  var selectedBookingId by remember { mutableStateOf<String?>(null) }

  // Worker Navigation State
  var workerSelectedTab by remember { mutableStateOf(0) }

  // Admin Navigation State
  var adminSelectedTab by remember { mutableStateOf(0) }

  Scaffold(
    topBar = {
      Column {
        HommieTopBar(
          onNotificationClick = { showNotificationsSheet = true },
          onRoleSwitchClick = { showRoleSwitcher = true }
        )
        // Real-Time Socket Event Ticker Banner
        HommieRepository.lastEventLog?.let { eventText ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = IndigoDeep
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(modifier = Modifier.size(6.dp).background(EmeraldSuccess, CircleShape))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = eventText,
                fontSize = 11.sp,
                color = IndigoLight,
                maxLines = 1
              )
            }
          }
        }
      }
    },
    bottomBar = {
      // Bottom Navigation depending on role
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceWhite,
        shadowElevation = 8.dp
      ) {
        when (HommieRepository.currentRole) {
          UserRole.CUSTOMER -> {
            NavigationBar(
              containerColor = SurfaceWhite,
              contentColor = IndigoPrimary
            ) {
              NavigationBarItem(
                selected = customerSelectedTab == 0 && selectedBookingId == null,
                onClick = { customerSelectedTab = 0; selectedBookingId = null },
                icon = { Icon(Icons.Default.Storefront, contentDescription = "Marketplace") },
                label = { Text("Marketplace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = customerSelectedTab == 1 || selectedBookingId != null,
                onClick = { customerSelectedTab = 1 },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Bookings") },
                label = { Text("My Bookings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = customerSelectedTab == 2 && selectedBookingId == null,
                onClick = { customerSelectedTab = 2; selectedBookingId = null },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
            }
          }
          UserRole.WORKER -> {
            NavigationBar(
              containerColor = SurfaceWhite,
              contentColor = IndigoPrimary
            ) {
              NavigationBarItem(
                selected = workerSelectedTab == 0 && selectedBookingId == null,
                onClick = { workerSelectedTab = 0; selectedBookingId = null },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = workerSelectedTab == 1 && selectedBookingId == null,
                onClick = { workerSelectedTab = 1; selectedBookingId = null },
                icon = { Icon(Icons.Default.Build, contentDescription = "Services") },
                label = { Text("Services & Rates", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = workerSelectedTab == 2 && selectedBookingId == null,
                onClick = { workerSelectedTab = 2; selectedBookingId = null },
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Earnings") },
                label = { Text("Earnings & Payout", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
            }
          }
          UserRole.ADMIN -> {
            NavigationBar(
              containerColor = SurfaceWhite,
              contentColor = IndigoPrimary
            ) {
              NavigationBarItem(
                selected = adminSelectedTab == 0,
                onClick = { adminSelectedTab = 0 },
                icon = { Icon(Icons.Default.Insights, contentDescription = "KPIs") },
                label = { Text("Executive", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = adminSelectedTab == 1,
                onClick = { adminSelectedTab = 1 },
                icon = { Icon(Icons.Default.People, contentDescription = "Users") },
                label = { Text("Users", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = adminSelectedTab == 2,
                onClick = { adminSelectedTab = 2 },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Verifications") },
                label = { Text("Verifications", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
              NavigationBarItem(
                selected = adminSelectedTab == 3,
                onClick = { adminSelectedTab = 3 },
                icon = { Icon(Icons.Default.Gavel, contentDescription = "Disputes") },
                label = { Text("Disputes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = IndigoPrimary, indicatorColor = IndigoLight)
              )
            }
          }
        }
      }
    },
    containerColor = SlateBackground
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // If a specific booking detail is open (can be opened by Customer or Worker)
      if (selectedBookingId != null) {
        BookingDetailScreen(
          bookingId = selectedBookingId!!,
          onBack = { selectedBookingId = null }
        )
      } else {
        when (HommieRepository.currentRole) {
          UserRole.CUSTOMER -> {
            when (customerSelectedTab) {
              0 -> CustomerExploreScreen(
                onSelectWorker = { selectedWorkerForProfile = it },
                onBookWorker = { selectedWorkerForBooking = it }
              )
              1 -> CustomerBookingsScreen(
                onSelectBooking = { selectedBookingId = it.id }
              )
              2 -> CustomerProfileScreen()
            }
          }
          UserRole.WORKER -> {
            when (workerSelectedTab) {
              0 -> WorkerDashboardScreen(
                onSelectBooking = { selectedBookingId = it.id }
              )
              1 -> ServicesPricingScreen()
              2 -> EarningsPayoutsScreen()
            }
          }
          UserRole.ADMIN -> {
            when (adminSelectedTab) {
              0 -> AdminOverviewScreen()
              1 -> AdminUsersScreen()
              2 -> AdminVerificationsScreen()
              3 -> AdminDisputesScreen()
            }
          }
        }
      }
    }
  }

  // Worker Public Profile Modal
  selectedWorkerForProfile?.let { worker ->
    WorkerPublicProfileModal(
      worker = worker,
      onDismiss = { selectedWorkerForProfile = null },
      onBookNow = {
        selectedWorkerForBooking = worker
        selectedWorkerForProfile = null
      }
    )
  }

  // Booking Wizard Bottom Sheet
  selectedWorkerForBooking?.let { worker ->
    BookingWizardSheet(
      worker = worker,
      onDismiss = { selectedWorkerForBooking = null },
      onBookingCreated = { createdBooking ->
        selectedWorkerForBooking = null
        selectedBookingId = createdBooking.id
      }
    )
  }

  // Notifications Bottom Sheet
  if (showNotificationsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showNotificationsSheet = false },
      containerColor = SurfaceWhite
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Notification Center", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
          TextButton(onClick = { HommieRepository.markAllNotificationsRead() }) {
            Text("Mark all read", color = IndigoPrimary, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (HommieRepository.notifications.isEmpty()) {
          Text("No notifications yet", color = SlateTextMuted, fontSize = 13.sp)
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(HommieRepository.notifications) { notif ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (notif.targetBookingId != null) {
                      selectedBookingId = notif.targetBookingId
                      showNotificationsSheet = false
                    }
                  },
                shape = RoundedCornerShape(10.dp),
                color = if (notif.isRead) SlateSurface else IndigoLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (notif.isRead) SlateBorder else IndigoSoft)
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(if (notif.isRead) Color.Transparent else IndigoPrimary, CircleShape)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateTextPrimary)
                      Text(notif.timestamp, fontSize = 11.sp, color = SlateTextSubtle)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(notif.body, fontSize = 12.sp, color = SlateTextMuted)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Quick Role Switcher Dialog
  if (showRoleSwitcher) {
    AlertDialog(
      onDismissRequest = { showRoleSwitcher = false },
      title = { Text("Switch Active Ecosystem Role", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Select any of the 3 core Hommie personas to evaluate features in real-time:",
            fontSize = 13.sp,
            color = SlateTextMuted
          )

          // Customer
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                HommieRepository.switchRole(UserRole.CUSTOMER)
                selectedBookingId = null
                showRoleSwitcher = false
              },
            shape = RoundedCornerShape(10.dp),
            color = if (HommieRepository.currentRole == UserRole.CUSTOMER) IndigoLight else SlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (HommieRepository.currentRole == UserRole.CUSTOMER) IndigoPrimary else SlateBorder)
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(36.dp).background(IndigoPrimary, CircleShape), contentAlignment = Alignment.Center) {
                Text("AR", fontWeight = FontWeight.Bold, color = Color.White)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("1. Customer (Alex Rivera)", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text("Marketplace, booking wizard, escrow release, reviews", fontSize = 11.sp, color = SlateTextMuted)
              }
            }
          }

          // Worker
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                HommieRepository.switchRole(UserRole.WORKER)
                selectedBookingId = null
                showRoleSwitcher = false
              },
            shape = RoundedCornerShape(10.dp),
            color = if (HommieRepository.currentRole == UserRole.WORKER) EmeraldLight else SlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (HommieRepository.currentRole == UserRole.WORKER) EmeraldSuccess else SlateBorder)
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(36.dp).background(EmeraldSuccess, CircleShape), contentAlignment = Alignment.Center) {
                Text("MV", fontWeight = FontWeight.Bold, color = Color.White)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("2. Worker (Marcus Vance)", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text("3m job offer countdown, status stepper, earnings & payout", fontSize = 11.sp, color = SlateTextMuted)
              }
            }
          }

          // Admin
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                HommieRepository.switchRole(UserRole.ADMIN)
                selectedBookingId = null
                showRoleSwitcher = false
              },
            shape = RoundedCornerShape(10.dp),
            color = if (HommieRepository.currentRole == UserRole.ADMIN) AmberLight else SlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (HommieRepository.currentRole == UserRole.ADMIN) AmberWarning else SlateBorder)
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(36.dp).background(AmberWarning, CircleShape), contentAlignment = Alignment.Center) {
                Text("SC", fontWeight = FontWeight.Bold, color = Color.White)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("3. Admin (Sarah Chen)", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text("Executive GMV KPIs, dispute resolution console, verifications", fontSize = 11.sp, color = SlateTextMuted)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showRoleSwitcher = false }) { Text("Close") }
      }
    )
  }
}
