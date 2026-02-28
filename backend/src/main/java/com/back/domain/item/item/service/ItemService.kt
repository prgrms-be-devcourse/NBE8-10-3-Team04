package com.back.domain.item.item.service

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.item.item.dto.ItemCreateRequest
import com.back.domain.item.item.dto.ItemUpdateRequest
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.item.item.vo.CyclePeriod
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import com.back.domain.item.itemHistory.service.ItemHistoryService
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.service.UserService
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.s3.S3ImageService
import com.google.genai.Client
import lombok.RequiredArgsConstructor
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.time.LocalDate
import java.util.*
import java.util.function.Supplier

/**
 * 아이템의 기본 CRUD 작업을 담당하는 서비스
 * - 생성, 조회, 수정, 삭제
 * - 교체, 활성화 토글
 */
@Service
class ItemService(
    private val userService: UserService,
    private val itemHistoryService: ItemHistoryService,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val s3ImageService: S3ImageService,
) {

    // == 조회 ==

    fun findById(id: Long): Optional<Item> {
        return itemRepository.findById(id)
    }

    // itemId로 Item 조회 (권한 검증 없음)
    private fun findItemOrThrow(itemId: Long): Item {
        return itemRepository.findById(itemId)
            .orElseThrow { ServiceException(ErrorCode.ITEM_NOT_FOUND) }
    }

    // itemId + userId로 Item 조회 및 권한 검증 (쿼리 1회로 최적화)
    private fun findOwnedItemOrThrow(itemId: Long, userId: Long): Item {
        return itemRepository.findByIdAndUserId(itemId, userId)
            ?: throw ServiceException(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION)
    }

    // userId로 User 조회
    private fun findUserOrThrow(userId: Long): User {
        return userService.findById(userId)
            ?: throw ServiceException(ErrorCode.USER_NOT_FOUND)
    }

    // categoryId로 Category 조회
    private fun findCategoryOrThrow(categoryId: Long): Category {
        return categoryRepository.findById(categoryId)
            .orElseThrow { ServiceException(ErrorCode.CATEGORY_NOT_FOUND) }
    }

    //목록조회용
    @Transactional(readOnly = true)
    fun findAllByUserIdOrderByNextReplacementDateAsc(userId: Long): List<Item> {
        return itemRepository.findAllByUserIdOrderByNextReplacementDateAsc(userId)
    }

    //단건조회용
    @Transactional(readOnly = true)
    fun findByIdAndUserId(itemId: Long, userId: Long): Item {
        return findOwnedItemOrThrow(itemId, userId) // 메서드 재사용
    }

    //카테고리별 목록조회용
    fun findAllByUserIdAndCategoryId(userId: Long, categoryId: Long): List<Item> {
        if (!categoryRepository.existsById(categoryId)) {
            throw ServiceException(ErrorCode.CATEGORY_NOT_FOUND)
        }

        return itemRepository.findAllByUserIdAndCategoryId(userId, categoryId)
    }

    @Transactional(readOnly = true)
    fun count(): Long {
        return itemRepository.count()
    }

    // == 생성 ==

    // 아이템 생성 (내부용)
    @Transactional
    fun create(
        userId: Long,
        category: Category,
        name: String,
        imgUrl: String?,
        startDate: LocalDate,
        cycleDays: String,
        nextReplacementDate: LocalDate,
        isActive: Boolean
    ): Item {
        val user = findUserOrThrow(userId)
        val item = Item(
            user = user,
            category = category,
            name = name,
            imgUrl = imgUrl,
            startDate = startDate,
            cycleDays = cycleDays,
            nextReplacementDate = nextReplacementDate,
            isActive = isActive
        )

        return itemRepository.save(item)
    }

    // 아이템 생성 (외부용)
    @Transactional
    fun createItem(userId: Long, request: ItemCreateRequest): Item {
        val category = findCategoryOrThrow(request.categoryId)

        val startDate = request.resolvedStartDate()
        val cyclePeriod = CyclePeriod.from(request.cycleDays)
        val nextReplacementDate = cyclePeriod.addTo(startDate)

        val finalImgUrl = resolveImageUrl(request.image, request.imgUrl, null)

        val item = create(
            userId = userId,
            category = category,
            name = request.name,
            imgUrl = finalImgUrl,
            startDate = startDate,
            cycleDays = request.cycleDays,
            nextReplacementDate = nextReplacementDate,
            isActive = true
        )

        // 아이템 생성 시 첫 번째 히스토리 생성 (추가)
        itemHistoryService.createItemHistory(item)

        return item
    }

    // == 수정 ==

    // 아이템 수정
    @Transactional
    fun modify(userId: Long, itemId: Long, request: ItemUpdateRequest): Item {
        val item = findOwnedItemOrThrow(itemId, userId) // 쿼리 1회로 감소
        val category = findCategoryOrThrow(request.categoryId) // 메서드 재사용

        val finalImgUrl = resolveImageUrl(request.image, request.imgUrl, item.imgUrl) // 중복 제거

        // // 기존 이미지 파일이 동일하지 않으면 삭제
        if (finalImgUrl != item.imgUrl && item.imgUrl != null) {
            item.imgUrl?.let { s3ImageService.delete(it) }
        }

        // 주기(cycleDays) 수정 시 다음 교체일도 함께 변경
        var nextReplacementDate = item.nextReplacementDate
        if (request.cycleDays != item.cycleDays) {
            val cyclePeriod = CyclePeriod.from(request.cycleDays)
            nextReplacementDate = cyclePeriod.addTo(item.startDate)
        }

        item.modify(category, request.name, finalImgUrl, request.cycleDays, nextReplacementDate, request.isActive)

        return item
    }

    // 아이템 날짜 수정 (교체 시 사용)
    fun modifyDate(item: Item) {
        // 시작일 변경 : 교체를 요청한 시각
        val newStartDate = LocalDate.now()

        // 다음 교체일 변경 : 시작일 + 주기
        val cyclePeriod = CyclePeriod.from(item.cycleDays)
        val newNextReplacementDate = cyclePeriod.addTo(newStartDate)

        item.modifyDate(newStartDate, newNextReplacementDate)
    }

    // 아이템 교체
    @Transactional
    fun replaceItem(userId: Long, itemId: Long): Item {
        val item = findOwnedItemOrThrow(itemId, userId) // 쿼리 1회로 감소

        // 비활성 아이템은 교체 불가
        if (!item.isActive) {
            throw ServiceException(ErrorCode.INACTIVE_ITEM_CANNOT_REPLACE)
        }

        // 기존 진행중인 이력 endDate 넣기
        val today = LocalDate.now()
        itemHistoryService.endHistory(itemId, today)

        // 아이템 정보 교체
        modifyDate(item)

        // 이력 추가
        itemHistoryService.createItemHistory(item)

        return item
    }

    @Transactional
    fun toggleActive(userId: Long, itemId: Long): Item {
        val item = findOwnedItemOrThrow(itemId, userId)
        item.toggleActive()
        return item
    }

    // == 삭제 ==

    // 아이템 삭제
    // itemId로 아이템을 조회하고 요청자(userId)가 소유자인지 검증한 뒤 실제 삭제 수행
    @Transactional
    fun deleteItem(userId: Long, itemId: Long) {
        val item = findOwnedItemOrThrow(itemId, userId)
        val imageUrl = item.imgUrl

        itemRepository.delete(item)

        // 트랜잭션 커밋 후 S3 삭제 실행
        if (!imageUrl.isNullOrEmpty()) {
            // 트랜잭션 동기화가 활성화된 경우(운영)와 아닌 경우(테스트) 분기 처리
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                    override fun afterCommit() {
                        s3ImageService.delete(imageUrl)
                    }
                })
            } else {
                // 테스트 환경 등 트랜잭션이 없는 경우 즉시 삭제
                s3ImageService.delete(imageUrl)
            }
        }
    }

    // == 유틸 메서드 ==
    /**
     * 이미지 URL 결정 로직
     * image 업로드된 파일
     * providedUrl 프론트에서 전달한 URL
     * existingUrl 기존 이미지 URL (수정 시)
     * return 최종 이미지 URL
     */
    private fun resolveImageUrl(image: MultipartFile?, providedUrl: String?, existingUrl: String?): String? {
        // 파일이 있으면 S3 업로드
        if (image != null && !image.isEmpty) {
            return try {
                s3ImageService.upload(image)
            } catch (e: Exception) {
                throw ServiceException(ErrorCode.IMAGE_UPLOAD_FAILED)
            }
        }

        // 2 & 3. 제공된 URL 확인 -> 없으면 기존 URL 확인 -> 모두 없으면 null
        return providedUrl?.takeIf { it.isNotBlank() }
            ?: existingUrl?.takeIf { it.isNotBlank() }
    }
}