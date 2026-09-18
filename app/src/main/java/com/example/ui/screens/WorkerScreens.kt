package com.example.ui.screens

import androidx.compose.animation.*
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
import java.util.Locale

@Composable
fun WorkerDashboardScreen(
  onSelectBooking: (Booking) -> Unit
) {
  var showIncomingOfferModal by remember { mutableStateOf(false) }

  // Check if there is an active offer for this worker
  val incomingOffer = HommieRepository.bookings.find {
    it.id == HommieRepository.activeOfferBookingId && it.status == BookingStatus.OFFERED
  }

  val activeJobs = HommieRepository.bookings.filter {
    listOf(
      BookingStatus.ACCEPTED,
      BookingStatus.EN_ROUTE,
      BookingStatus.IN_PROGRESS,
      BookingStatus.COMPLETED
    ).contains(it.status)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Availability Status 3-Way Pill
    HommieCard {
      Text("Provider Availability Status", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        WorkerAvailability.values().forEach { status ->
          val isSelected = HommieRepository.workerAvailability == status
          val (color, label) = when (status) {
            WorkerAvailability.ONLINE -> EmeraldSuccess to "ONLINE"
            WorkerAvailability.BUSY -> AmberWarning to "BUSY"
            WorkerAvailability.OFFLINE -> SlateTextSubtle to "OFFLINE"
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .clickable {
                HommieRepository.workerAvailability = status
                HommieRepository.pushEvent("Worker set availability to ${status.name}")
              },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) color.copy(alpha = 0.15f) else SlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) color else SlateBorder)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(color, CircleShape)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) color else SlateTextPrimary
              )
            }
          }
        }
      }
    }

    // Incoming Job Offer Banner (with live countdown!)
    if (incomingOffer != null) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showIncomingOfferModal = true },
        shape = RoundedCornerShape(16.dp),
        color = AmberLight,
        border = androidx.compose.foundation.BorderStroke(2.dp, AmberWarning)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(AmberWarning, CircleShape)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("INCOMING JOB OFFER", fontWeight = FontWeight.Black, fontSize = 13.sp, color = AmberWarning)
            }
            Text(
              text = "${HommieRepository.offerRemainingSeconds / 60}:${String.format(Locale.getDefault(), "%02d", HommieRepository.offerRemainingSeconds % 60)}",
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              color = AmberWarning
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(incomingOffer.serviceName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = SlateTextPrimary)
          Text("${incomingOffer.customerName} • ${incomingOffer.address}", fontSize = 13.sp, color = SlateTextMuted)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Estimated Payout: $${incomingOffer.pricing.baseAmount} (Authorized in Escrow)",
            fontWeight = FontWeight.Bold,
            color = EmeraldSuccess,
            fontSize = 14.sp
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HommieButton(
              text = "Decline",
              onClick = { HommieRepository.declineOffer(incomingOffer.id) },
              variant = ButtonVariant.OUTLINE,
              modifier = Modifier.weight(1f).height(40.dp)
            )
            HommieButton(
              text = "Accept Job Offer",
              onClick = {
                val accepted = HommieRepository.acceptOffer(incomingOffer.id)
                if (accepted) {
                  showIncomingOfferModal = false
                }
              },
              variant = ButtonVariant.PRIMARY,
              modifier = Modifier.weight(1.5f).height(40.dp)
            )
          }
        }
      }
    }

    // Active Jobs Section with Instant Status Transitions
    Text("Active Jobs & Dispatches (${activeJobs.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateTextPrimary)

    if (activeJobs.isEmpty()) {
      HommieCard {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(40.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text("No Active Jobs Right Now", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
          Text("Keep availability ONLINE to receive nearby dispatches.", color = SlateTextMuted, fontSize = 12.sp)
        }
      }
    }

    activeJobs.forEach { job ->
      WorkerJobCard(
        job = job,
        onSelect = { onSelectBooking(job) }
      )
    }

    // Quick Metrics Overview
    HommieCard {
      Text("Provider Ledger Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Available for Payout", fontSize = 12.sp, color = SlateTextMuted)
          Text("$${HommieRepository.availableEarnings}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = EmeraldSuccess)
        }
        Column {
          Text("Held in Escrow", fontSize = 12.sp, color = SlateTextMuted)
          Text("$${HommieRepository.heldFunds}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = IndigoPrimary)
        }
        Column {
          Text("Lifetime Earned", fontSize = 12.sp, color = SlateTextMuted)
          Text("$${HommieRepository.lifetimeEarnings}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = SlateTextPrimary)
        }
      }
    }
  }

  // Incoming Offer Full-Screen Dialog
  if (showIncomingOfferModal && incomingOffer != null) {
    AlertDialog(
      onDismissRequest = { showIncomingOfferModal = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("New Job Offer", fontWeight = FontWeight.Bold)
          Text(
            text = "${HommieRepository.offerRemainingSeconds / 60}:${String.format(Locale.getDefault(), "%02d", HommieRepository.offerRemainingSeconds % 60)}",
            fontWeight = FontWeight.Black,
            color = AmberWarning
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          LinearProgressIndicator(
            progress = { HommieRepository.offerRemainingSeconds / 180f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = AmberWarning
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(incomingOffer.serviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IndigoDeep)
          Text("Customer: ${incomingOffer.customerName}", fontSize = 13.sp)
          Text("Location: ${incomingOffer.address}", fontSize = 13.sp)
          Text("Schedule: ${incomingOffer.scheduledDate} • ${incomingOffer.scheduledTime}", fontSize = 13.sp)
          Text("Customer Notes: ${incomingOffer.notes}", fontSize = 12.sp, color = SlateTextMuted)
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = EmeraldLight
          ) {
            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Your Payout:", fontWeight = FontWeight.SemiBold, color = EmeraldSuccess)
              Text("$${incomingOffer.pricing.baseAmount}", fontWeight = FontWeight.Black, color = EmeraldSuccess)
            }
          }
        }
      },
      confirmButton = {
        HommieButton(
          text = "Accept Offer",
          onClick = {
            val ok = HommieRepository.acceptOffer(incomingOffer.id)
            if (ok) showIncomingOfferModal = false
          }
        )
      },
      dismissButton = {
        TextButton(onClick = {
          HommieRepository.declineOffer(incomingOffer.id)
          showIncomingOfferModal = false
        }) {
          Text("Decline")
        }
      }
    )
  }
}

@Composable
fun WorkerJobCard(
  job: Booking,
  onSelect: () -> Unit
) {
  HommieCard(onClick = onSelect) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "#${job.id.takeLast(6).uppercase()}",
        fontWeight = FontWeight.Bold,
        color = SlateTextMuted,
        fontSize = 12.sp
      )
      StatusBadge(status = job.status)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(job.serviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateTextPrimary)
    Text("Customer: ${job.customerName} • ${job.address}", fontSize = 12.sp, color = SlateTextMuted)

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = SlateBorderLight)
    Spacer(modifier = Modifier.height(10.dp))

    // Step Transition Controls for Worker
    when (job.status) {
      BookingStatus.ACCEPTED -> {
        HommieButton(
          text = "Mark En Route ➔",
          onClick = { HommieRepository.updateBookingStatus(job.id, BookingStatus.EN_ROUTE) },
          variant = ButtonVariant.PRIMARY,
          modifier = Modifier.fillMaxWidth().height(42.dp)
        )
      }
      BookingStatus.EN_ROUTE -> {
        HommieButton(
          text = "Start Work (In Progress) ➔",
          onClick = { HommieRepository.updateBookingStatus(job.id, BookingStatus.IN_PROGRESS) },
          variant = ButtonVariant.PRIMARY,
          modifier = Modifier.fillMaxWidth().height(42.dp)
        )
      }
      BookingStatus.IN_PROGRESS -> {
        HommieButton(
          text = "Mark Job Completed ✓",
          onClick = { HommieRepository.updateBookingStatus(job.id, BookingStatus.COMPLETED) },
          variant = ButtonVariant.SUCCESS,
          modifier = Modifier.fillMaxWidth().height(42.dp)
        )
      }
      BookingStatus.COMPLETED -> {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = EmeraldLight
        ) {
          Text(
            text = "✓ Completed! Awaiting customer inspection & escrow release.",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = EmeraldSuccess,
            modifier = Modifier.padding(10.dp)
          )
        }
      }
      else -> {}
    }
  }
}

