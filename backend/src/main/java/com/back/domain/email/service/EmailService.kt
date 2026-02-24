package com.back.domain.email.service

import com.back.domain.email.dto.ReplacementEmailContent
import com.back.domain.item.item.entity.Item
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService (
    private val javaMailSender: JavaMailSender
) {
    //  발송된 이메일 주소 반환
    fun sendDDayNotification(recipientEmail: String, item: Item): String =
        runCatching {
            val emailContent = ReplacementEmailContent.from(item)
            val mimeMessage = javaMailSender.createMimeMessage()
            val mimeMessageHelper = MimeMessageHelper(mimeMessage, false, "UTF-8")

            // 아이템 소유자의 이메일로 발송
            mimeMessageHelper.setTo(recipientEmail)

            // 메일 제목
            mimeMessageHelper.setSubject("[교체 알림] TODO추가부분 교체 시기입니다!")
            //TODO: Item 엔티티가 Java+Lombok 상태라 Kotlin에서 프로퍼티/게터 참조 호환 이슈가 있음.
            // item 도메인 Kotlin 전환 이후 `${item.name}` 형태로 정리 예정.
            // `${item.getName()}` 형태도 불가능한 이유는 (여기는 Kotlin 코드이기 때문)
            // Kotlin 컴파일 시점에 Java Lombok(@Getter) 생성 메서드를 해석하지 못해
            // `Unresolved reference getName`가 발생함.

            // HTML 형식의 이메일 내용
            mimeMessageHelper.setText(emailContent.toHtmlContent(), true)

            javaMailSender.send(mimeMessage)
            recipientEmail
        }.fold(
            onSuccess = { it },
            onFailure = { throw ServiceException(ErrorCode.EMAIL_SEND_FAILED) }
        )
}
