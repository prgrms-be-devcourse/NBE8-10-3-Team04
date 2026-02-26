package com.back.domain.email.service

import com.back.domain.category.category.entity.Category
import com.back.domain.item.item.entity.Item
import com.back.domain.user.user.entity.User
import com.back.global.exception.ServiceException
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDate

@ExtendWith(MockitoExtension::class) // Mockito 확장 사용 (Mock 객체 초기화)
@DisplayName("EmailService 테스트")
internal class EmailServiceTest {
    @Mock
    private lateinit var javaMailSender: JavaMailSender// 실제 메일 전송을 막기 위한 Mock 객체

    @InjectMocks
    private lateinit var emailService: EmailService // Mock이 주입된 EmailService

    private lateinit var testUser: User
    private lateinit var testCategory: Category
    private lateinit var testItem: Item
    private lateinit var recipientEmail: String

    @BeforeEach
    fun setUp() {
        // 테스트 user
        testUser = User(
            "testUser",
            "password123",
            "test@example.com"
        )

        // 테스트 Category
        testCategory = Category(
            "테스트 카테고리"
        )

        // D-Day 알림 테스트 Item 생성
        testItem = Item(
            testUser,
            testCategory,
            "칫솔",
            "/images/toothbrush.png",
            LocalDate.of(2024, 1, 1),
            "90일",
            LocalDate.of(2024, 4, 1),
            true
        )

        // 기본 수신자 이메일
        recipientEmail = "test@example.com"

        // 테스트마다 MimeMessage 생성은 공통 스텁으로 고정
        BDDMockito.given(javaMailSender.createMimeMessage())
            .willAnswer { MimeMessage(null as Session?) }
    }

    @Test
    @DisplayName("이메일 발송 성공 테스트")
    fun sendDDayNotification_Success() {
        // 메일 전송 시 예외 없이 정상 동작하도록 설정
        BDDMockito.willDoNothing().given(javaMailSender)
            .send(any<MimeMessage>())

        // D-Day 알림 메일 발송
        val result = emailService.sendDDayNotification(recipientEmail, testItem)

        // 반환값이 수신자 이메일인지 검증
        Assertions.assertThat(result).isEqualTo(recipientEmail)

        // 메일 생성 및 전송이 각각 1번씩 호출되었는지 검증
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1)).createMimeMessage()
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1))
            .send(any<MimeMessage>())
    }

    @Test
    @DisplayName("이메일 발송 실패 시 예외 발생 테스트")
    fun sendDDayNotification_Failure() {
        // 메일 전송 시 RuntimeException 발생하도록 설정
        BDDMockito.willThrow(RuntimeException("메일 서버 오류"))
            .given(javaMailSender).send(any<MimeMessage>())

        // 메일 발송 실패 시 ServiceException이 발생하는지 검증
        Assertions.assertThatThrownBy {
            emailService.sendDDayNotification(
                recipientEmail,
                testItem
            )
        }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining("메일 발송 실패")

        // 메일 생성 및 전송 시도가 있었는지 검증
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1)).createMimeMessage()
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1))
            .send(any<MimeMessage>())
    }

    @Test
    @DisplayName("다양한 아이템 정보로 이메일 발송 테스트")
    fun sendDDayNotification_WithDifferentItems() {
        // 다른 아이템 정보로 테스트
        val differentItem = Item(
            testUser,
            testCategory,
            "수세미",
            "/images/sponge.png",
            LocalDate.of(2024, 6, 1),
            "30일",
            LocalDate.of(2024, 7, 1),
            true
        )

        // 메일 전송 정상 설정
        BDDMockito.willDoNothing().given(javaMailSender)
            .send(any<MimeMessage>())

        // 다른 수신자 + 다른 아이템으로 메일 발송
        val result = emailService.sendDDayNotification("another@example.com", differentItem)

        // 반환값 검증
        Assertions.assertThat(result).isEqualTo("another@example.com")

        // 메일 생성 및 전송이 1회 호출되었는지 검증
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1)).createMimeMessage()
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1))
            .send(any<MimeMessage>())
    }

    @ParameterizedTest(name = "수신자 [{0}] 이메일 발송 성공")
    @ValueSource(
        strings = [
            "user1@example.com",
            "user2@example.com",
            "user3@example.com"
        ]
    )
    @DisplayName("여러 수신자에게 이메일 발송 테스트")
    fun sendDDayNotification_MultipleRecipients(email: String) {
        BDDMockito.willDoNothing().given(javaMailSender)
            .send(any<MimeMessage>())

        val result = emailService.sendDDayNotification(email, testItem)

        Assertions.assertThat(result).isEqualTo(email)
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1)).createMimeMessage()
        BDDMockito.then(javaMailSender).should(BDDMockito.times(1)).send(any<MimeMessage>())
    }

    @Test
    @DisplayName("한 명의 수신자에게 여러 아이템에 대한 이메일 발송 테스트")
    fun sendDDayNotification_SingleRecipientMultipleItems() {
        val item1 = Item(
            testUser,
            testCategory,
            "칫솔",
            "/images/toothbrush.png",
            LocalDate.of(2024, 1, 1),
            "90일",
            LocalDate.of(2024, 4, 1),
            true
        )

        val item2 = Item(
            testUser,
            testCategory,
            "수세미",
            "/images/sponge.png",
            LocalDate.of(2024, 2, 1),
            "30일",
            LocalDate.of(2024, 3, 1),
            true
        )

        val item3 = Item(
            testUser,
            testCategory,
            "샤워타올",
            "/images/towel.png",
            LocalDate.of(2024, 3, 1),
            "180일",
            LocalDate.of(2024, 9, 1),
            true
        )

        // 교체 알림이 필요한 아이템 리스트
        val items = arrayOf(item1, item2, item3)

        // 메일 전송 정상 설정
        BDDMockito.willDoNothing().given(javaMailSender)
            .send(any<MimeMessage>())

        // 동일한 수신자에게 여러 아이템에 대한 메일 발송
        for (item in items) {
            val result = emailService.sendDDayNotification(recipientEmail, item)

            // 각 메일 발송마다 동일한 수신자 이메일이 반환되는지 검증
            Assertions.assertThat(result).isEqualTo(recipientEmail)
        }

        // 아이템 개수만큼 메일 생성/전송이 호출되었는지 검증
        BDDMockito.then(javaMailSender).should(BDDMockito.times(3)).createMimeMessage()
        BDDMockito.then(javaMailSender).should(BDDMockito.times(3))
            .send(any<MimeMessage>())
    }
}