@Composable
fun ServicesPricingScreen() {
  val currentWorker = HommieRepository.workers.find { it.id == HommieRepository.currentUser.id }
    ?: HommieRepository.workers.first()

  var showAddServiceDialog by remember { mutableStateOf(false) }
  var selectedCatId by remember { mutableStateOf(HommieRepository.catalogCategories.first().id) }
  var customRateInput by remember { mutableStateOf("95.0") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    HommieCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("My Offered Services & Rates", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateTextPrimary)
          Text("Customize rates for each trade you offer", fontSize = 12.sp, color = SlateTextMuted)
        }
        HommieButton(
          text = "+ Add Service",
          onClick = { showAddServiceDialog = true },
          modifier = Modifier.height(36.dp)
        )
      }
    }

    currentWorker.services.forEach { s ->
      HommieCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(s.serviceName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)
            Text(
              text = if (s.isActive) "Active on Marketplace" else "Paused (Hidden from Search)",
              fontSize = 12.sp,
              color = if (s.isActive) EmeraldSuccess else SlateTextMuted
            )
          }
          Text("$${s.hourlyRate.toInt()}/hr", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = IndigoPrimary)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = { HommieRepository.toggleWorkerService(currentWorker.id, s.serviceId) }) {
            Text(if (s.isActive) "Pause" else "Activate", color = if (s.isActive) AmberWarning else EmeraldSuccess)
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(onClick = { HommieRepository.removeWorkerService(currentWorker.id, s.serviceId) }) {
            Text("Remove", color = RoseDestructive)
          }
        }
      }
    }
  }

  // Add Service Dialog
  if (showAddServiceDialog) {
    AlertDialog(
      onDismissRequest = { showAddServiceDialog = false },
      title = { Text("Add Service from Catalog", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Select Service Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          HommieRepository.catalogCategories.forEach { cat ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedCatId = cat.id }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedCatId == cat.id,
                onClick = { selectedCatId = cat.id }
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(cat.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Suggested: $${cat.defaultHourlyRate.toInt()}/hr", fontSize = 11.sp, color = SlateTextMuted)
              }
            }
          }
          OutlinedTextField(
            value = customRateInput,
            onValueChange = { customRateInput = it },
            label = { Text("Your Custom Hourly Rate ($)") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        HommieButton(
          text = "Save Service",
          onClick = {
            val rate = customRateInput.toDoubleOrNull() ?: 90.0
            HommieRepository.addServiceToWorker(currentWorker.id, selectedCatId, rate)
            showAddServiceDialog = false
          }
        )
      },
      dismissButton = {
        TextButton(onClick = { showAddServiceDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun EarningsPayoutsScreen() {
  var showWithdrawDialog by remember { mutableStateOf(false) }
  var withdrawAmountInput by remember { mutableStateOf("150.0") }
  var bankNameInput by remember { mutableStateOf("JPMorgan Chase") }
  var accountLast4Input by remember { mutableStateOf("4821") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Balance Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = IndigoDeep
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("Available Balance", fontSize = 13.sp, color = IndigoLight)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "$${HommieRepository.availableEarnings}",
          fontSize = 32.sp,
          fontWeight = FontWeight.Black,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Pending Escrow", fontSize = 12.sp, color = IndigoLight)
            Text("$${HommieRepository.heldFunds}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
          Column {
            Text("Lifetime Earnings", fontSize = 12.sp, color = IndigoLight)
            Text("$${HommieRepository.lifetimeEarnings}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HommieButton(
          text = "Instant Bank Payout / Withdraw",
          icon = Icons.Default.AccountBalance,
          variant = ButtonVariant.PRIMARY,
          onClick = { showWithdrawDialog = true },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // Ledger History
    Text("Chronological Earnings Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateTextPrimary)

    HommieCard {
      if (HommieRepository.ledgerEntries.isEmpty()) {
        Text("No ledger activity yet", color = SlateTextMuted, fontSize = 13.sp)
      } else {
        HommieRepository.ledgerEntries.forEach { entry ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SlateTextPrimary)
              Text("${entry.date} • ${entry.type.name}", fontSize = 11.sp, color = SlateTextMuted)
            }
            Text(
              text = if (entry.isCredit) "+$${entry.amount}" else "-$${entry.amount}",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = if (entry.isCredit) EmeraldSuccess else RoseDestructive
            )
          }
          HorizontalDivider(color = SlateBorderLight)
        }
      }
    }

    // Payout Requests
    Text("Recent Bank Withdrawals", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateTextPrimary)

    HommieCard {
      if (HommieRepository.payoutRecords.isEmpty()) {
        Text("No past withdrawal requests.", color = SlateTextMuted, fontSize = 13.sp)
      } else {
        HommieRepository.payoutRecords.forEach { po ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("${po.bankName} (••${po.accountLast4})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Text("Requested: ${po.requestedAt}", fontSize = 11.sp, color = SlateTextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("$${po.amount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              HommieBadge(text = po.status, backgroundColor = EmeraldLight, textColor = EmeraldSuccess)
            }
          }
          HorizontalDivider(color = SlateBorderLight)
        }
      }
    }
  }

  // Payout Dialog
  if (showWithdrawDialog) {
    AlertDialog(
      onDismissRequest = { showWithdrawDialog = false },
      title = { Text("Request Bank Withdrawal", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Available Balance: $${HommieRepository.availableEarnings}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EmeraldSuccess)
          OutlinedTextField(
            value = withdrawAmountInput,
            onValueChange = { withdrawAmountInput = it },
            label = { Text("Withdrawal Amount ($)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = bankNameInput,
            onValueChange = { bankNameInput = it },
            label = { Text("Bank Name") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = accountLast4Input,
            onValueChange = { accountLast4Input = it },
            label = { Text("Account Last 4 Digits") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        HommieButton(
          text = "Submit Transfer",
          onClick = {
            val amt = withdrawAmountInput.toDoubleOrNull() ?: 0.0
            val ok = HommieRepository.requestPayout(amt, bankNameInput, accountLast4Input)
            if (ok) showWithdrawDialog = false
          }
        )
      },
      dismissButton = {
        TextButton(onClick = { showWithdrawDialog = false }) { Text("Cancel") }
      }
    )
  }
}
