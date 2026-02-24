package com.back.domain.item.item.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * D-day 계산을 위한 유틸리티 클래스
 * 아이템의 다음 교체일까지 남은 일수를 계산합니다.
 */

// 기존 인스턴스화 방지 코드를 빼고 코틀린 object를 사용하여 싱글톤으로 만들고 인스턴스화를 원천 차단
object DDayCalculator {
    /**
     * D-day 계산 로직
     * - 다음 교체일이 없으면 -1 반환
     * - 있으면 오늘부터 다음 교체일까지 남은 일수 계산
     * - 음수가 나올 수 있음 (교체일이 지난 경우)
     *
     * @param nextReplacementDate 다음 교체 예정일
     * @return D-day (남은 일수, null인 경우 -1)
     */
    @JvmStatic
    fun calculate(nextReplacementDate: LocalDate?): Long {
        return nextReplacementDate?.let {
            ChronoUnit.DAYS.between(LocalDate.now(), it)
        } ?: -1L
    }
}
