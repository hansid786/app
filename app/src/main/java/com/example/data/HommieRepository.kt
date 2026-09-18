package com.example.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.model.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object HommieRepository {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  // Current Logged In Role and User
  var currentRole by mutableStateOf(UserRole.CUSTOMER)
  var currentUser by mutableStateOf(
    User(
      id = "usr_alex_01",
      name = "Alex Rivera",
      email = "alex.rivera@example.com",
      role = UserRole.CUSTOMER,
      phone = "+1 (555) 234-5678",
      avatarInitials = "AR"
    )
  )

  // Pre-configured Users for switching roles
  val customerUser = User("usr_alex_01", "Alex Rivera", "alex.rivera@example.com", UserRole.CUSTOMER, "+1 (555) 234-5678", avatarInitials = "AR")
  val workerUser = User("wrk_marcus_01", "Marcus Vance", "marcus.vance@example.com", UserRole.WORKER, "+1 (555) 876-5432", isVerified = true, avatarInitials = "MV")
  val adminUser = User("adm_sarah_01", "Sarah Chen", "sarah.chen@hommie.internal", UserRole.ADMIN, "+1 (555) 999-0000", avatarInitials = "SC")

  // Worker availability state
  var workerAvailability by mutableStateOf(WorkerAvailability.ONLINE)

  // Incoming offer for worker
  var activeOfferBookingId by mutableStateOf<String?>("bk_live_offer_01")
  var offerRemainingSeconds by mutableStateOf(180) // 3 minute countdown timer

  // Live real-time event log for toast / transparency
  var lastEventLog by mutableStateOf<String?>("Connected to HommieState real-time cluster")

  // Catalog Categories
  val catalogCategories = listOf(
    ServiceCatalogCategory("cat_plumbing", "Plumbing", "Wrench", 95.0, "Pipe repairs, leak detection, fixture installations"),
    ServiceCatalogCategory("cat_electrical", "Electrical", "Zap", 110.0, "Panel upgrades, lighting, outlet wiring, smart home"),
    ServiceCatalogCategory("cat_cleaning", "Deep Cleaning", "Sparkles", 60.0, "Move-out cleans, sanitization, deep kitchen & bath"),
    ServiceCatalogCategory("cat_handyman", "Handyman", "Hammer", 75.0, "Drywall patch, furniture assembly, door alignment"),
    ServiceCatalogCategory("cat_appliance", "Appliance Repair", "Cpu", 85.0, "Refrigerators, washers, ovens, dishwashers"),
    ServiceCatalogCategory("cat_lawn", "Lawn & Garden", "Trees", 55.0, "Mowing, edging, seasonal cleanup, sprinkler fixes"),
    ServiceCatalogCategory("cat_painting", "Painting", "Paintbrush", 70.0, "Interior trim, touchups, accent walls, cabinet spray")
  )

  // Pro Workers Directory
  val workers = mutableStateListOf<WorkerProfile>()

  // Bookings list
  val bookings = mutableStateListOf<Booking>()

  // Notifications list
  val notifications = mutableStateListOf<NotificationItem>()

  // Users Directory for Admin
  val allUsers = mutableStateListOf<User>()

  // Verifications Queue for Admin
  val verificationQueue = mutableStateListOf<WorkerVerification>()

  // Ledger Entries for Worker
  val ledgerEntries = mutableStateListOf<LedgerEntry>()

  // Payout Records
  val payoutRecords = mutableStateListOf<PayoutRecord>()

  // Customer Saved Addresses
  val savedAddresses = mutableStateListOf<SavedAddress>()

  init {
    seedInitialData()
    startOfferCountdownTicker()
  }

  fun switchRole(newRole: UserRole) {
    currentRole = newRole
    currentUser = when (newRole) {
      UserRole.CUSTOMER -> customerUser
      UserRole.WORKER -> workerUser
      UserRole.ADMIN -> adminUser
    }
    pushEvent("HommieState: Switched active view to ${newRole.name}")
  }

  private fun startOfferCountdownTicker() {
    scope.launch {
      while (true) {
        delay(1000)
        if (activeOfferBookingId != null && offerRemainingSeconds > 0) {
          offerRemainingSeconds--
          if (offerRemainingSeconds == 0) {
            // Expire offer safely
            val booking = bookings.find { it.id == activeOfferBookingId }
            if (booking != null && booking.status == BookingStatus.OFFERED) {
              updateBookingStatus(booking.id, BookingStatus.REQUESTED)
              pushEvent("offer.expired: Offer for #${booking.id} expired after 3 minutes")
              addNotification(
                title = "Job Offer Expired",
                body = "Offer for ${booking.serviceName} timed out without acceptance.",
                type = "OFFER_EXPIRED",
                bookingId = booking.id
              )
            }
            activeOfferBookingId = null
          }
        }
      }
    }
  }

  fun pushEvent(eventDesc: String) {
    lastEventLog = eventDesc
  }

  fun addNotification(title: String, body: String, type: String, bookingId: String? = null) {
    val dateStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    notifications.add(
      0,
      NotificationItem(
        id = UUID.randomUUID().toString(),
        title = title,
        body = body,
        timestamp = dateStr,
        isRead = false,
        type = type,
        targetBookingId = bookingId
      )
    )
  }

  fun markAllNotificationsRead() {
    val updated = notifications.map { it.copy(isRead = true) }
    notifications.clear()
    notifications.addAll(updated)
  }

  val unreadNotificationsCount: Int
    get() = notifications.count { !it.isRead }

  // --- Strict Booking State Machine ---
  // DRAFT -> REQUESTED -> PAYMENT_PENDING -> OFFERED -> ACCEPTED -> EN_ROUTE -> IN_PROGRESS -> COMPLETED -> CONFIRMED -> FUNDS_RELEASED
  // Cancel: REQUESTED, PAYMENT_PENDING, OFFERED, ACCEPTED -> CANCELLED
  // Dispute: COMPLETED, CONFIRMED, FUNDS_RELEASED -> DISPUTED
  // Dispute Resolution: DISPUTED -> RESOLVED_REFUND, RESOLVED_RELEASE, RESOLVED_PARTIAL_REFUND

  fun createBooking(
    workerId: String,
    serviceId: String,
    scheduledDate: String,
    scheduledTime: String,
    address: String,
    notes: String,
    hours: Double = 2.0
  ): Booking {
    val worker = workers.find { it.id == workerId } ?: workers.first()
    val service = worker.services.find { it.serviceId == serviceId }
      ?: WorkerServiceItem(serviceId, "Custom Service", worker.baseHourlyRate)

    val pricing = PriceBreakdown.calculate(service.hourlyRate, hours)
    val newId = "bk_" + UUID.randomUUID().toString().take(8)
    val now = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())

    // Direct upfront escrow authorization moves directly to OFFERED
    val booking = Booking(
      id = newId,
      customerId = currentUser.id,
      customerName = currentUser.name,
      workerId = worker.id,
      workerName = worker.name,
      serviceId = service.serviceId,
      serviceName = service.serviceName,
      scheduledDate = scheduledDate,
      scheduledTime = scheduledTime,
      address = address,
      notes = notes,
      pricing = pricing,
      status = BookingStatus.OFFERED,
      createdAt = now,
      offerExpiresAtMs = System.currentTimeMillis() + (180 * 1000)
    )

    bookings.add(0, booking)
    activeOfferBookingId = newId
    offerRemainingSeconds = 180

    pushEvent("offer.created: Dispatched to ${worker.name}. Held: $${pricing.totalHeldAmount}")
    addNotification(
      title = "Job Offer Dispatched",
      body = "Authorized payment of $${pricing.totalHeldAmount} held in escrow. 3m offer dispatched to ${worker.name}.",
      type = "OFFER_CREATED",
      bookingId = newId
    )

    // Add Escrow Held ledger entry for worker
    ledgerEntries.add(
      0,
      LedgerEntry(
        id = "led_" + UUID.randomUUID().toString().take(6),
        bookingId = newId,
        type = LedgerType.EARNING_HELD,
        amount = pricing.baseAmount,
        description = "Pending held escrow for ${service.serviceName} (#$newId)",
        date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date()),
        isCredit = true
      )
    )

    return booking
  }

  fun acceptOffer(bookingId: String): Boolean {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index == -1) return false
    val b = bookings[index]
    // Race-safe check
    if (b.status != BookingStatus.OFFERED) {
      pushEvent("offer.accepted error: Offer #$bookingId is already ${b.status.name}")
      return false
    }

    val updated = b.copy(status = BookingStatus.ACCEPTED)
    bookings[index] = updated
    if (activeOfferBookingId == bookingId) {
      activeOfferBookingId = null
    }

    pushEvent("offer.accepted: Pro accepted booking #$bookingId")
    addNotification(
      title = "Offer Accepted!",
      body = "${b.workerName} accepted the job for ${b.serviceName}.",
      type = "OFFER_ACCEPTED",
      bookingId = bookingId
    )
    return true
  }

  fun declineOffer(bookingId: String) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      if (b.status == BookingStatus.OFFERED) {
        bookings[index] = b.copy(status = BookingStatus.REQUESTED)
        if (activeOfferBookingId == bookingId) {
          activeOfferBookingId = null
        }
        pushEvent("offer.declined: Pro declined booking #$bookingId")
        addNotification(
          title = "Offer Declined",
          body = "Worker declined offer. Finding another available pro.",
          type = "OFFER_DECLINED",
          bookingId = bookingId
        )
      }
    }
  }

  fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      bookings[index] = b.copy(status = newStatus)
      pushEvent("booking.status_changed: #$bookingId -> ${newStatus.name}")
      addNotification(
        title = "Booking Status Update",
        body = "Job #${bookingId.takeLast(5)} moved to ${newStatus.displayName}.",
        type = "STATUS_CHANGED",
        bookingId = bookingId
      )

      // When confirmed by customer, automatically release funds
      if (newStatus == BookingStatus.CONFIRMED) {
        releaseFundsForBooking(bookingId)
      }
    }
  }

  fun cancelBooking(bookingId: String, reason: String = "Customer request") {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      val allowedToCancel = listOf(
        BookingStatus.REQUESTED,
        BookingStatus.PAYMENT_PENDING,
        BookingStatus.OFFERED,
        BookingStatus.ACCEPTED
      )
      if (allowedToCancel.contains(b.status)) {
        bookings[index] = b.copy(status = BookingStatus.CANCELLED)
        if (activeOfferBookingId == bookingId) {
          activeOfferBookingId = null
        }
        pushEvent("booking.status_changed: #$bookingId CANCELLED. Authorized funds released.")
        addNotification(
          title = "Booking Cancelled",
          body = "Job #$bookingId cancelled. Authorization voided with zero penalty.",
          type = "BOOKING_CANCELLED",
          bookingId = bookingId
        )
      }
    }
  }

  private fun releaseFundsForBooking(bookingId: String) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      bookings[index] = b.copy(status = BookingStatus.FUNDS_RELEASED)
      pushEvent("booking.status_changed: #$bookingId FUNDS_RELEASED")
      addNotification(
        title = "Funds Released to Pro",
        body = "$${b.pricing.baseAmount} captured and deposited into ${b.workerName}'s available earnings.",
        type = "FUNDS_RELEASED",
        bookingId = bookingId
      )

      // Add to available earnings
      ledgerEntries.add(
        0,
        LedgerEntry(
          id = "led_" + UUID.randomUUID().toString().take(6),
          bookingId = bookingId,
          type = LedgerType.EARNING_AVAILABLE,
          amount = b.pricing.baseAmount,
          description = "Earned payout for ${b.serviceName} (#$bookingId)",
          date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date()),
          isCredit = true
        )
      )
    }
  }

  fun submitReview(bookingId: String, rating: Int, comment: String) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      val now = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
      val rev = BookingReview(rating, comment, now)
      bookings[index] = b.copy(review = rev)

      // Also append to worker's public profile reviews
      val wIndex = workers.indexOfFirst { it.id == b.workerId }
      if (wIndex != -1) {
        val w = workers[wIndex]
        val newReviews = w.reviews.toMutableList()
        newReviews.add(
          0,
          WorkerReview(
            id = UUID.randomUUID().toString(),
            customerName = b.customerName,
            rating = rating,
            date = now,
            comment = comment,
            serviceName = b.serviceName
          )
        )
        val newAvg = newReviews.map { it.rating }.average()
        workers[wIndex] = w.copy(
          reviews = newReviews,
          reviewCount = newReviews.size,
          rating = Math.round(newAvg * 10.0) / 10.0
        )
      }

      pushEvent("Review submitted: $rating stars for #${bookingId.takeLast(4)}")
      addNotification(
        title = "Review Submitted",
        body = "Thank you! Your feedback for ${b.workerName} has been recorded.",
        type = "REVIEW_SUBMITTED",
        bookingId = bookingId
      )
    }
  }

  fun raiseDispute(bookingId: String, reason: DisputeReason, explanation: String) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      val now = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
      val initialMessage = DisputeMessage(
        id = UUID.randomUUID().toString(),
        senderId = currentUser.id,
        senderName = currentUser.name,
        senderRole = currentUser.role,
        message = explanation,
        timestamp = now
      )
      val dispute = Dispute(
        id = "dsp_" + UUID.randomUUID().toString().take(6),
        bookingId = bookingId,
        reason = reason,
        description = explanation,
        status = DisputeStatus.OPEN,
        messages = listOf(initialMessage),
        createdAt = now
      )
      bookings[index] = b.copy(status = BookingStatus.DISPUTED, dispute = dispute)
      pushEvent("dispute.updated: Dispute opened for #$bookingId (${reason.label})")
      addNotification(
        title = "Dispute Case Opened",
        body = "Dispute #${dispute.id} is under review. Escrow funds remain safely frozen.",
        type = "DISPUTE_OPENED",
        bookingId = bookingId
      )
    }
  }

  fun sendDisputeMessage(bookingId: String, messageText: String) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      val currentDispute = b.dispute ?: return
      val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
      val newMsg = DisputeMessage(
        id = UUID.randomUUID().toString(),
        senderId = currentUser.id,
        senderName = currentUser.name,
        senderRole = currentUser.role,
        message = messageText,
        timestamp = now
      )
      val updatedMsgs = currentDispute.messages + newMsg
      bookings[index] = b.copy(dispute = currentDispute.copy(messages = updatedMsgs))
      pushEvent("dispute.updated: New message added to dispute for #$bookingId")
    }
  }

  fun resolveDispute(bookingId: String, resolution: DisputeStatus, note: String) {
    val index = bookings.indexOfFirst { it.id == bookingId }
    if (index != -1) {
      val b = bookings[index]
      val currentDispute = b.dispute ?: return

      val newBookingStatus = when (resolution) {
        DisputeStatus.RESOLVED_REFUND -> BookingStatus.RESOLVED_REFUND
        DisputeStatus.RESOLVED_RELEASE -> BookingStatus.RESOLVED_RELEASE
        DisputeStatus.RESOLVED_PARTIAL_REFUND -> BookingStatus.RESOLVED_PARTIAL_REFUND
        else -> b.status
      }

      bookings[index] = b.copy(
        status = newBookingStatus,
        dispute = currentDispute.copy(
          status = resolution,
          resolutionNote = note
        )
      )

      pushEvent("dispute.updated: Dispute #$bookingId resolved as ${resolution.label}")
      addNotification(
        title = "Dispute Resolved",
        body = "Resolution: ${resolution.label}. $note",
        type = "DISPUTE_RESOLVED",
        bookingId = bookingId
      )
    }
  }

  // --- Worker Service Catalog Management ---
  fun addServiceToWorker(workerId: String, serviceId: String, hourlyRate: Double) {
    val index = workers.indexOfFirst { it.id == workerId }
    if (index != -1) {
      val w = workers[index]
      val cat = catalogCategories.find { it.id == serviceId }
      val serviceName = cat?.name ?: "Service"
      val existing = w.services.toMutableList()
      existing.removeAll { it.serviceId == serviceId }
      existing.add(WorkerServiceItem(serviceId, serviceName, hourlyRate, true))
      workers[index] = w.copy(services = existing)
      pushEvent("Service catalog updated: Added $serviceName at $$hourlyRate/hr")
    }
  }

  fun toggleWorkerService(workerId: String, serviceId: String) {
    val index = workers.indexOfFirst { it.id == workerId }
    if (index != -1) {
      val w = workers[index]
      val updated = w.services.map {
        if (it.serviceId == serviceId) it.copy(isActive = !it.isActive) else it
      }
      workers[index] = w.copy(services = updated)
    }
  }

  fun removeWorkerService(workerId: String, serviceId: String) {
    val index = workers.indexOfFirst { it.id == workerId }
    if (index != -1) {
      val w = workers[index]
      val updated = w.services.filterNot { it.serviceId == serviceId }
      workers[index] = w.copy(services = updated)
    }
  }

  // --- Payout Request ---
  fun requestPayout(amount: Double, bankName: String, accountLast4: String): Boolean {
    if (amount <= 0 || amount > availableEarnings) return false
    val now = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
    payoutRecords.add(
      0,
      PayoutRecord(
        id = "po_" + UUID.randomUUID().toString().take(6),
        amount = amount,
        bankName = bankName,
        accountLast4 = accountLast4,
        status = "PROCESSING",
        requestedAt = now
      )
    )
    ledgerEntries.add(
      0,
      LedgerEntry(
        id = "led_" + UUID.randomUUID().toString().take(6),
        bookingId = null,
        type = LedgerType.PAYOUT_DEBIT,
        amount = amount,
        description = "Withdrawal to $bankName (••$accountLast4)",
        date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date()),
        isCredit = false
      )
    )
    pushEvent("Payout request: $${amount} submitted to $bankName")
    addNotification(
      title = "Payout Initiated",
      body = "Transfer of $${amount} to $bankName (••$accountLast4) is on its way (1-2 business days).",
      type = "PAYOUT_INITIATED"
    )
    return true
  }

  // Worker Financial Stats
  val availableEarnings: Double
    get() {
      val credits = ledgerEntries.filter { it.type == LedgerType.EARNING_AVAILABLE && it.isCredit }.sumOf { it.amount }
      val debits = ledgerEntries.filter { !it.isCredit }.sumOf { it.amount }
      return Math.max(0.0, Math.round((credits - debits) * 100.0) / 100.0)
    }

  val heldFunds: Double
    get() {
      val activeHeldBookings = bookings.filter {
        it.workerId == workerUser.id &&
        listOf(BookingStatus.OFFERED, BookingStatus.ACCEPTED, BookingStatus.EN_ROUTE, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.DISPUTED).contains(it.status)
      }
      val total = activeHeldBookings.sumOf { it.pricing.baseAmount }
      return Math.round(total * 100.0) / 100.0
    }

  val lifetimeEarnings: Double
    get() {
      val total = ledgerEntries.filter { it.type == LedgerType.EARNING_AVAILABLE }.sumOf { it.amount }
      return Math.round((total + 4250.0) * 100.0) / 100.0
    }

  // --- Admin User Actions ---
  fun toggleUserSuspension(userId: String) {
    val index = allUsers.indexOfFirst { it.id == userId }
    if (index != -1) {
      val u = allUsers[index]
      val newStatus = if (u.status == UserStatus.ACTIVE) UserStatus.SUSPENDED else UserStatus.ACTIVE
      allUsers[index] = u.copy(status = newStatus)
      pushEvent("Admin: User ${u.name} set to ${newStatus.name}")
    }
  }

  // --- Admin Verification Actions ---
  fun approveWorkerVerification(verificationId: String) {
    val index = verificationQueue.indexOfFirst { it.id == verificationId }
    if (index != -1) {
      val v = verificationQueue[index]
      verificationQueue[index] = v.copy(status = "APPROVED")
      // Update worker profile
      val wIndex = workers.indexOfFirst { it.id == v.workerId }
      if (wIndex != -1) {
        workers[wIndex] = workers[wIndex].copy(isVerified = true)
      }
      pushEvent("Admin: Approved Pro verification for ${v.workerName}")
    }
  }

  fun rejectWorkerVerification(verificationId: String) {
    val index = verificationQueue.indexOfFirst { it.id == verificationId }
    if (index != -1) {
      val v = verificationQueue[index]
      verificationQueue[index] = v.copy(status = "REJECTED")
      pushEvent("Admin: Rejected verification request #${v.id}")
    }
  }

  // --- Address Management ---
  fun addSavedAddress(label: String, street: String, city: String, zip: String) {
    savedAddresses.add(
      SavedAddress(
        id = UUID.randomUUID().toString(),
        label = label,
        street = street,
        city = city,
        zip = zip,
        isDefault = savedAddresses.isEmpty()
      )
    )
  }

  // Seed Data
  private fun seedInitialData() {
    // Seed Users
    allUsers.addAll(
      listOf(
        customerUser,
        workerUser,
        adminUser,
        User("usr_elena_02", "Elena Rostova", "elena.r@example.com", UserRole.CUSTOMER, "+1 (555) 345-6789", avatarInitials = "ER"),
        User("usr_david_03", "David Kim", "d.kim@example.com", UserRole.CUSTOMER, "+1 (555) 456-7890", avatarInitials = "DK"),
        User("wrk_carlos_02", "Carlos Mendez", "carlos.m@example.com", UserRole.WORKER, "+1 (555) 567-8901", isVerified = true, avatarInitials = "CM"),
        User("wrk_jordan_03", "Jordan Lee", "jordan.l@example.com", UserRole.WORKER, "+1 (555) 678-9012", isVerified = false, avatarInitials = "JL")
      )
    )

    // Seed Pro Workers
    val marcusReviews = listOf(
      WorkerReview("rev_1", "Elena R.", 5, "Sep 12, 2026", "Marcus diagnosed our leaking copper pipe in 10 minutes and had all parts in his van. Pristine clean-up!", "Plumbing"),
      WorkerReview("rev_2", "David K.", 5, "Sep 4, 2026", "Installed smart EV charger and main panel breaker. Highly professional and courteous.", "Electrical"),
      WorkerReview("rev_3", "Rachel T.", 4, "Aug 28, 2026", "Great work on our bathroom fixture installation. Arrived on time.", "Handyman")
    )

    val marcusServices = listOf(
      WorkerServiceItem("cat_plumbing", "Plumbing Repairs & Fixtures", 95.0, true),
      WorkerServiceItem("cat_electrical", "Electrical & Smart Home", 110.0, true),
      WorkerServiceItem("cat_handyman", "Handyman & Carpentry", 75.0, true)
    )

    workers.add(
      WorkerProfile(
        id = "wrk_marcus_01",
        userId = "wrk_marcus_01",
        name = "Marcus Vance",
        bio = "Licensed Master Plumber & Certified Electrician with 12+ years serving the metro community. Fully insured, background-checked, and committed to transparent craftsmanship.",
        skills = listOf("Pipe Welding", "Leak Detection", "EV Chargers", "Water Heaters", "Drywall Finish"),
        baseHourlyRate = 95.0,
        isVerified = true,
        rating = 4.9,
        reviewCount = 128,
        availability = WorkerAvailability.ONLINE,
        services = marcusServices,
        reviews = marcusReviews,
        completedJobsCount = 142
      )
    )

    workers.add(
      WorkerProfile(
        id = "wrk_carlos_02",
        userId = "wrk_carlos_02",
        name = "Carlos Mendez",
        bio = "Expert Residential Electrician specializing in emergency power troubleshooting, panel swaps, and modern architectural lighting systems.",
        skills = listOf("Panel Upgrades", "Ceiling Fans", "Recessed Lights", "Code Compliance"),
        baseHourlyRate = 105.0,
        isVerified = true,
        rating = 4.8,
        reviewCount = 89,
        availability = WorkerAvailability.ONLINE,
        services = listOf(
          WorkerServiceItem("cat_electrical", "Electrical Systems", 105.0, true),
          WorkerServiceItem("cat_appliance", "Appliance Hookups", 85.0, true)
        ),
        reviews = listOf(
          WorkerReview("rev_4", "Michael B.", 5, "Aug 15, 2026", "Carlos was super efficient installing 8 recessed lights.", "Electrical")
        ),
        completedJobsCount = 98
      )
    )

    workers.add(
      WorkerProfile(
        id = "wrk_jordan_03",
        userId = "wrk_jordan_03",
        name = "Jordan Lee",
        bio = "Eco-friendly deep cleaning & restoration specialist. Uses hospital-grade plant-based sanitizers safe for children and pets.",
        skills = listOf("Deep Clean", "Steam Sanitization", "Window Detailing", "Move-Out Cleans"),
        baseHourlyRate = 60.0,
        isVerified = false,
        rating = 4.7,
        reviewCount = 43,
        availability = WorkerAvailability.BUSY,
        services = listOf(
          WorkerServiceItem("cat_cleaning", "Deep Home Cleaning", 60.0, true)
        ),
        reviews = listOf(
          WorkerReview("rev_5", "Sophia L.", 5, "Jul 22, 2026", "Left our apartment spotless for our move-out inspection.", "Deep Cleaning")
        ),
        completedJobsCount = 52
      )
    )

    // Seed Saved Addresses
    savedAddresses.addAll(
      listOf(
        SavedAddress("addr_1", "Home", "742 Evergreen Terrace", "Metro Springs", "97477", isDefault = true),
        SavedAddress("addr_2", "Studio Office", "450 Broadway Ave, Suite 3B", "Metro Springs", "97401", isDefault = false),
        SavedAddress("addr_3", "Rental Property", "1820 Oak Crest Dr", "Metro Springs", "97405", isDefault = false)
      )
    )

    // Seed Bookings
    // 1. Live Offer Booking with 3 min countdown
    val livePricing = PriceBreakdown.calculate(95.0, 2.0)
    bookings.add(
      Booking(
        id = "bk_live_offer_01",
        customerId = "usr_alex_01",
        customerName = "Alex Rivera",
        workerId = "wrk_marcus_01",
        workerName = "Marcus Vance",
        serviceId = "cat_plumbing",
        serviceName = "Plumbing Repairs & Fixtures",
        scheduledDate = "Today",
        scheduledTime = "3:30 PM",
        address = "742 Evergreen Terrace, Metro Springs",
        notes = "Urgent kitchen sink trap is leaking into the lower cabinet. Valve is sticky.",
        pricing = livePricing,
        status = BookingStatus.OFFERED,
        createdAt = "Just now",
        offerExpiresAtMs = System.currentTimeMillis() + (180 * 1000)
      )
    )

    // 2. In Progress Booking
    val inProgressPricing = PriceBreakdown.calculate(110.0, 2.5)
    bookings.add(
      Booking(
        id = "bk_active_02",
        customerId = "usr_elena_02",
        customerName = "Elena Rostova",
        workerId = "wrk_marcus_01",
        workerName = "Marcus Vance",
        serviceId = "cat_electrical",
        serviceName = "Electrical & Smart Home",
        scheduledDate = "Today",
        scheduledTime = "1:00 PM",
        address = "124 Pine Blvd, Apt 4C",
        notes = "Install smart thermostat and replace 3 three-way light switches.",
        pricing = inProgressPricing,
        status = BookingStatus.IN_PROGRESS,
        createdAt = "Today, 12:45 PM"
      )
    )

    // 3. Completed Booking awaiting customer confirmation
    val completedPricing = PriceBreakdown.calculate(75.0, 3.0)
    bookings.add(
      Booking(
        id = "bk_completed_03",
        customerId = "usr_alex_01",
        customerName = "Alex Rivera",
        workerId = "wrk_marcus_01",
        workerName = "Marcus Vance",
        serviceId = "cat_handyman",
        serviceName = "Handyman & Carpentry",
        scheduledDate = "Yesterday",
        scheduledTime = "10:00 AM",
        address = "742 Evergreen Terrace, Metro Springs",
        notes = "Custom shelving mount in study room and realignment of entry door latch.",
        pricing = completedPricing,
        status = BookingStatus.COMPLETED,
        createdAt = "Yesterday, 9:55 AM"
      )
    )

    // 4. Disputed Booking for testing Dispute Resolution Console
    val disputedPricing = PriceBreakdown.calculate(60.0, 4.0)
    val disputeMessage = DisputeMessage(
      id = "msg_dsp_1",
      senderId = "usr_david_03",
      senderName = "David Kim",
      senderRole = UserRole.CUSTOMER,
      message = "Cleaner left after only 2 hours and skipped the inside of the oven and refrigerator which were booked.",
      timestamp = "Sep 15, 2:10 PM"
    )
    val disputeReply = DisputeMessage(
      id = "msg_dsp_2",
      senderId = "wrk_jordan_03",
      senderName = "Jordan Lee",
      senderRole = UserRole.WORKER,
      message = "The water in the condo was shut off for municipal repairs during the last 90 minutes so I couldn't rinse the appliances.",
      timestamp = "Sep 15, 2:45 PM"
    )
    bookings.add(
      Booking(
        id = "bk_dispute_04",
        customerId = "usr_david_03",
        customerName = "David Kim",
        workerId = "wrk_jordan_03",
        workerName = "Jordan Lee",
        serviceId = "cat_cleaning",
        serviceName = "Deep Home Cleaning",
        scheduledDate = "Sep 15, 2026",
        scheduledTime = "11:00 AM",
        address = "55 Skyline Way, #1202",
        notes = "Full deep move-in clean including kitchen appliances.",
        pricing = disputedPricing,
        status = BookingStatus.DISPUTED,
        createdAt = "Sep 15, 2026",
        dispute = Dispute(
          id = "dsp_88190",
          bookingId = "bk_dispute_04",
          reason = DisputeReason.WORK_INCOMPLETE,
          description = "Cleaner left early due to water shut-off; kitchen appliances skipped.",
          status = DisputeStatus.OPEN,
          messages = listOf(disputeMessage, disputeReply),
          createdAt = "Sep 15, 2026 2:10 PM"
        )
      )
    )

    // 5. Confirmed & Released Past Booking with Review
    val confirmedPricing = PriceBreakdown.calculate(95.0, 2.0)
    bookings.add(
      Booking(
        id = "bk_past_05",
        customerId = "usr_alex_01",
        customerName = "Alex Rivera",
        workerId = "wrk_marcus_01",
        workerName = "Marcus Vance",
        serviceId = "cat_plumbing",
        serviceName = "Plumbing Repairs & Fixtures",
        scheduledDate = "Sep 10, 2026",
        scheduledTime = "2:00 PM",
        address = "742 Evergreen Terrace, Metro Springs",
        notes = "Replaced main bathroom shutoff valve.",
        pricing = confirmedPricing,
        status = BookingStatus.FUNDS_RELEASED,
        createdAt = "Sep 10, 2026",
        review = BookingReview(5, "Marcus is exceptional! Clean, fast, and polite.", "Sep 10, 2026")
      )
    )

    // Seed Ledger Entries for Worker
    ledgerEntries.addAll(
      listOf(
        LedgerEntry("led_01", "bk_past_05", LedgerType.EARNING_AVAILABLE, 190.0, "Completed job: Plumbing Repairs (#bk_past_05)", "Sep 10", true),
        LedgerEntry("led_02", "bk_active_02", LedgerType.EARNING_HELD, 275.0, "Held in Escrow: Electrical & Smart Home (#bk_active_02)", "Today", true),
        LedgerEntry("led_03", "bk_completed_03", LedgerType.EARNING_HELD, 225.0, "Held in Escrow: Handyman & Carpentry (#bk_completed_03)", "Yesterday", true),
        LedgerEntry("led_04", null, LedgerType.PAYOUT_DEBIT, 450.0, "Direct Deposit to Chase Bank (••4821)", "Sep 8", false)
      )
    )

    // Seed Payout Records
    payoutRecords.add(
      PayoutRecord("po_7710", 450.0, "JPMorgan Chase", "4821", "COMPLETED", "Sep 8, 2026")
    )

    // Seed Admin Verification Queue
    verificationQueue.addAll(
      listOf(
        WorkerVerification(
          id = "ver_01",
          workerId = "wrk_jordan_03",
          workerName = "Jordan Lee",
          tradeType = "Deep Cleaning & Sanitization",
          licenseNumber = "EPA-CERT-884910",
          idDocument = "State Driver License (Verified Real ID)",
          backgroundCheckStatus = "PASSED (Clear Background)",
          submittedAt = "Sep 16, 2026",
          status = "PENDING"
        ),
        WorkerVerification(
          id = "ver_02",
          workerId = "wrk_marcus_01",
          workerName = "Marcus Vance",
          tradeType = "Master Plumbing & Electrical",
          licenseNumber = "MP-2023-99120",
          idDocument = "State Contractor License Card",
          backgroundCheckStatus = "PASSED (Clear Background)",
          submittedAt = "Aug 1, 2026",
          status = "APPROVED"
        )
      )
    )

    // Seed Notifications
    notifications.addAll(
      listOf(
        NotificationItem("notif_1", "Incoming Job Offer", "New Plumbing repair request near Evergreen Terrace ($190.00)", "Just now", false, "OFFER_CREATED", "bk_live_offer_01"),
        NotificationItem("notif_2", "Job In Progress", "Marcus marked Electrical & Smart Home as Started.", "35m ago", false, "STATUS_CHANGED", "bk_active_02"),
        NotificationItem("notif_3", "Awaiting Completion Approval", "Marcus finished Handyman & Carpentry. Tap to review and release funds.", "Yesterday", true, "STATUS_CHANGED", "bk_completed_03")
      )
    )
  }
}
