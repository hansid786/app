package com.example.model

enum class UserRole {
  CUSTOMER,
  WORKER,
  ADMIN
}

enum class UserStatus {
  ACTIVE,
  SUSPENDED
}

enum class WorkerAvailability {
  ONLINE,
  BUSY,
  OFFLINE
}

enum class BookingStatus(val displayName: String) {
  DRAFT("Draft"),
  REQUESTED("Requested"),
  PAYMENT_PENDING("Payment Pending"),
  OFFERED("Offered"),
  ACCEPTED("Accepted"),
  EN_ROUTE("En Route"),
  IN_PROGRESS("In Progress"),
  COMPLETED("Completed"),
  CONFIRMED("Confirmed"),
  FUNDS_RELEASED("Funds Released"),
  CANCELLED("Cancelled"),
  DISPUTED("Disputed"),
  RESOLVED_REFUND("Refunded"),
  RESOLVED_RELEASE("Released"),
  RESOLVED_PARTIAL_REFUND("Partial Refund")
}

enum class DisputeReason(val label: String) {
  WORK_INCOMPLETE("Work incomplete or abandoned"),
  POOR_QUALITY("Substandard service quality"),
  WORKER_NO_SHOW("Service provider did not arrive"),
  DAMAGE_INCIDENT("Property damage incurred"),
  BILLING_DISCREPANCY("Billing or scope discrepancy"),
  OTHER("Other service issue")
}

enum class DisputeStatus(val label: String) {
  OPEN("Open"),
  UNDER_REVIEW("Under Review"),
  RESOLVED_REFUND("Resolved - Full Refund"),
  RESOLVED_RELEASE("Resolved - Released to Pro"),
  RESOLVED_PARTIAL_REFUND("Resolved - 50/50 Split")
}

data class User(
  val id: String,
  val name: String,
  val email: String,
  val role: UserRole,
  val phone: String,
  val status: UserStatus = UserStatus.ACTIVE,
  val isVerified: Boolean = false,
  val avatarInitials: String = name.take(2).uppercase()
)

data class WorkerReview(
  val id: String,
  val customerName: String,
  val rating: Int,
  val date: String,
  val comment: String,
  val serviceName: String
)

data class WorkerServiceItem(
  val serviceId: String,
  val serviceName: String,
  val hourlyRate: Double,
  val isActive: Boolean = true
)

data class WorkerProfile(
  val id: String,
  val userId: String,
  val name: String,
  val bio: String,
  val skills: List<String>,
  val baseHourlyRate: Double,
  val isVerified: Boolean,
  val rating: Double,
  val reviewCount: Int,
  val availability: WorkerAvailability,
  val services: List<WorkerServiceItem>,
  val reviews: List<WorkerReview>,
  val completedJobsCount: Int = 34
)

data class PriceBreakdown(
  val hours: Double,
  val hourlyRate: Double,
  val baseAmount: Double,
  val platformFee: Double,    // 10%
  val tax: Double,            // 8.25%
  val totalHeldAmount: Double
) {
  companion object {
    fun calculate(hourlyRate: Double, hours: Double): PriceBreakdown {
      val base = hourlyRate * hours
      val fee = base * 0.10
      val taxVal = (base + fee) * 0.0825
      val total = base + fee + taxVal
      return PriceBreakdown(
        hours = hours,
        hourlyRate = hourlyRate,
        baseAmount = Math.round(base * 100.0) / 100.0,
        platformFee = Math.round(fee * 100.0) / 100.0,
        tax = Math.round(taxVal * 100.0) / 100.0,
        totalHeldAmount = Math.round(total * 100.0) / 100.0
      )
    }
  }
}

data class DisputeMessage(
  val id: String,
  val senderId: String,
  val senderName: String,
  val senderRole: UserRole,
  val message: String,
  val timestamp: String
)

data class Dispute(
  val id: String,
  val bookingId: String,
  val reason: DisputeReason,
  val description: String,
  val status: DisputeStatus = DisputeStatus.OPEN,
  val resolutionNote: String? = null,
  val messages: List<DisputeMessage> = emptyList(),
  val createdAt: String
)

data class BookingReview(
  val rating: Int,
  val comment: String,
  val submittedAt: String
)

data class Booking(
  val id: String,
  val customerId: String,
  val customerName: String,
  val workerId: String,
  val workerName: String,
  val serviceId: String,
  val serviceName: String,
  val scheduledDate: String,
  val scheduledTime: String,
  val address: String,
  val notes: String,
  val pricing: PriceBreakdown,
  val status: BookingStatus,
  val createdAt: String,
  val dispute: Dispute? = null,
  val review: BookingReview? = null,
  val offerExpiresAtMs: Long? = null // for 3-minute countdown when OFFERED
)

enum class LedgerType {
  EARNING_HELD,
  EARNING_AVAILABLE,
  PLATFORM_FEE,
  PAYOUT_DEBIT
}

data class LedgerEntry(
  val id: String,
  val bookingId: String?,
  val type: LedgerType,
  val amount: Double,
  val description: String,
  val date: String,
  val isCredit: Boolean
)

data class PayoutRecord(
  val id: String,
  val amount: Double,
  val bankName: String,
  val accountLast4: String,
  val status: String,
  val requestedAt: String
)

data class NotificationItem(
  val id: String,
  val title: String,
  val body: String,
  val timestamp: String,
  val isRead: Boolean = false,
  val type: String,
  val targetBookingId: String? = null
)

data class WorkerVerification(
  val id: String,
  val workerId: String,
  val workerName: String,
  val tradeType: String,
  val licenseNumber: String,
  val idDocument: String,
  val backgroundCheckStatus: String,
  val submittedAt: String,
  val status: String // "PENDING", "APPROVED", "REJECTED"
)

data class SavedAddress(
  val id: String,
  val label: String,
  val street: String,
  val city: String,
  val zip: String,
  val isDefault: Boolean = false
)

data class ServiceCatalogCategory(
  val id: String,
  val name: String,
  val iconName: String,
  val defaultHourlyRate: Double,
  val description: String
)
