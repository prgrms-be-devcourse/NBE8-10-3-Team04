package com.back.domain.email.scheduler

import com.back.domain.category.category.entity.Category
import com.back.domain.email.service.EmailService
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.user.user.entity.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("DdayEmailScheduler 테스트")
internal class DdayEmailSchedulerTest {
    @Mock
    private lateinit var itemRepository: ItemRepository

    @Mock
    private lateinit var emailService: EmailService

    @InjectMocks
    private lateinit var ddayEmailScheduler: DdayEmailScheduler

    @Test
    @DisplayName("오늘 교체 대상 아이템이 없으면 이메일을 발송하지 않는다")
    fun checkAndSendDdayEmails_noItemsDueToday() {

        whenever(itemRepository.findAllByNextReplacementDateAndIsActive(
            any<LocalDate>(),
            eq(true)
        )).thenReturn(
            emptyList()
        )

        ddayEmailScheduler.checkAndSendDdayEmails()

        verify(itemRepository, times(1))
            .findAllByNextReplacementDateAndIsActive(
                any<LocalDate>(),
                eq(true)
            )

        verify(emailService, never())
            .sendDDayNotification(any(), any())
    }

    @Test
    @DisplayName("오늘 교체 대상 아이템이 있으면 사용자 이메일로 발송한다")
    fun checkAndSendDdayEmails_sendNotifications() {
        val user1 = mock<User>()
        whenever(user1.email).thenReturn("user1@test.com")

        val user2 = mock<User>()
        whenever(user2.email).thenReturn("user2@test.com")

        val category = Category("욕실")

        val item1 = Item(
            user1,
            category,
            "칫솔",
            null,
            LocalDate.now().minusDays(90),
            "90d",
            LocalDate.now(),
            true
        )

        val item2 = Item(
            user2,
            category,
            "수세미",
            null,
            LocalDate.now().minusDays(30),
            "30d",
            LocalDate.now(),
            true
        )

        whenever(itemRepository.findAllByNextReplacementDateAndIsActive(
            any<LocalDate>(),
            eq(true)
        )).thenReturn(
            listOf(item1, item2)
        )

        ddayEmailScheduler.checkAndSendDdayEmails()

        verify(itemRepository, times(1))
            .findAllByNextReplacementDateAndIsActive(
                any<LocalDate>(),
                eq(true)
            )
        verify(emailService).sendDDayNotification(user1.email, item1)

        verify(emailService).sendDDayNotification(user2.email, item2)
    }
}
