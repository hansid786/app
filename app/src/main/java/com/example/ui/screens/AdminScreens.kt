package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HommieRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AdminOverviewScreen() {
  val totalGMV = HommieRepository.bookings.sumOf { it.pricing.totalHeldAmount }
  val netPlatformRev = totalGMV * 0.10
  val totalBookings = HommieRepository.bookings.size
  val completedBookings = HommieRepository.bookings.count {
    it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.FUNDS_RELEASED
  }
  val completionRate = if (totalBookings > 0) (completedBookings * 100) / totalBookings else 94
  val disputedBookings = HommieRepository.bookings.count { it.status == BookingStatus.DISPUTED }
  val disputeRate = if (totalBookings > 0) (disputedBookings * 100) / totalBookings else 2

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text("Executive Dashboard & Platform KPIs", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateTextPrimary)

    // KPI Cards Grid
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      HommieCard(modifier = Modifier.weight(1f)) {
        Text("Gross Marketplace (GMV)", fontSize = 11.sp, color = SlateTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text("$${Math.round(totalGMV * 100.0) / 100.0}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = IndigoPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text("+18.4% this week", fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
      }
      HommieCard(modifier = Modifier.weight(1f)) {
        Text("Net Platform Revenue", fontSize = 11.sp, color = SlateTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text("$${Math.round(netPlatformRev * 100.0) / 100.0}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = EmeraldSuccess)
        Spacer(modifier = Modifier.height(2.dp))
        Text("10% fee captured", fontSize = 10.sp, color = SlateTextMuted)
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      HommieCard(modifier = Modifier.weight(1f)) {
        Text("Completion Rate", fontSize = 11.sp, color = SlateTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text("${completionRate}%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Industry top 5%", fontSize = 10.sp, color = IndigoDark)
      }
      HommieCard(modifier = Modifier.weight(1f)) {
        Text("Dispute Rate", fontSize = 11.sp, color = SlateTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text("${disputeRate}%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = RoseDestructive)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Protected by Escrow", fontSize = 10.sp, color = SlateTextMuted)
      }
    }

    // Active Users Summary
    HommieCard {
      Text("Platform User Ecosystem", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Customers", fontSize = 12.sp, color = SlateTextMuted)
          Text("${HommieRepository.allUsers.count { it.role == UserRole.CUSTOMER }} Active", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column {
          Text("Service Pros", fontSize = 12.sp, color = SlateTextMuted)
          Text("${HommieRepository.workers.size} Onboarded", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column {
          Text("Pending Verifications", fontSize = 12.sp, color = SlateTextMuted)
          Text("${HommieRepository.verificationQueue.count { it.status == "PENDING" }} Pending", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AmberWarning)
        }
      }
    }

    // HommieState Real-Time Heartbeat Card
    HommieCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(10.dp).background(EmeraldSuccess, CircleShape))
          Spacer(modifier = Modifier.width(8.dp))
          Text("HommieState Cluster: HEALTHY", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldSuccess)
        }
        Text("WebSocket: Auth JWT", fontSize = 11.sp, color = SlateTextMuted)
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text("Latest Broadcast Event:", fontSize = 11.sp, color = SlateTextMuted)
      Text(
        text = HommieRepository.lastEventLog ?: "Cluster idle",
        fontSize = 12.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = SlateTextPrimary
      )
    }
  }
}

@Composable
fun AdminUsersScreen() {
  var searchQuery by remember { mutableStateOf("") }
  var filterRole by remember { mutableStateOf("ALL") }

  val filteredUsers = HommieRepository.allUsers.filter { user ->
    val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
      user.email.contains(searchQuery, ignoreCase = true)
    val matchesRole = when (filterRole) {
      "CUSTOMER" -> user.role == UserRole.CUSTOMER
      "WORKER" -> user.role == UserRole.WORKER
      else -> true
    }
    matchesSearch && matchesRole
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text("User Management & Trust Control", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateTextPrimary)

    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search users by name or email...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextMuted) },
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth(),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = SurfaceWhite
      )
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("ALL", "CUSTOMER", "WORKER").forEach { r ->
        val isSelected = filterRole == r
        Surface(
          modifier = Modifier.clickable { filterRole = r },
          shape = RoundedCornerShape(8.dp),
          color = if (isSelected) IndigoPrimary else SurfaceWhite,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoPrimary else SlateBorder)
        ) {
          Text(
            text = r,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else SlateTextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }
      }
    }

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(filteredUsers) { user ->
        HommieCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(if (user.role == UserRole.WORKER) EmeraldLight else IndigoLight, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = user.avatarInitials,
                  fontWeight = FontWeight.Bold,
                  color = if (user.role == UserRole.WORKER) EmeraldSuccess else IndigoPrimary
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(user.email, fontSize = 12.sp, color = SlateTextMuted)
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              HommieBadge(
                text = user.role.name,
                backgroundColor = if (user.role == UserRole.WORKER) EmeraldLight else IndigoLight,
                textColor = if (user.role == UserRole.WORKER) EmeraldSuccess else IndigoPrimary
              )
              Spacer(modifier = Modifier.height(4.dp))
              HommieBadge(
                text = user.status.name,
                backgroundColor = if (user.status == UserStatus.ACTIVE) EmeraldLight else RoseLight,
                textColor = if (user.status == UserStatus.ACTIVE) EmeraldSuccess else RoseDestructive
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = SlateBorderLight)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = { HommieRepository.toggleUserSuspension(user.id) }
            ) {
              Text(
                text = if (user.status == UserStatus.ACTIVE) "Suspend User" else "Reactivate User",
                color = if (user.status == UserStatus.ACTIVE) RoseDestructive else EmeraldSuccess,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminVerificationsScreen() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text("Worker Credential Verifications", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateTextPrimary)
    Text("Inspect trade licenses and ID documents to grant Verified Pro badges.", fontSize = 12.sp, color = SlateTextMuted)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      items(HommieRepository.verificationQueue) { ver ->
        HommieCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(ver.workerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("Trade: ${ver.tradeType}", fontSize = 12.sp, color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
            }
            HommieBadge(
              text = ver.status,
              backgroundColor = when (ver.status) {
                "APPROVED" -> EmeraldLight
                "REJECTED" -> RoseLight
                else -> AmberLight
              },
              textColor = when (ver.status) {
                "APPROVED" -> EmeraldSuccess
                "REJECTED" -> RoseDestructive
                else -> AmberWarning
              }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text("Trade License: ${ver.licenseNumber}", fontSize = 12.sp, color = SlateTextPrimary)
          Text("Document: ${ver.idDocument}", fontSize = 12.sp, color = SlateTextMuted)
          Text("Background Check: ${ver.backgroundCheckStatus}", fontSize = 12.sp, color = SlateTextMuted)
          Text("Submitted: ${ver.submittedAt}", fontSize = 11.sp, color = SlateTextSubtle)

          if (ver.status == "PENDING") {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              HommieButton(
                text = "Reject",
                onClick = { HommieRepository.rejectWorkerVerification(ver.id) },
                variant = ButtonVariant.DESTRUCTIVE,
                modifier = Modifier.weight(1f).height(38.dp)
              )
              HommieButton(
                text = "Approve Pro Badge",
                onClick = { HommieRepository.approveWorkerVerification(ver.id) },
                variant = ButtonVariant.SUCCESS,
                modifier = Modifier.weight(1.5f).height(38.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminDisputesScreen() {
  val disputedBookings = HommieRepository.bookings.filter {
    it.status == BookingStatus.DISPUTED ||
    it.status == BookingStatus.RESOLVED_REFUND ||
    it.status == BookingStatus.RESOLVED_RELEASE ||
    it.status == BookingStatus.RESOLVED_PARTIAL_REFUND
  }

  var selectedDisputeBooking by remember { mutableStateOf<Booking?>(null) }
  var resolutionNoteInput by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text("Escrow Dispute Resolution Console", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateTextPrimary)
    Text("Arbitrate held escrow claims with full refund, release, or 50/50 split.", fontSize = 12.sp, color = SlateTextMuted)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      if (disputedBookings.isEmpty()) {
        item {
          HommieCard {
            Text("No active dispute claims in queue.", color = SlateTextMuted, fontSize = 13.sp)
          }
        }
      }

      items(disputedBookings) { booking ->
        val dispute = booking.dispute
        HommieCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Dispute #${dispute?.id ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoseDestructive)
              Text("Job #${booking.id.takeLast(6).uppercase()}", fontSize = 11.sp, color = SlateTextMuted)
            }
            StatusBadge(status = booking.status)
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text("Reason: ${dispute?.reason?.label ?: "Dispute claim"}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          Text("Claim details: ${dispute?.description ?: "Under review"}", fontSize = 12.sp, color = SlateTextMuted)
          Spacer(modifier = Modifier.height(6.dp))
          Text("Held in Escrow: $${booking.pricing.totalHeldAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = IndigoDeep)

          Spacer(modifier = Modifier.height(10.dp))

          // Execution Buttons if OPEN
          if (booking.status == BookingStatus.DISPUTED) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              HommieButton(
                text = "1. Full Refund to Customer ($${booking.pricing.totalHeldAmount})",
                onClick = {
                  HommieRepository.resolveDispute(booking.id, DisputeStatus.RESOLVED_REFUND, "Full refund processed by Admin Sarah Chen.")
                },
                variant = ButtonVariant.DESTRUCTIVE,
                modifier = Modifier.fillMaxWidth().height(38.dp)
              )
              HommieButton(
                text = "2. Release Funds to Worker ($${booking.pricing.baseAmount})",
                onClick = {
                  HommieRepository.resolveDispute(booking.id, DisputeStatus.RESOLVED_RELEASE, "Proof of completed work confirmed by Admin.")
                },
                variant = ButtonVariant.SUCCESS,
                modifier = Modifier.fillMaxWidth().height(38.dp)
              )
              HommieButton(
                text = "3. Partial 50/50 Refund Split",
                onClick = {
                  HommieRepository.resolveDispute(booking.id, DisputeStatus.RESOLVED_PARTIAL_REFUND, "Compromise settlement: 50% refund, 50% release.")
                },
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth().height(38.dp)
              )
            }
          } else {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = EmeraldLight
            ) {
              Text(
                text = "✓ Resolved: ${dispute?.resolutionNote ?: "Resolution complete"}",
                fontSize = 12.sp,
                color = EmeraldSuccess,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }
      }
    }
  }
}
