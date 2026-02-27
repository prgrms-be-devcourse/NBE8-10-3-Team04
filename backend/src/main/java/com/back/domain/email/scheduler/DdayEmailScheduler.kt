package com.back.domain.email.scheduler

import com.back.domain.email.service.EmailService
import com.back.domain.item.item.repository.ItemRepository
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DdayEmailScheduler(
    private val itemRepository: ItemRepository,
    private val emailService: EmailService
) {

    /**
     * 매일 오전 9시에 D-Day가 0인 아이템들을 확인하고 이메일 발송
     * cron: 초 분 시 일 월 요일
     * "0 0 9 * * *" = 매일 오전 9시 정각
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional // Lazy 로딩된 연관 엔티티(User) 접근을 위해 트랜잭션 유지
    fun checkAndSendDdayEmails() {
        // 오늘 날짜
        val today = LocalDate.now()

        // 교체 예정일이 오늘이고 활성화된 아이템 목록 조회
        val itemsDueToday = itemRepository.findAllByNextReplacementDateAndIsActive(today, true)

        for (item in itemsDueToday) {
            val member = item.user
                ?: throw ServiceException(ErrorCode.DATA_NOT_FOUND, "아이템 사용자 정보가 없습니다. itemId=${item.id}")
            emailService.sendDDayNotification(member.email, item)
        }
    }
}
