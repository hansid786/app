package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HommieRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerExploreScreen(
  onSelectWorker: (WorkerProfile) -> Unit,
  onBookWorker: (WorkerProfile) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var showFilterSheet by remember { mutableStateOf(false) }
  var verifiedOnly by remember { mutableStateOf(false) }
  var maxRate by remember { mutableStateOf(150f) }
  var sortBy by remember { mutableStateOf("Rating: High to Low") }

  val categories = listOf("All", "Plumbing", "Electrical", "Deep Cleaning", "Handyman", "Appliance Repair", "Lawn & Garden")

  val filteredWorkers = HommieRepository.workers.filter { worker ->
    val matchesSearch = worker.name.contains(searchQuery, ignoreCase = true) ||
      worker.skills.any { it.contains(searchQuery, ignoreCase = true) } ||
      worker.bio.contains(searchQuery, ignoreCase = true)

    val matchesCategory = if (selectedCategory == "All") true else {
      worker.services.any { it.serviceName.contains(selectedCategory, ignoreCase = true) } ||
      worker.skills.any { it.contains(selectedCategory, ignoreCase = true) }
    }

    val matchesVerified = if (verifiedOnly) worker.isVerified else true
    val matchesRate = worker.baseHourlyRate <= maxRate

    matchesSearch && matchesCategory && matchesVerified && matchesRate
  }.sortedWith { a, b ->
    when (sortBy) {
      "Price: Low to High" -> a.baseHourlyRate.compareTo(b.baseHourlyRate)
      "Price: High to Low" -> b.baseHourlyRate.compareTo(a.baseHourlyRate)
      "Most Reviewed" -> b.reviewCount.compareTo(a.reviewCount)
      else -> b.rating.compareTo(a.rating)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
  ) {
    // Search & Filter Header
    Column(
      modifier = Modifier
        .background(SurfaceWhite)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search plumbers, electricians, cleaners...", fontSize = 14.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextMuted)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = SlateTextMuted)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IndigoPrimary,
            unfocusedBorderColor = SlateBorder,
            focusedContainerColor = SlateBackground,
            unfocusedContainerColor = SlateBackground
          ),
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Filter Button
        Surface(
          modifier = Modifier
            .size(52.dp)
            .clickable { showFilterSheet = true },
          shape = RoundedCornerShape(12.dp),
          color = if (verifiedOnly || maxRate < 150f) IndigoLight else SlateBackground,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (verifiedOnly || maxRate < 150f) IndigoPrimary else SlateBorder)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Filter",
              tint = if (verifiedOnly || maxRate < 150f) IndigoPrimary else SlateTextPrimary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Category Pill Selector
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        categories.forEach { cat ->
          val isSelected = cat == selectedCategory
          Surface(
            modifier = Modifier.clickable { selectedCategory = cat },
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) IndigoPrimary else SlateSurface,
            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
          ) {
            Text(
              text = cat,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color.White else SlateTextPrimary,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
          }
        }
      }
    }

    // Worker Cards List
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Top Verified Pros (${filteredWorkers.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
          Text(
            text = "Escrow Guaranteed",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = IndigoPrimary
          )
        }
      }

      if (filteredWorkers.isEmpty()) {
        item {
          HommieCard {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No Pros Found", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
              Text("Try widening your filters or search keywords.", color = SlateTextMuted, fontSize = 13.sp)
            }
          }
        }
      }

      items(filteredWorkers) { worker ->
        WorkerCard(
          worker = worker,
          onCardClick = { onSelectWorker(worker) },
          onBookClick = { onBookWorker(worker) }
        )
      }
    }
  }

  // Filter Bottom Sheet
  if (showFilterSheet) {
    ModalBottomSheet(
      onDismissRequest = { showFilterSheet = false },
      containerColor = SurfaceWhite
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Text("Filter & Sort Providers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // Verified Pro Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Verified Pros Only", fontWeight = FontWeight.SemiBold, color = SlateTextPrimary)
            Text("Trade license & background verified", fontSize = 12.sp, color = SlateTextMuted)
          }
          Switch(
            checked = verifiedOnly,
            onCheckedChange = { verifiedOnly = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SlateBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // Max Hourly Rate Slider
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Max Hourly Rate", fontWeight = FontWeight.SemiBold, color = SlateTextPrimary)
          Text("$${maxRate.toInt()}/hr", fontWeight = FontWeight.Bold, color = IndigoPrimary)
        }
        Slider(
          value = maxRate,
          onValueChange = { maxRate = it },
          valueRange = 40f..200f,
          steps = 15,
          colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoPrimary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sort By
        Text("Sort By", fontWeight = FontWeight.SemiBold, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        val sortOptions = listOf("Rating: High to Low", "Price: Low to High", "Price: High to Low", "Most Reviewed")
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          sortOptions.forEach { opt ->
            val isSelected = opt == sortBy
            Surface(
              modifier = Modifier.clickable { sortBy = opt },
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) IndigoLight else SlateSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IndigoPrimary else SlateBorder)
            ) {
              Text(
                text = opt,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) IndigoDeep else SlateTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HommieButton(
          text = "Apply Filters",
          onClick = { showFilterSheet = false },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
fun WorkerCard(
  worker: WorkerProfile,
  onCardClick: () -> Unit,
  onBookClick: () -> Unit
) {
  HommieCard(
    onClick = onCardClick,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top
    ) {
      // Avatar with Initials
      Box(
        modifier = Modifier
          .size(54.dp)
          .background(IndigoSoft, CircleShape)
          .border(1.5.dp, IndigoPrimary, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = worker.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
          fontWeight = FontWeight.Black,
          fontSize = 18.sp,
          color = IndigoDeep
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = worker.name,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = SlateTextPrimary
            )
            if (worker.isVerified) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verified Pro",
                tint = IndigoPrimary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Text(
            text = "$${worker.baseHourlyRate.toInt()}/hr",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = IndigoPrimary
          )
        }

        Spacer(modifier = Modifier.height(4.dp))
        RatingStars(rating = worker.rating, reviewCount = worker.reviewCount, starSize = 14)

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = worker.bio,
          fontSize = 13.sp,
          color = SlateTextMuted,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Skills tags
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          worker.skills.take(3).forEach { skill ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SlateSurface
            ) {
              Text(
                text = skill,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SlateTextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(
                  when (worker.availability) {
                    WorkerAvailability.ONLINE -> EmeraldSuccess
                    WorkerAvailability.BUSY -> AmberWarning
                    WorkerAvailability.OFFLINE -> SlateTextSubtle
                  },
                  CircleShape
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when (worker.availability) {
                WorkerAvailability.ONLINE -> "Available Now"
                WorkerAvailability.BUSY -> "Busy (Schedule)"
                WorkerAvailability.OFFLINE -> "Offline"
              },
              fontSize = 12.sp,
              color = SlateTextMuted,
              fontWeight = FontWeight.Medium
            )
          }

          HommieButton(
            text = "Book Pro",
            onClick = onBookClick,
            modifier = Modifier.height(38.dp)
          )
        }
      }
    }
  }
}

@Composable
fun WorkerPublicProfileModal(
  worker: WorkerProfile,
  onDismiss: () -> Unit,
  onBookNow: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      // Header with Back Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(IndigoPrimary)
          .padding(top = 16.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
      ) {
        Column {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(68.dp)
                .background(Color.White, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = worker.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = IndigoPrimary
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = worker.name,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                if (worker.isVerified) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              RatingStars(rating = worker.rating, reviewCount = worker.reviewCount, starSize = 16)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${worker.completedJobsCount} verified jobs completed",
                fontSize = 12.sp,
                color = IndigoLight
              )
            }
          }
        }
      }

      Column(modifier = Modifier.padding(16.dp)) {
        // Escrow Security
        EscrowSecurityBanner()

        Spacer(modifier = Modifier.height(16.dp))

        // About Bio
        HommieCard {
          Text("About the Professional", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = worker.bio, fontSize = 14.sp, color = SlateTextMuted, lineHeight = 20.sp)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Skills & Credentials", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SlateTextPrimary)
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            worker.skills.forEach { skill ->
              Surface(shape = RoundedCornerShape(6.dp), color = IndigoLight) {
                Text(skill, color = IndigoDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Services & Pricing
        HommieCard {
          Text("Services & Hourly Rates", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)
          Spacer(modifier = Modifier.height(8.dp))
          worker.services.forEach { service ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(service.serviceName, fontWeight = FontWeight.SemiBold, color = SlateTextPrimary, fontSize = 14.sp)
                Text("Includes standard tooling & safety prep", fontSize = 12.sp, color = SlateTextMuted)
              }
              Text("$${service.hourlyRate.toInt()}/hr", fontWeight = FontWeight.Bold, color = IndigoPrimary, fontSize = 15.sp)
            }
            HorizontalDivider(color = SlateBorderLight)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verified Customer Reviews
        HommieCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Verified Reviews (${worker.reviews.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)
            RatingStars(rating = worker.rating, starSize = 14)
          }
          Spacer(modifier = Modifier.height(12.dp))
          worker.reviews.forEach { rev ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(rev.customerName, fontWeight = FontWeight.SemiBold, color = SlateTextPrimary, fontSize = 13.sp)
                Text(rev.date, fontSize = 11.sp, color = SlateTextSubtle)
              }
              RatingStars(rating = rev.rating.toDouble(), starSize = 12)
              Spacer(modifier = Modifier.height(4.dp))
              Text(rev.comment, fontSize = 13.sp, color = SlateTextMuted, lineHeight = 18.sp)
            }
            HorizontalDivider(color = SlateBorderLight)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        HommieButton(
          text = "Continue to Booking ($${worker.baseHourlyRate.toInt()}/hr)",
          onClick = onBookNow,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingWizardSheet(
  worker: WorkerProfile,
  onDismiss: () -> Unit,
  onBookingCreated: (Booking) -> Unit
) {
  var selectedServiceId by remember {
    mutableStateOf(worker.services.firstOrNull()?.serviceId ?: "cat_plumbing")
  }
  var estimatedHours by remember { mutableStateOf(2.0) }
  var scheduledDate by remember { mutableStateOf("Tomorrow") }
  var scheduledTime by remember { mutableStateOf("10:00 AM") }
  var selectedAddress by remember {
    mutableStateOf(HommieRepository.savedAddresses.firstOrNull()?.street ?: "742 Evergreen Terrace, Metro Springs")
  }
  var customNotes by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }

  val activeService = worker.services.find { it.serviceId == selectedServiceId }
    ?: worker.services.firstOrNull()
    ?: WorkerServiceItem("cat_default", "General Service", worker.baseHourlyRate)

  val priceBreakdown = PriceBreakdown.calculate(activeService.hourlyRate, estimatedHours)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = SurfaceWhite
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Schedule & Authorize", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
          Text("Booking with ${worker.name}", fontSize = 13.sp, color = SlateTextMuted)
        }
        Text("$${priceBreakdown.totalHeldAmount}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = IndigoPrimary)
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 1. Service Selection
      Text("1. Select Service", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(8.dp))
      worker.services.forEach { s ->
        val isSelected = s.serviceId == selectedServiceId
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { selectedServiceId = s.serviceId },
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) IndigoLight else SlateBackground,
          border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) IndigoPrimary else SlateBorder)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(s.serviceName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = SlateTextPrimary)
            Text("$${s.hourlyRate.toInt()}/hr", fontWeight = FontWeight.Bold, color = IndigoPrimary)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Estimated Hours
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Estimated Duration: ${estimatedHours.toInt()} Hours", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SlateTextPrimary)
        Row {
          IconButton(
            onClick = { if (estimatedHours > 1.0) estimatedHours -= 1.0 },
            modifier = Modifier.size(32.dp).background(SlateSurface, CircleShape)
          ) {
            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = { if (estimatedHours < 8.0) estimatedHours += 1.0 },
            modifier = Modifier.size(32.dp).background(SlateSurface, CircleShape)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Schedule Date & Time
      Text("2. Date & Time", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Today (Express)", "Tomorrow", "This Saturday").forEach { d ->
          val isSelected = d == scheduledDate
          Surface(
            modifier = Modifier.weight(1f).clickable { scheduledDate = d },
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) IndigoLight else SlateSurface,
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary) else null
          ) {
            Text(
              text = d,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) IndigoPrimary else SlateTextPrimary,
              modifier = Modifier.padding(vertical = 8.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Service Location
      Text("3. Service Address", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(8.dp))
      HommieRepository.savedAddresses.forEach { addr ->
        val fullAddr = "${addr.street}, ${addr.city}"
        val isSelected = selectedAddress == fullAddr
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { selectedAddress = fullAddr },
          shape = RoundedCornerShape(8.dp),
          color = if (isSelected) IndigoLight else SlateSurface,
          border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary) else null
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (addr.label == "Home") Icons.Default.Home else Icons.Default.LocationOn,
              contentDescription = null,
              tint = if (isSelected) IndigoPrimary else SlateTextMuted,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(addr.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SlateTextPrimary)
              Text(fullAddr, fontSize = 11.sp, color = SlateTextMuted)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Job Notes
      Text("4. Job Instructions / Notes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = customNotes,
        onValueChange = { customNotes = it },
        placeholder = { Text("Describe the issue, gate codes, parking tips...", fontSize = 13.sp) },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = IndigoPrimary,
          unfocusedBorderColor = SlateBorder
        ),
        modifier = Modifier.fillMaxWidth().height(90.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Price Breakdown (Server-Calculated Escrow)
      HommieCard(backgroundColor = SlateBackground) {
        Text("Escrow Price Breakdown", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Base Rate (${estimatedHours.toInt()} hrs @ $${activeService.hourlyRate.toInt()}/hr)", fontSize = 12.sp, color = SlateTextMuted)
          Text("$${priceBreakdown.baseAmount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Hommie Trust & Platform Fee (10%)", fontSize = 12.sp, color = SlateTextMuted)
          Text("$${priceBreakdown.platformFee}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Estimated Sales Tax (8.25%)", fontSize = 12.sp, color = SlateTextMuted)
          Text("$${priceBreakdown.tax}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = SlateBorder)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Authorized & Held Amount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IndigoDeep)
          Text("$${priceBreakdown.totalHeldAmount}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = IndigoPrimary)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      EscrowSecurityBanner(amount = priceBreakdown.totalHeldAmount)

      Spacer(modifier = Modifier.height(20.dp))

      HommieButton(
        text = "Authorize Payment & Dispatch Offer",
        icon = Icons.Filled.Lock,
        isLoading = isSubmitting,
        onClick = {
          isSubmitting = true
          val booking = HommieRepository.createBooking(
            workerId = worker.id,
            serviceId = activeService.serviceId,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            address = selectedAddress,
            notes = if (customNotes.isBlank()) "Standard diagnostic and repair requested" else customNotes,
            hours = estimatedHours
          )
          isSubmitting = false
          onBookingCreated(booking)
        },
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
fun CustomerBookingsScreen(
  onSelectBooking: (Booking) -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }
  val tabs = listOf("Active & Live", "Completed", "All")

  val displayedBookings = when (selectedTab) {
    0 -> HommieRepository.bookings.filter {
      listOf(
        BookingStatus.REQUESTED,
        BookingStatus.PAYMENT_PENDING,
        BookingStatus.OFFERED,
        BookingStatus.ACCEPTED,
        BookingStatus.EN_ROUTE,
        BookingStatus.IN_PROGRESS,
        BookingStatus.COMPLETED,
        BookingStatus.DISPUTED
      ).contains(it.status)
    }
    1 -> HommieRepository.bookings.filter {
      listOf(BookingStatus.CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.RESOLVED_REFUND, BookingStatus.RESOLVED_RELEASE).contains(it.status)
    }
    else -> HommieRepository.bookings
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
  ) {
    // Header
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = SurfaceWhite,
      shadowElevation = 1.dp
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("My Service Bookings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = SurfaceWhite,
          contentColor = IndigoPrimary,
          divider = {}
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Text(
                  text = title,
                  fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 13.sp
                )
              }
            )
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (displayedBookings.isEmpty()) {
        item {
          HommieCard {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Outlined.EventNote, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No Bookings in this Category", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
              Text("Browse the marketplace to book a verified professional.", color = SlateTextMuted, fontSize = 13.sp)
            }
          }
        }
      }

      items(displayedBookings) { booking ->
        BookingSummaryCard(
          booking = booking,
          onClick = { onSelectBooking(booking) }
        )
      }
    }
  }
}

@Composable
fun BookingSummaryCard(
  booking: Booking,
  onClick: () -> Unit
) {
  HommieCard(onClick = onClick) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "#${booking.id.takeLast(6).uppercase()}",
        fontWeight = FontWeight.Bold,
        color = SlateTextMuted,
        fontSize = 12.sp
      )
      StatusBadge(status = booking.status)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = booking.serviceName,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      color = SlateTextPrimary
    )

    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Person, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(booking.workerName, fontSize = 13.sp, color = SlateTextPrimary, fontWeight = FontWeight.Medium)
      Spacer(modifier = Modifier.width(12.dp))
      Icon(Icons.Default.Schedule, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("${booking.scheduledDate} • ${booking.scheduledTime}", fontSize = 12.sp, color = SlateTextMuted)
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = SlateBorderLight)
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Held in Escrow: $${booking.pricing.totalHeldAmount}",
        fontWeight = FontWeight.Bold,
        color = IndigoDeep,
        fontSize = 13.sp
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("View Timeline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IndigoPrimary)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
  bookingId: String,
  onBack: () -> Unit
) {
  val booking = HommieRepository.bookings.find { it.id == bookingId }
  if (booking == null) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("Booking Not Found")
      HommieButton(text = "Go Back", onClick = onBack)
    }
    return
  }

  var showReviewModal by remember { mutableStateOf(false) }
  var showDisputeModal by remember { mutableStateOf(false) }
  var disputeMessageInput by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Booking #${booking.id.takeLast(6).uppercase()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(booking.serviceName, fontSize = 12.sp, color = SlateTextMuted)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          StatusBadge(status = booking.status)
          Spacer(modifier = Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
      )
    },
    containerColor = SlateBackground
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Real-Time Stepper Tracker
      HommieCard {
        Text("Real-Time Service Stepper", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        BookingStatusTimeline(status = booking.status)
      }

      // Live Offer Countdown Banner if in OFFERED status
      if (booking.status == BookingStatus.OFFERED && HommieRepository.activeOfferBookingId == booking.id) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberLight,
          border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              progress = { HommieRepository.offerRemainingSeconds / 180f },
              modifier = Modifier.size(36.dp),
              color = AmberWarning,
              strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Offer Awaiting Pro Acceptance: ${HommieRepository.offerRemainingSeconds / 60}:${String.format(Locale.getDefault(), "%02d", HommieRepository.offerRemainingSeconds % 60)}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AmberWarning
              )
              Text(
                text = "Dispatched to ${booking.workerName}. Pro has 3 minutes to confirm.",
                fontSize = 11.sp,
                color = SlateTextPrimary
              )
            }
          }
        }
      }

      // Worker Details Card
      HommieCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier.size(46.dp).background(IndigoLight, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(booking.workerName.take(2).uppercase(), fontWeight = FontWeight.Bold, color = IndigoPrimary)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(booking.workerName, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 15.sp)
            Text("Assigned Professional", fontSize = 12.sp, color = SlateTextMuted)
          }
          HommieButton(
            text = "Call / Message",
            onClick = { HommieRepository.pushEvent("Contacting ${booking.workerName} via secure masked proxy") },
            variant = ButtonVariant.SECONDARY,
            modifier = Modifier.height(36.dp)
          )
        }
      }

      // Appointment & Location
      HommieCard {
        Text("Service Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CalendarToday, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("${booking.scheduledDate} at ${booking.scheduledTime}", fontSize = 13.sp, color = SlateTextPrimary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.LocationOn, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(booking.address, fontSize = 13.sp, color = SlateTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Customer Instructions:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SlateTextMuted)
        Text(booking.notes, fontSize = 13.sp, color = SlateTextPrimary)
      }

      // Escrow Protection & Pricing Breakdown
      HommieCard {
        EscrowSecurityBanner(amount = booking.pricing.totalHeldAmount)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Base Amount (${booking.pricing.hours} hrs)", fontSize = 13.sp, color = SlateTextMuted)
          Text("$${booking.pricing.baseAmount}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Platform Fee (10%)", fontSize = 13.sp, color = SlateTextMuted)
          Text("$${booking.pricing.platformFee}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Tax (8.25%)", fontSize = 13.sp, color = SlateTextMuted)
          Text("$${booking.pricing.tax}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = SlateBorder)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Authorized Escrow", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("$${booking.pricing.totalHeldAmount}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = IndigoPrimary)
        }
      }

      // Contextual Action Buttons based on state machine
      when (booking.status) {
        BookingStatus.COMPLETED -> {
          // Worker finished; customer can confirm & release funds OR dispute
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = EmeraldLight,
              border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text("Job Marked Completed by Pro!", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 14.sp)
                Text(
                  text = "Please inspect the completed work. Confirming will capture and release the $${booking.pricing.baseAmount} held in escrow.",
                  fontSize = 12.sp,
                  color = SlateTextPrimary
                )
              }
            }

            HommieButton(
              text = "Confirm Completion & Release Funds",
              icon = Icons.Default.CheckCircle,
              variant = ButtonVariant.SUCCESS,
              onClick = {
                HommieRepository.updateBookingStatus(booking.id, BookingStatus.CONFIRMED)
                showReviewModal = true
              },
              modifier = Modifier.fillMaxWidth()
            )

            HommieButton(
              text = "Raise a Service Dispute",
              variant = ButtonVariant.DESTRUCTIVE,
              onClick = { showDisputeModal = true },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        BookingStatus.CONFIRMED, BookingStatus.FUNDS_RELEASED -> {
          // Completed & funds released; rate if not rated yet
          if (booking.review != null) {
            HommieCard {
              Text("Your Verified Review", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)
              Spacer(modifier = Modifier.height(6.dp))
              RatingStars(rating = booking.review.rating.toDouble(), starSize = 16)
              Spacer(modifier = Modifier.height(4.dp))
              Text(booking.review.comment, fontSize = 13.sp, color = SlateTextMuted)
            }
          } else {
            HommieButton(
              text = "Rate & Review Professional",
              icon = Icons.Default.Star,
              onClick = { showReviewModal = true },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        BookingStatus.DISPUTED -> {
          // Dispute chat thread
          val dispute = booking.dispute
          if (dispute != null) {
            HommieCard {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Dispute #${dispute.id}", fontWeight = FontWeight.Bold, color = RoseDestructive)
                HommieBadge(text = dispute.status.label, backgroundColor = RoseLight, textColor = RoseDestructive)
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text("Reason: ${dispute.reason.label}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Text(dispute.description, fontSize = 12.sp, color = SlateTextMuted)

              Spacer(modifier = Modifier.height(12.dp))
              HorizontalDivider(color = SlateBorder)
              Spacer(modifier = Modifier.height(12.dp))

              Text("Dispute Messages Thread", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(8.dp))

              dispute.messages.forEach { msg ->
                val isMe = msg.senderId == HommieRepository.currentUser.id
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                  Text("${msg.senderName} (${msg.senderRole.name}) • ${msg.timestamp}", fontSize = 10.sp, color = SlateTextSubtle)
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMe) IndigoLight else SlateSurface
                  ) {
                    Text(msg.message, fontSize = 12.sp, color = SlateTextPrimary, modifier = Modifier.padding(8.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                  value = disputeMessageInput,
                  onValueChange = { disputeMessageInput = it },
                  placeholder = { Text("Type message to pro & admin...", fontSize = 12.sp) },
                  modifier = Modifier.weight(1f).height(50.dp),
                  shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    if (disputeMessageInput.isNotBlank()) {
                      HommieRepository.sendDisputeMessage(booking.id, disputeMessageInput)
                      disputeMessageInput = ""
                    }
                  }
                ) {
                  Icon(Icons.Default.Send, contentDescription = "Send", tint = IndigoPrimary)
                }
              }
            }
          }
        }

        else -> {
          // Eligible for cancellation
          val canCancel = listOf(
            BookingStatus.REQUESTED,
            BookingStatus.PAYMENT_PENDING,
            BookingStatus.OFFERED,
            BookingStatus.ACCEPTED
          ).contains(booking.status)

          if (canCancel) {
            HommieButton(
              text = "Cancel Booking (Zero Penalty)",
              variant = ButtonVariant.OUTLINE,
              onClick = { HommieRepository.cancelBooking(booking.id) },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }
  }

  // Rate & Review Modal
  if (showReviewModal) {
    var ratingVal by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showReviewModal = false },
      title = { Text("Rate & Review Pro", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("How was your experience with ${booking.workerName}?", fontSize = 13.sp, color = SlateTextMuted)
          Spacer(modifier = Modifier.height(12.dp))
          RatingStars(rating = ratingVal.toDouble(), starSize = 28, onRatingChanged = { ratingVal = it })
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedTextField(
            value = reviewComment,
            onValueChange = { reviewComment = it },
            placeholder = { Text("Leave verified review feedback...", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            shape = RoundedCornerShape(10.dp)
          )
        }
      },
      confirmButton = {
        HommieButton(
          text = "Submit Review",
          onClick = {
            HommieRepository.submitReview(booking.id, ratingVal, if (reviewComment.isBlank()) "Excellent service!" else reviewComment)
            showReviewModal = false
          }
        )
      },
      dismissButton = {
        TextButton(onClick = { showReviewModal = false }) { Text("Later") }
      }
    )
  }

  // Raise Dispute Modal
  if (showDisputeModal) {
    var selectedReason by remember { mutableStateOf(DisputeReason.WORK_INCOMPLETE) }
    var disputeExplanation by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showDisputeModal = false },
      title = { Text("Raise Service Dispute", fontWeight = FontWeight.Bold, color = RoseDestructive) },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text("Escrow funds will remain safely locked until our admin team reviews your claim.", fontSize = 12.sp, color = SlateTextMuted)
          Spacer(modifier = Modifier.height(12.dp))
          Text("Dispute Reason", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(6.dp))
          DisputeReason.values().forEach { r ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedReason = r }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedReason == r,
                onClick = { selectedReason = r },
                colors = RadioButtonDefaults.colors(selectedColor = IndigoPrimary)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(r.label, fontSize = 12.sp)
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = disputeExplanation,
            onValueChange = { disputeExplanation = it },
            placeholder = { Text("Explain what happened in detail...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            shape = RoundedCornerShape(8.dp)
          )
        }
      },
      confirmButton = {
        HommieButton(
          text = "Submit Dispute",
          variant = ButtonVariant.DESTRUCTIVE,
          onClick = {
            if (disputeExplanation.isNotBlank()) {
              HommieRepository.raiseDispute(booking.id, selectedReason, disputeExplanation)
              showDisputeModal = false
            }
          }
        )
      },
      dismissButton = {
        TextButton(onClick = { showDisputeModal = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
fun CustomerProfileScreen() {
  var showAddAddress by remember { mutableStateOf(false) }
  var newLabel by remember { mutableStateOf("Home") }
  var newStreet by remember { mutableStateOf("") }
  var newCity by remember { mutableStateOf("Metro Springs") }
  var newZip by remember { mutableStateOf("97401") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(SlateBackground)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // User Profile Card
    HommieCard {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .background(IndigoPrimary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = HommieRepository.currentUser.avatarInitials,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(HommieRepository.currentUser.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateTextPrimary)
          Text(HommieRepository.currentUser.email, fontSize = 13.sp, color = SlateTextMuted)
          Text(HommieRepository.currentUser.phone, fontSize = 12.sp, color = SlateTextSubtle)
        }
      }
    }

    // Escrow Trust Banner
    EscrowSecurityBanner()

    // Saved Addresses
    HommieCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Saved Addresses", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)
        TextButton(onClick = { showAddAddress = true }) {
          Text("+ Add Address", color = IndigoPrimary, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      HommieRepository.savedAddresses.forEach { addr ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (addr.label == "Home") Icons.Default.Home else Icons.Default.Business,
            contentDescription = null,
            tint = IndigoPrimary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(addr.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateTextPrimary)
              if (addr.isDefault) {
                Spacer(modifier = Modifier.width(6.dp))
                HommieBadge(text = "Default", backgroundColor = IndigoLight, textColor = IndigoDeep)
              }
            }
            Text("${addr.street}, ${addr.city} ${addr.zip}", fontSize = 12.sp, color = SlateTextMuted)
          }
        }
        HorizontalDivider(color = SlateBorderLight)
      }
    }

    // Payment Methods Info
    HommieCard {
      Text("Payment & Security", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CreditCard, contentDescription = null, tint = IndigoPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text("Visa ending in 4242", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          Text("Expires 12/28 • Default Payment Method", fontSize = 12.sp, color = SlateTextMuted)
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Funds are never charged without authorization. Charges are captured only when you confirm job completion.",
        fontSize = 11.sp,
        color = SlateTextMuted
      )
    }
  }

  // Add Address Dialog
  if (showAddAddress) {
    AlertDialog(
      onDismissRequest = { showAddAddress = false },
      title = { Text("Add Saved Address", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newLabel,
            onValueChange = { newLabel = it },
            label = { Text("Label (e.g. Home, Cabin, Studio)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newStreet,
            onValueChange = { newStreet = it },
            label = { Text("Street Address") },
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = newCity,
              onValueChange = { newCity = it },
              label = { Text("City") },
              modifier = Modifier.weight(1.5f)
            )
            OutlinedTextField(
              value = newZip,
              onValueChange = { newZip = it },
              label = { Text("ZIP") },
              modifier = Modifier.weight(1f)
            )
          }
        }
      },
      confirmButton = {
        HommieButton(
          text = "Save Address",
          onClick = {
            if (newStreet.isNotBlank()) {
              HommieRepository.addSavedAddress(newLabel, newStreet, newCity, newZip)
              showAddAddress = false
            }
          }
        )
      },
      dismissButton = {
        TextButton(onClick = { showAddAddress = false }) { Text("Cancel") }
      }
    )
  }
}
