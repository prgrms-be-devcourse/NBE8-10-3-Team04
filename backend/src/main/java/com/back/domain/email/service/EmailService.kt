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
            mimeMessageHelper.setSubject("[교체 알림] ${item.name} 교체 시기입니다!")

            // HTML 형식의 이메일 내용
            mimeMessageHelper.setText(emailContent.toHtmlContent(), true)

            javaMailSender.send(mimeMessage)
            recipientEmail
        }.fold(
            onSuccess = { it },
            onFailure = { throw ServiceException(ErrorCode.EMAIL_SEND_FAILED) }
        )
}
