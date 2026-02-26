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
            throw ServiceException(ErrorCode.INVALID_INPUT_VALUE, "startDate는 필수입니다.")
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
                    else -> throw ServiceException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 단위입니다: $code")
                }
            }
        }
    }

    companion object {
        private val PATTERN = Regex("^(\\d+)([dmy])$")

        fun from(raw: String?): CyclePeriod {
            if (raw.isNullOrBlank()) {
                throw ServiceException(ErrorCode.INVALID_INPUT_VALUE, "cycleDays는 필수입니다.")
            }

            val matchResult = PATTERN.matchEntire(raw.trim().lowercase())
                ?: throw ServiceException(ErrorCode.INVALID_INPUT_VALUE, "cycleDays 형식이 올바르지 않습니다. 예: 30d, 2m, 1y")

            val amount = matchResult.groupValues[1].toInt()
            val unit = Unit.from(matchResult.groupValues[2][0])

            if (amount <= 0) {
                throw ServiceException(ErrorCode.INVALID_INPUT_VALUE, "cycleDays 값은 1 이상이어야 합니다.")
            }

            return CyclePeriod(amount, unit)
        }
    }
}