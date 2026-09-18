package com.example.ui.components

import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HommieRepository
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun HommieCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  backgroundColor: Color = SurfaceWhite,
  borderColor: Color = SlateBorder,
  elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    elevation = elevation
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      content = content
    )
  }
}

enum class ButtonVariant {
  PRIMARY,
  SECONDARY,
  OUTLINE,
  DESTRUCTIVE,
  SUCCESS
}

@Composable
fun HommieButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: ButtonVariant = ButtonVariant.PRIMARY,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false
) {
  val containerColor = when (variant) {
    ButtonVariant.PRIMARY -> IndigoPrimary
    ButtonVariant.SECONDARY -> IndigoLight
    ButtonVariant.OUTLINE -> Color.Transparent
    ButtonVariant.DESTRUCTIVE -> RoseDestructive
    ButtonVariant.SUCCESS -> EmeraldSuccess
  }

  val contentColor = when (variant) {
    ButtonVariant.PRIMARY -> Color.White
    ButtonVariant.SECONDARY -> IndigoDeep
    ButtonVariant.OUTLINE -> IndigoPrimary
    ButtonVariant.DESTRUCTIVE -> Color.White
    ButtonVariant.SUCCESS -> Color.White
  }

  val borderStroke = when (variant) {
    ButtonVariant.OUTLINE -> androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary)
    ButtonVariant.SECONDARY -> androidx.compose.foundation.BorderStroke(1.dp, IndigoSoft)
    else -> null
  }

  Button(
    onClick = onClick,
    modifier = modifier
      .defaultMinSize(minHeight = 48.dp),
    enabled = enabled && !isLoading,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = SlateSurface,
      disabledContentColor = SlateTextSubtle
    ),
    border = borderStroke,
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          color = contentColor,
          strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
      } else if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
      )
    }
  }
}

@Composable
fun HommieBadge(
  text: String,
  backgroundColor: Color = IndigoLight,
  textColor: Color = IndigoDeep,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = backgroundColor
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StatusBadge(status: BookingStatus) {
  val (bg, fg) = when (status) {
    BookingStatus.DRAFT -> SlateSurface to SlateTextMuted
    BookingStatus.REQUESTED -> BlueLight to BlueInfo
    BookingStatus.PAYMENT_PENDING -> AmberLight to AmberWarning
    BookingStatus.OFFERED -> IndigoLight to IndigoPrimary
    BookingStatus.ACCEPTED -> IndigoLight to IndigoDark
    BookingStatus.EN_ROUTE -> BlueLight to BlueInfo
    BookingStatus.IN_PROGRESS -> AmberLight to AmberWarning
    BookingStatus.COMPLETED -> EmeraldLight to EmeraldSuccess
    BookingStatus.CONFIRMED -> EmeraldLight to EmeraldSuccess
    BookingStatus.FUNDS_RELEASED -> EmeraldLight to EmeraldSuccess
    BookingStatus.CANCELLED -> SlateSurface to SlateTextMuted
    BookingStatus.DISPUTED -> RoseLight to RoseDestructive
    BookingStatus.RESOLVED_REFUND -> BlueLight to BlueInfo
    BookingStatus.RESOLVED_RELEASE -> EmeraldLight to EmeraldSuccess
    BookingStatus.RESOLVED_PARTIAL_REFUND -> AmberLight to AmberWarning
  }
  HommieBadge(text = status.displayName, backgroundColor = bg, textColor = fg)
}

@Composable
fun RatingStars(
  rating: Double,
  reviewCount: Int? = null,
  starSize: Int = 16,
  modifier: Modifier = Modifier,
  onRatingChanged: ((Int) -> Unit)? = null
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 1..5) {
      val isFilled = i <= rating.toInt()
      Icon(
        imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
        contentDescription = null,
        tint = if (isFilled) AmberWarning else SlateTextSubtle,
        modifier = Modifier
          .size(starSize.dp)
          .then(
            if (onRatingChanged != null) Modifier.clickable { onRatingChanged(i) } else Modifier
          )
      )
      if (i < 5) Spacer(modifier = Modifier.width(2.dp))
    }
    if (reviewCount != null) {
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "${String.format(Locale.getDefault(), "%.1f", rating)} ($reviewCount)",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SlateTextPrimary
      )
    }
  }
}

@Composable
fun EscrowSecurityBanner(
  modifier: Modifier = Modifier,
  amount: Double? = null
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = IndigoLight,
    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoSoft)
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .background(IndigoPrimary, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Security,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (amount != null) "Held in Escrow: $${String.format(Locale.getDefault(), "%.2f", amount)}" else "Escrow Protection Guarantee",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = IndigoDeep
        )
        Text(
          text = "Payment is securely authorized and held until the job is completed.",
          fontSize = 12.sp,
          color = IndigoDark,
          lineHeight = 16.sp
        )
      }
    }
  }
}

