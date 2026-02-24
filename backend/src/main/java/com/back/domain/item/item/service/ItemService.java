package com.back.domain.item.item.service;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.item.item.dto.CategoryAverageUsageResponse;
import com.back.domain.item.item.dto.ItemCreateRequest;
import com.back.domain.item.item.dto.ItemCycleRecommendResponse;
import com.back.domain.item.item.dto.ItemUpdateRequest;
import com.back.domain.item.item.dto.MostReplacedItemResponse;
import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.item.item.vo.CyclePeriod;
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository;
import com.back.domain.item.itemHistory.service.ItemHistoryService;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3ImageService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 아이템의 기본 CRUD 작업을 담당하는 서비스
 * - 생성, 조회, 수정, 삭제
 * - 교체, 활성화 토글
 */
@Service
@RequiredArgsConstructor
public class ItemService {
    private final UserService userService;
    private final ItemHistoryService itemHistoryService;
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final S3ImageService s3ImageService;
    private final ItemHistoryRepository itemHistoryRepository;
    private final Client genAiClient;
    private final ObjectMapper objectMapper;

    // == 조회 ==

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    // itemId로 Item 조회 (권한 검증 없음)
    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));
    }

    // itemId + userId로 Item 조회 및 권한 검증 (쿼리 1회로 최적화)
    private Item findOwnedItemOrThrow(Long itemId, Long userId) {
        return itemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION));
    }

    // userId로 User 조회
    private User findUserOrThrow(Long userId) {
        return userService.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }

    // categoryId로 Category 조회
    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ServiceException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    //목록조회용
    @Transactional(readOnly = true)
    public List<Item> findAllByUserIdOrderByNextReplacementDateAsc(Long userId) {
        return itemRepository.findAllByUserIdOrderByNextReplacementDateAsc(userId);
    }

    //단건조회용
    @Transactional(readOnly = true)
    public Item findByIdAndUserId(Long itemId, Long userId) {
        return findOwnedItemOrThrow(itemId, userId); // 메서드 재사용
    }


    //카테고리별 목록조회용
    public List<Item> findAllByUserIdAndCategoryId(Long userId, Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ServiceException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return itemRepository.findAllByUserIdAndCategoryId(userId, categoryId);
    }

    @Transactional(readOnly = true)
    public long count() {
        return itemRepository.count();
    }

    // == 생성 ==

    // 아이템 생성 (내부용)
    @Transactional
    public Item create(
            Long userId,
            Category category,
            String name,
            String imgUrl,
            LocalDate startDate,
            String cycleDays,
            LocalDate nextReplacementDate,
            Boolean isActive
    ) {
        User user = findUserOrThrow(userId);
        Item item = new Item(
                user, category, name, imgUrl,
                startDate, cycleDays, nextReplacementDate, isActive
        );

        return itemRepository.save(item);
    }

    // 아이템 생성 (외부용)
    @Transactional
    public Item createItem(Long userId, ItemCreateRequest request) {
        Category category = findCategoryOrThrow(request.categoryId());

        LocalDate startDate = request.resolvedStartDate();
        CyclePeriod cyclePeriod = CyclePeriod.from(request.cycleDays());
        LocalDate nextReplacementDate = cyclePeriod.addTo(startDate);


        String finalImgUrl = resolveImageUrl(request.image(), request.imgUrl(), null);

        Item item = create(
                userId,
                category,
                request.name(),
                finalImgUrl,
                startDate,
                request.cycleDays(),
                nextReplacementDate,
                true
        );

        // 아이템 생성 시 첫 번째 히스토리 생성 (추가)
        itemHistoryService.createItemHistory(item);

        return item;
    }

    // == 수정 ==

    // 아이템 수정
    @Transactional
    public Item modify(Long userId, Long itemId, ItemUpdateRequest request) {
        Item item = findOwnedItemOrThrow(itemId, userId); // 쿼리 1회로 감소
        Category category = findCategoryOrThrow(request.categoryId()); // 메서드 재사용

        String finalImgUrl = resolveImageUrl(request.image(), request.imgUrl(), item.getImgUrl()); // 중복 제거

        // // 기존 이미지 파일이 동일하지 않으면 삭제
        if (!Objects.equals(finalImgUrl, item.getImgUrl())) {
            s3ImageService.delete(item.getImgUrl());
        }

        // 주기(cycleDays) 수정 시 다음 교체일도 함께 변경
        LocalDate nextReplacementDate = item.getNextReplacementDate();
        if (!Objects.equals(request.cycleDays(), item.getCycleDays())) {
            CyclePeriod cyclePeriod = CyclePeriod.from(request.cycleDays());
            nextReplacementDate = cyclePeriod.addTo(item.getStartDate());
        }

        item.modify(category, request.name(), finalImgUrl, request.cycleDays(), nextReplacementDate,
                request.isActive());

        return item;
    }

    // 아이템 날짜 수정 (교체 시 사용)
    public void modifyDate(Item item) {
        // 시작일 변경 : 교체를 요청한 시각
        LocalDate newStartDate = LocalDate.now();

        // 다음 교체일 변경 : 시작일 + 주기
        CyclePeriod cyclePeriod = CyclePeriod.from(item.getCycleDays());
        LocalDate newNextReplacementDate = cyclePeriod.addTo(newStartDate);

        item.modifyDate(newStartDate, newNextReplacementDate);
    }

    // 아이템 교체
    @Transactional
    public Item replaceItem(Long userId, Long itemId) {
        Item item = findOwnedItemOrThrow(itemId, userId); // 쿼리 1회로 감소

        // 비활성 아이템은 교체 불가
        if (!item.getIsActive()) {
            throw new ServiceException(ErrorCode.INACTIVE_ITEM_CANNOT_REPLACE);
        }

        // 기존 진행중인 이력 endDate 넣기
        LocalDate today = LocalDate.now();
        itemHistoryService.endHistory(itemId, today);

        // 아이템 정보 교체
        modifyDate(item);

        // 이력 추가
        itemHistoryService.createItemHistory(item);

        return item;
    }

    @Transactional
    public Item toggleActive(Long userId, Long itemId) {
        Item item = findOwnedItemOrThrow(itemId, userId);
        item.toggleActive();
        return item;
    }

    // == 삭제 ==

    // 아이템 삭제
    // itemId로 아이템을 조회하고 요청자(userId)가 소유자인지 검증한 뒤 실제 삭제 수행
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        Item item = findOwnedItemOrThrow(itemId, userId);
        String imageUrl = item.getImgUrl();

        itemRepository.delete(item);

        // 트랜잭션 커밋 후 S3 삭제 실행
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 트랜잭션 동기화가 활성화된 경우(운영)와 아닌 경우(테스트) 분기 처리
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        s3ImageService.delete(imageUrl);
                    }
                });
            } else {
                // 테스트 환경 등 트랜잭션이 없는 경우 즉시 삭제
                s3ImageService.delete(imageUrl);
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
    private String resolveImageUrl(MultipartFile image, String providedUrl, String existingUrl) {
        // 파일이 있으면 S3 업로드
        if (image != null && !image.isEmpty()) {
            try {
                return s3ImageService.upload(image);
            } catch (IOException e) {
                throw new ServiceException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }

        // 프론트에서 보낸 URL
        if (StringUtils.isNotBlank(providedUrl)) {
            return providedUrl;
        }

        // 기존 URL 유지 (수정 시), 없으면 빈 문자열
        return StringUtils.isNotBlank(existingUrl) ? existingUrl : "";
    }
}