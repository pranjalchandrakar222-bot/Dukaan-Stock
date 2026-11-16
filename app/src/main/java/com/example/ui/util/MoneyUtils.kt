package com.example.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object MoneyUtils {
  fun round2(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return 0.0
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
  }

  fun toPaise(value: Double): Long {
    if (value.isNaN() || value.isInfinite()) return 0L
    return BigDecimal.valueOf(value)
      .multiply(BigDecimal.valueOf(100))
      .setScale(0, RoundingMode.HALF_UP)
      .longValueExact()
  }

  fun fromPaise(paise: Long): Double =
    BigDecimal.valueOf(paise).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toDouble()

  fun isZero(value: Double, tolerance: Double = 0.005): Boolean = abs(value) <= tolerance

  /**
   * Standard Indian financial year code (Apr -> Mar), e.g. "26-27".
   */
  fun financialYearTag(dateMillis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
    val year = cal.get(java.util.Calendar.YEAR)
    val month = cal.get(java.util.Calendar.MONTH) // 0 = Jan, 3 = Apr
    val startYear = if (month >= java.util.Calendar.APRIL) year else year - 1
    val endYear = startYear + 1
    return "${(startYear % 100).toString().padStart(2, '0')}-${(endYear % 100).toString().padStart(2, '0')}"
  }
}
