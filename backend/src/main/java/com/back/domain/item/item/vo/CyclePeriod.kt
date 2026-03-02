package com.back.domain.item.item.vo

import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import java.time.LocalDate

data class CyclePeriod(
    val amount: Int,
    val unit: Unit
) {

    fun addTo(startDate: LocalDate?): LocalDate {
        if (startDate == null) {
            throw ServiceException(ErrorCode.CYCLE_PERIOD_START_DATE_REQUIRED)
        }

        return when (unit) {
            Unit.DAY -> startDate.plusDays(amount.toLong())
            Unit.MONTH -> startDate.plusMonths(amount.toLong())
            Unit.YEAR -> startDate.plusYears(amount.toLong())
        }
    }

    enum class Unit(val code: Char) {
        DAY('d'),
        MONTH('m'),
        YEAR('y');

        companion object {
            fun from(code: Char): Unit {
                return when (code) {
                    'd' -> DAY
                    'm' -> MONTH
                    'y' -> YEAR
                    else -> throw ServiceException(ErrorCode.UNSUPPORTED_CYCLE_PERIOD_UNIT)
                }
            }
        }
    }

    companion object {
        private val PATTERN = Regex("^(\\d+)([dmy])$")

        fun from(raw: String?): CyclePeriod {
            if (raw.isNullOrBlank()) {
                throw ServiceException(ErrorCode.CYCLE_DAYS_REQUIRED)
            }

            val matchResult = PATTERN.matchEntire(raw.trim().lowercase())
                ?: throw ServiceException(ErrorCode.INVALID_CYCLE_DAYS_FORMAT)

            val amount = matchResult.groupValues[1].toInt()
            val unit = Unit.from(matchResult.groupValues[2][0])

            if (amount <= 0) {
                throw ServiceException(ErrorCode.CYCLE_DAYS_MUST_BE_POSITIVE)
            }

            return CyclePeriod(amount, unit)
        }
    }
}