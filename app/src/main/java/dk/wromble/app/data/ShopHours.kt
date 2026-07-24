package dk.wromble.app.data

import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ShopOpenState(val isOpen: Boolean, val reopenText: String? = null)

// Danish weekday names as returned by the API (Søndag..Lørdag)
private val DANISH_WEEKDAYS = listOf(
    "Søndag", "Mandag", "Tirsdag", "Onsdag", "Torsdag", "Fredag", "Lørdag"
)

private fun toMinutes(hhmm: String?): Int? {
    if (hhmm.isNullOrBlank()) return null
    val parts = hhmm.trim().split(":")
    if (parts.size < 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}

// Port of iOS wrombleShopOpenState(): opening hours (incl. past-midnight) + manual "Lukket".
fun wrombleShopOpenState(
    days: List<CompanyHourDay>,
    shopStatus: String?,
    useDelivery: Boolean = false
): ShopOpenState {
    if (!shopStatus.isNullOrBlank()) {
        val s = shopStatus.trim().lowercase()
        if (s == "lukket" || s == "closed") return ShopOpenState(false, "Midlertidigt lukket")
    }
    if (days.isEmpty()) return ShopOpenState(true) // no hours known -> assume open

    val cal = Calendar.getInstance()
    val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val todayName = DANISH_WEEKDAYS.getOrNull(dow) ?: ""

    fun dayFor(name: String): CompanyHourDay? =
        days.firstOrNull { it.weekday.equals(name, ignoreCase = true) }

    val today = dayFor(todayName)
    val open = toMinutes(if (useDelivery) today?.bringOpen else today?.storeOpen)
    val close = toMinutes(if (useDelivery) today?.bringClose else today?.storeClose)

    // Check yesterday's past-midnight window that may still cover "now"
    val yName = DANISH_WEEKDAYS.getOrNull((dow + 6) % 7) ?: ""
    val yDay = dayFor(yName)
    val yOpen = toMinutes(if (useDelivery) yDay?.bringOpen else yDay?.storeOpen)
    val yClose = toMinutes(if (useDelivery) yDay?.bringClose else yDay?.storeClose)
    if (yOpen != null && yClose != null && yClose <= yOpen && nowMin < yClose) {
        return ShopOpenState(true)
    }

    if (open == null || close == null || (open == 0 && close == 0)) {
        return ShopOpenState(false, nextOpen(days, dow, useDelivery))
    }

    val isOpen = if (close > open) {
        nowMin in open until close
    } else {
        // past-midnight: open late today through early tomorrow
        nowMin >= open || nowMin < close
    }
    return if (isOpen) ShopOpenState(true)
    else ShopOpenState(false, nextOpen(days, dow, useDelivery))
}

private fun fmt(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

private fun nextOpen(days: List<CompanyHourDay>, fromDow: Int, useDelivery: Boolean): String? {
    for (offset in 0..7) {
        val d = (fromDow + offset) % 7
        val name = DANISH_WEEKDAYS.getOrNull(d) ?: continue
        val day = days.firstOrNull { it.weekday.equals(name, ignoreCase = true) } ?: continue
        val open = toMinutes(if (useDelivery) day.bringOpen else day.storeOpen) ?: continue
        val close = toMinutes(if (useDelivery) day.bringClose else day.storeClose) ?: continue
        if (open == 0 && close == 0) continue
        return when (offset) {
            0 -> "Åbner kl. ${fmt(open)}"
            1 -> "Åbner i morgen kl. ${fmt(open)}"
            else -> "Åbner $name kl. ${fmt(open)}"
        }
    }
    return null
}

// Haversine distance in km between two coordinates
fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    if (lat1 == 0.0 && lng1 == 0.0) return Double.MAX_VALUE
    if (lat2 == 0.0 && lng2 == 0.0) return Double.MAX_VALUE
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun distanceLabel(km: Double): String = when {
    km == Double.MAX_VALUE -> ""
    km < 1.0 -> "${(km * 1000).toInt()} m"
    else -> "%.1f km".format(km)
}
