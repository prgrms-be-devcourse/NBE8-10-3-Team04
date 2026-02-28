package com.back.domain.email.dto

import com.back.domain.item.item.entity.Item
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import java.time.LocalDate

data class ReplacementEmailContent(
    val itemName: String,
    val startDate: LocalDate,
    val cycleDays: String,
    val nextReplacementDate: LocalDate
) {

    fun toHtmlContent(): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; }
                .container { margin: 50px; padding: 20px; }
                .header { background-color: #4CAF50; color: white; padding: 15px; border-radius: 5px; }
                .content { margin-top: 20px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
                .item-info { background-color: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px; }
                .footer { margin-top: 20px; color: #666; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>🔔 교체 알림</h2>
                </div>

                <div class="content">
                    <p>안녕하세요!</p>
                    <p>등록하신 아이템의 교체 시기가 되었습니다.</p>

                    <div class="item-info">
                        <h3>📦 아이템 정보</h3>
                        <p><strong>아이템 이름:</strong> $itemName</p>
                        <p><strong>시작일:</strong> $startDate</p>
                        <p><strong>교체 주기:</strong> $cycleDays</p>
                        <p><strong>교체 예정일:</strong> $nextReplacementDate</p>
                        <p><strong>D-Day:</strong> <span style="color: red; font-weight: bold;">오늘!</span></p>
                    </div>

                    <p>아이템을 교체하신 후 앱에서 교체 완료 처리를 해주세요.</p>
                </div>

                <div class="footer">
                    <p>본 메일은 자동으로 발송되었습니다.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    companion object {
        fun from(item: Item): ReplacementEmailContent {
            return ReplacementEmailContent(
                itemName = item.name ?: throw ServiceException(ErrorCode.DATA_NOT_FOUND, "아이템 이름이 없습니다."),
                startDate = item.startDate ?: throw ServiceException(ErrorCode.DATA_NOT_FOUND, "아이템 시작일이 없습니다."),
                cycleDays = item.cycleDays ?: throw ServiceException(ErrorCode.DATA_NOT_FOUND, "아이템 주기 정보가 없습니다."),
                nextReplacementDate = item.nextReplacementDate ?: throw ServiceException(ErrorCode.DATA_NOT_FOUND, "아이템 교체 예정일이 없습니다.")
            )
        }
    }
}
