package com.back.domain.email.controller

import com.back.domain.email.service.EmailService
import com.back.domain.item.item.repository.ItemRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/email")
class EmailController (
    private val emailService: EmailService,
    private val itemRepository: ItemRepository
) {
    //테스트용 D-Day 이메일 발송 API
    @PostMapping("/test/dday/{itemId}")
    fun sendTestDdayEmail(@PathVariable itemId: Long): ResponseEntity<MutableMap<String, Any>> {
        val response = mutableMapOf<String, Any>()

        return runCatching {
            // 아이템 조회
            val item = itemRepository.findById(itemId)
                .orElseThrow { RuntimeException("아이템을 찾을 수 없습니다.") }

            // 아이템에 연결된 사용자 조회 // 사용자 미연결 방어
            val user = item.user ?: throw RuntimeException("아이템에 연결된 사용자가 없습니다.")

            val email = user.email?.takeIf { it.isNotBlank() }
                ?: throw RuntimeException("사용자의 이메일 주소가 없습니다.")

            // D-Day 알림 이메일 발송
            val sentToEmail = emailService.sendDDayNotification(email, item)

            // 테스트 확인용 응답 데이터
            response["success"] = true
            response["message"] = "테스트 D-Day 이메일 발송 성공"
            response["recipientEmail"] = sentToEmail
            response["itemId"] = itemId
            response["itemName"] = item.name ?: ""
            response["userId"] = user.id
            response["userLoginId"] = user.loginId

            ResponseEntity.ok(response)
        }.getOrElse { e ->
            // 예외 발생시 스택 트레이스 출력
            e.printStackTrace()
            response["success"] = false
            response["message"] = "이메일 발송 실패: ${e.message}"
            ResponseEntity.badRequest().body(response)
        }
    }
}