@Composable
fun BookingStatusTimeline(
  status: BookingStatus,
  modifier: Modifier = Modifier
) {
  val steps = listOf(
    BookingStatus.OFFERED to "Offer Sent",
    BookingStatus.ACCEPTED to "Accepted",
    BookingStatus.EN_ROUTE to "En Route",
    BookingStatus.IN_PROGRESS to "In Progress",
    BookingStatus.COMPLETED to "Completed",
    BookingStatus.FUNDS_RELEASED to "Confirmed"
  )

  // Map status to integer progression index
  val currentProgressIndex = when (status) {
    BookingStatus.DRAFT, BookingStatus.REQUESTED, BookingStatus.PAYMENT_PENDING -> 0
    BookingStatus.OFFERED -> 0
    BookingStatus.ACCEPTED -> 1
    BookingStatus.EN_ROUTE -> 2
    BookingStatus.IN_PROGRESS -> 3
    BookingStatus.COMPLETED -> 4
    BookingStatus.CONFIRMED, BookingStatus.FUNDS_RELEASED -> 5
    BookingStatus.CANCELLED -> -1
    BookingStatus.DISPUTED, BookingStatus.RESOLVED_REFUND, BookingStatus.RESOLVED_RELEASE, BookingStatus.RESOLVED_PARTIAL_REFUND -> -2
  }

  if (currentProgressIndex < 0) {
    // Show special banner for Cancelled or Disputed
    val (label, desc, bg, fg) = if (currentProgressIndex == -1) {
      Quadruple("Booking Cancelled", "This reservation was cancelled. Authorized funds have been voided.", SlateSurface, SlateTextMuted)
    } else {
      Quadruple("Booking Under Dispute", "Escrow funds are safely frozen while our resolution team reviews the claim.", RoseLight, RoseDestructive)
    }
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      color = bg,
      border = androidx.compose.foundation.BorderStroke(1.dp, fg.copy(alpha = 0.3f))
    ) {
      Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (currentProgressIndex == -1) Icons.Filled.Cancel else Icons.Filled.Gavel,
          contentDescription = null,
          tint = fg,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = label, fontWeight = FontWeight.Bold, color = fg, fontSize = 14.sp)
          Text(text = desc, color = SlateTextPrimary, fontSize = 12.sp)
        }
      }
    }
    return
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      steps.forEachIndexed { index, (stepStatus, stepLabel) ->
        val isCompleted = index < currentProgressIndex
        val isCurrent = index == currentProgressIndex
        val stepColor = when {
          isCompleted -> EmeraldSuccess
          isCurrent -> IndigoPrimary
          else -> SlateBorder
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .background(
                color = when {
                  isCompleted -> EmeraldSuccess
                  isCurrent -> IndigoPrimary
                  else -> SlateSurface
                },
                shape = CircleShape
              )
              .border(
                width = 2.dp,
                color = stepColor,
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isCompleted) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            } else {
              Text(
                text = "${index + 1}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Color.White else SlateTextMuted
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stepLabel,
            fontSize = 10.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrent) IndigoPrimary else if (isCompleted) SlateTextPrimary else SlateTextSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun HommieTopBar(
  onNotificationClick: () -> Unit,
  onRoleSwitchClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = SurfaceWhite,
    shadowElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Logo and Role Badge
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(IndigoPrimary, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "H",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color.White
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Hommie",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(
                  when (HommieRepository.currentRole) {
                    UserRole.CUSTOMER -> IndigoPrimary
                    UserRole.WORKER -> EmeraldSuccess
                    UserRole.ADMIN -> AmberWarning
                  },
                  CircleShape
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = HommieRepository.currentRole.name,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = SlateTextMuted
            )
          }
        }
      }

      // Actions: Switch Role + Notification Bell
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          modifier = Modifier.clickable { onRoleSwitchClick() },
          shape = RoundedCornerShape(20.dp),
          color = IndigoLight,
          border = androidx.compose.foundation.BorderStroke(1.dp, IndigoSoft)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Filled.SwapHoriz,
              contentDescription = null,
              tint = IndigoPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Switch",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = IndigoPrimary
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Notification Bell with Badge
        IconButton(onClick = onNotificationClick) {
          Box {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Notifications",
              tint = SlateTextPrimary
            )
            if (HommieRepository.unreadNotificationsCount > 0) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .size(16.dp)
                  .background(RoseDestructive, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${HommieRepository.unreadNotificationsCount}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }
        }
      }
    }
  }
}
