package com.back.global.event

import com.back.global.s3.S3ImageService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class S3ImageEventListener(
    private val s3ImageService: S3ImageService
) {
    // 트랜잭션이 성공적으로 커밋된 이후에만 실행됨
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleS3ImageDeleteEvent(event: S3ImageDeleteEvent) {
        if (event.imageUrls.isEmpty()) return

        if (event.imageUrls.size == 1) {
            s3ImageService.delete(event.imageUrls.first())
        } else {
            s3ImageService.deleteMultiple(event.imageUrls)
        }
    }
}