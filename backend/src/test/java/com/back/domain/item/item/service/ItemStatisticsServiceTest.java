package com.back.domain.item.item.service;

import com.back.domain.item.item.dto.CategoryAverageUsageResponse;
import com.back.domain.item.item.dto.MostReplacedItemResponse;
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemStatisticsService 테스트")
class ItemStatisticsServiceTest {

    @Mock
    private ItemHistoryRepository itemHistoryRepository;

    @InjectMocks
    private ItemStatisticsService itemStatisticsService;

    private Long testUserId;
    private List<Map<String, Object>> categoryAverageUsageData;
    private List<Map<String, Object>> mostReplacedItemsData;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 사용자 ID 설정
        testUserId = 1L;

        // 카테고리별 평균 사용일 데이터 초기화 (DB 조회 결과 모킹용)
        categoryAverageUsageData = new ArrayList<>();

        Map<String, Object> category1 = new HashMap<>();
        category1.put("categoryId", 1L);
        category1.put("categoryName", "생활용품");
        category1.put("averageUsageDays", 45.5);
        categoryAverageUsageData.add(category1);

        Map<String, Object> category2 = new HashMap<>();
        category2.put("categoryId", 2L);
        category2.put("categoryName", "주방용품");
        category2.put("averageUsageDays", 30.0);
        categoryAverageUsageData.add(category2);

        Map<String, Object> category3 = new HashMap<>();
        category3.put("categoryId", 3L);
        category3.put("categoryName", "욕실용품");
        category3.put("averageUsageDays", 60.3);
        categoryAverageUsageData.add(category3);

        // 가장 자주 교체한 아이템 데이터 초기화 (DB 조회 결과 모킹용)
        mostReplacedItemsData = new ArrayList<>();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("itemId", 1L);
        item1.put("itemName", "칫솔");
        item1.put("categoryName", "욕실용품");
        item1.put("replacementCount", 10L);
        item1.put("imgUrl", "/images/toothbrush.png");
        mostReplacedItemsData.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("itemId", 2L);
        item2.put("itemName", "수세미");
        item2.put("categoryName", "주방용품");
        item2.put("replacementCount", 8L);
        item2.put("imgUrl", "/images/sponge.png");
        mostReplacedItemsData.add(item2);

        Map<String, Object> item3 = new HashMap<>();
        item3.put("itemId", 3L);
        item3.put("itemName", "마스크");
        item3.put("categoryName", "생활용품");
        item3.put("replacementCount", 5L);
        item3.put("imgUrl", "/images/mask.png");
        mostReplacedItemsData.add(item3);
    }

    // == 카테고리별 평균 사용 기간 조회 테스트 ==

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 성공")
    void getCategoryAverageUsage_Success() {
        // 리포지토리가 미리 정의된 카테고리 데이터를 반환하도록 설정
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
                .willReturn(categoryAverageUsageData);

        // 서비스 메서드 호출
        List<CategoryAverageUsageResponse> result =
                itemStatisticsService.getCategoryAverageUsage(testUserId);

        // 반환된 리스트의 크기 및 각 항목의 데이터 정합성 검증
        assertThat(result).hasSize(3);

        assertThat(result.get(0).categoryId()).isEqualTo(1L);
        assertThat(result.get(0).categoryName()).isEqualTo("생활용품");
        assertThat(result.get(0).averageUsageDays()).isEqualTo(45.5);

        assertThat(result.get(1).categoryId()).isEqualTo(2L);
        assertThat(result.get(1).categoryName()).isEqualTo("주방용품");
        assertThat(result.get(1).averageUsageDays()).isEqualTo(30.0);

        assertThat(result.get(2).categoryId()).isEqualTo(3L);
        assertThat(result.get(2).categoryName()).isEqualTo("욕실용품");
        assertThat(result.get(2).averageUsageDays()).isEqualTo(60.3);

        // 리포지토리 메서드가 정확히 1회 호출되었는지 확인
        verify(itemHistoryRepository, times(1))
                .findAverageUsageDaysByCategoryForUser(testUserId);
    }

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 성공 - 빈 결과")
    void getCategoryAverageUsage_Success_EmptyResult() {
        // 리포지토리가 빈 리스트를 반환하도록 설정
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
                .willReturn(new ArrayList<>());

        // 서비스 메서드 호출
        List<CategoryAverageUsageResponse> result =
                itemStatisticsService.getCategoryAverageUsage(testUserId);

        // 결과가 비어있는지 확인
        assertThat(result).isEmpty();
        verify(itemHistoryRepository, times(1))
                .findAverageUsageDaysByCategoryForUser(testUserId);
    }

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 성공 - 단일 카테고리")
    void getCategoryAverageUsage_Success_SingleCategory() {
        // 단일 카테고리 데이터 준비 및 리포지토리 반환 설정
        List<Map<String, Object>> singleCategoryData = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("categoryId", 1L);
        category.put("categoryName", "생활용품");
        category.put("averageUsageDays", 50.0);
        singleCategoryData.add(category);

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
                .willReturn(singleCategoryData);

        // 서비스 메서드 호출
        List<CategoryAverageUsageResponse> result =
                itemStatisticsService.getCategoryAverageUsage(testUserId);

        // 결과 리스트 크기 및 데이터 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
        assertThat(result.get(0).categoryName()).isEqualTo("생활용품");
        assertThat(result.get(0).averageUsageDays()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 - averageUsageDays가 null인 경우")
    void getCategoryAverageUsage_WithNullAverageDays() {
        // 평균 사용일이 null인 데이터 준비
        List<Map<String, Object>> dataWithNull = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("categoryId", 1L);
        category.put("categoryName", "생활용품");
        category.put("averageUsageDays", null);
        dataWithNull.add(category);

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
                .willReturn(dataWithNull);

        // 서비스 메서드 호출
        List<CategoryAverageUsageResponse> result =
                itemStatisticsService.getCategoryAverageUsage(testUserId);

        // null 값이 0.0으로 안전하게 처리되었는지 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).averageUsageDays()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 - 다양한 평균값 처리")
    void getCategoryAverageUsage_VariousAverageValues() {
        // 다양한 숫자형 데이터 준비 (정수형 실수, 큰 수, 소수점)
        List<Map<String, Object>> variousData = new ArrayList<>();

        Map<String, Object> category1 = new HashMap<>();
        category1.put("categoryId", 1L);
        category1.put("categoryName", "아주 짧은 주기");
        category1.put("averageUsageDays", 1.0);
        variousData.add(category1);

        Map<String, Object> category2 = new HashMap<>();
        category2.put("categoryId", 2L);
        category2.put("categoryName", "아주 긴 주기");
        category2.put("averageUsageDays", 365.5);
        variousData.add(category2);

        Map<String, Object> category3 = new HashMap<>();
        category3.put("categoryId", 3L);
        category3.put("categoryName", "소수점 포함");
        category3.put("averageUsageDays", 27.89);
        variousData.add(category3);

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
                .willReturn(variousData);

        // 서비스 메서드 호출
        List<CategoryAverageUsageResponse> result =
                itemStatisticsService.getCategoryAverageUsage(testUserId);

        // 각 값이 정확히 매핑되었는지 검증
        assertThat(result).hasSize(3);
        assertThat(result.get(0).averageUsageDays()).isEqualTo(1.0);
        assertThat(result.get(1).averageUsageDays()).isEqualTo(365.5);
        assertThat(result.get(2).averageUsageDays()).isEqualTo(27.89);
    }

    // == 가장 자주 교체한 아이템 순위 조회 테스트 ==

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 성공 - 5개 제한")
    void getMostReplacedItems_Success_Limit5() {
        // limit 5로 요청 시 미리 준비된 3개의 아이템 리스트 반환 설정
        int limit = 5;
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(mostReplacedItemsData);

        // 서비스 메서드 호출
        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        // 결과 크기 및 각 아이템의 상세 정보 매핑 검증
        assertThat(result).hasSize(3);

        assertThat(result.get(0).itemId()).isEqualTo(1L);
        assertThat(result.get(0).itemName()).isEqualTo("칫솔");
        assertThat(result.get(0).categoryName()).isEqualTo("욕실용품");
        assertThat(result.get(0).replacementCount()).isEqualTo(10L);
        assertThat(result.get(0).imgUrl()).isEqualTo("/images/toothbrush.png");

        assertThat(result.get(1).itemId()).isEqualTo(2L);
        assertThat(result.get(1).itemName()).isEqualTo("수세미");
        assertThat(result.get(1).replacementCount()).isEqualTo(8L);

        assertThat(result.get(2).itemId()).isEqualTo(3L);
        assertThat(result.get(2).itemName()).isEqualTo("마스크");
        assertThat(result.get(2).replacementCount()).isEqualTo(5L);

        // 리포지토리 호출 시 limit 파라미터 전달 확인
        verify(itemHistoryRepository, times(1))
                .findMostReplacedItemsByUser(testUserId, limit);
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 성공 - 3개 제한")
    void getMostReplacedItems_Success_Limit3() {
        // limit 3으로 설정 및 호출
        int limit = 3;
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(mostReplacedItemsData);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(3);
        verify(itemHistoryRepository, times(1))
                .findMostReplacedItemsByUser(testUserId, limit);
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 성공 - 10개 제한")
    void getMostReplacedItems_Success_Limit10() {
        // limit 10으로 설정 및 호출
        int limit = 10;
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(mostReplacedItemsData);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(3);
        verify(itemHistoryRepository, times(1))
                .findMostReplacedItemsByUser(testUserId, limit);
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 성공 - 빈 결과")
    void getMostReplacedItems_Success_EmptyResult() {
        // 리포지토리가 빈 리스트를 반환할 때 서비스가 빈 리스트를 반환하는지 확인
        int limit = 5;
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(new ArrayList<>());

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).isEmpty();
        verify(itemHistoryRepository, times(1))
                .findMostReplacedItemsByUser(testUserId, limit);
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 성공 - 단일 아이템")
    void getMostReplacedItems_Success_SingleItem() {
        // 단일 아이템 데이터 준비
        int limit = 5;
        List<Map<String, Object>> singleItemData = new ArrayList<>();

        Map<String, Object> item = new HashMap<>();
        item.put("itemId", 1L);
        item.put("itemName", "칫솔");
        item.put("categoryName", "욕실용품");
        item.put("replacementCount", 15L);
        item.put("imgUrl", "/images/toothbrush.png");
        singleItemData.add(item);

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(singleItemData);

        // 서비스 호출 및 검증
        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo(1L);
        assertThat(result.get(0).itemName()).isEqualTo("칫솔");
        assertThat(result.get(0).replacementCount()).isEqualTo(15L);
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 - 교체 횟수 내림차순 정렬 확인")
    void getMostReplacedItems_SortedByReplacementCount() {
        // 반환된 리스트가 교체 횟수 기준 내림차순인지 확인
        int limit = 5;
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(mostReplacedItemsData);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).replacementCount()).isGreaterThanOrEqualTo(result.get(1).replacementCount());
        assertThat(result.get(1).replacementCount()).isGreaterThanOrEqualTo(result.get(2).replacementCount());
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 - 이미지 URL이 null인 경우")
    void getMostReplacedItems_WithNullImageUrl() {
        // 이미지 URL이 null인 데이터 처리 확인
        int limit = 5;
        List<Map<String, Object>> dataWithNullImage = new ArrayList<>();

        Map<String, Object> item = new HashMap<>();
        item.put("itemId", 1L);
        item.put("itemName", "칫솔");
        item.put("categoryName", "욕실용품");
        item.put("replacementCount", 10L);
        item.put("imgUrl", null);
        dataWithNullImage.add(item);

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(dataWithNullImage);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).imgUrl()).isNull();
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 조회 - 동일한 교체 횟수를 가진 아이템들")
    void getMostReplacedItems_WithSameReplacementCount() {
        // 교체 횟수가 동일한 아이템이 있을 때 정상적으로 리스트에 포함되는지 확인
        int limit = 5;
        List<Map<String, Object>> sameCountData = new ArrayList<>();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("itemId", 1L);
        item1.put("itemName", "칫솔");
        item1.put("categoryName", "욕실용품");
        item1.put("replacementCount", 5L);
        item1.put("imgUrl", "/images/toothbrush.png");
        sameCountData.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("itemId", 2L);
        item2.put("itemName", "수세미");
        item2.put("categoryName", "주방용품");
        item2.put("replacementCount", 5L);
        item2.put("imgUrl", "/images/sponge.png");
        sameCountData.add(item2);

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(sameCountData);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).replacementCount()).isEqualTo(5L);
        assertThat(result.get(1).replacementCount()).isEqualTo(5L);
    }

    // == 다양한 사용자 ID 테스트 ==

    @Test
    @DisplayName("다른 사용자의 통계 조회")
    void getStatistics_DifferentUser() {
        // 테스트 유저가 아닌 다른 유저 ID로 조회 시 빈 결과 반환 확인
        Long anotherUserId = 999L;
        List<Map<String, Object>> emptyData = new ArrayList<>();

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(anotherUserId))
                .willReturn(emptyData);
        given(itemHistoryRepository.findMostReplacedItemsByUser(anotherUserId, 5))
                .willReturn(emptyData);

        List<CategoryAverageUsageResponse> categoryResult =
                itemStatisticsService.getCategoryAverageUsage(anotherUserId);
        List<MostReplacedItemResponse> itemResult =
                itemStatisticsService.getMostReplacedItems(anotherUserId, 5);

        assertThat(categoryResult).isEmpty();
        assertThat(itemResult).isEmpty();
        verify(itemHistoryRepository, times(1))
                .findAverageUsageDaysByCategoryForUser(anotherUserId);
        verify(itemHistoryRepository, times(1))
                .findMostReplacedItemsByUser(anotherUserId, 5);
    }

    @Test
    @DisplayName("동일한 사용자로 여러 번 조회")
    void getStatistics_MultipleCalls() {
        // 서비스 메서드를 여러 번 호출했을 때 리포지토리도 동일한 횟수로 호출되는지 확인
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
                .willReturn(categoryAverageUsageData);

        itemStatisticsService.getCategoryAverageUsage(testUserId);
        itemStatisticsService.getCategoryAverageUsage(testUserId);
        itemStatisticsService.getCategoryAverageUsage(testUserId);

        verify(itemHistoryRepository, times(3))
                .findAverageUsageDaysByCategoryForUser(testUserId);
    }

    // == 엣지 케이스 테스트 ==

    @Test
    @DisplayName("limit이 1인 경우 - 가장 많이 교체한 아이템 1개만 조회")
    void getMostReplacedItems_LimitOne() {
        // limit을 1로 설정했을 때 1개의 데이터만 반환되는지 확인
        int limit = 1;
        List<Map<String, Object>> singleItemData = List.of(mostReplacedItemsData.get(0));

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(singleItemData);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemName()).isEqualTo("칫솔");
        assertThat(result.get(0).replacementCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("매우 큰 교체 횟수 처리")
    void getMostReplacedItems_LargeReplacementCount() {
        // 교체 횟수가 매우 큰 값일 때 DTO 매핑이 정상적인지 확인
        int limit = 5;
        List<Map<String, Object>> largeCountData = new ArrayList<>();

        Map<String, Object> item = new HashMap<>();
        item.put("itemId", 1L);
        item.put("itemName", "칫솔");
        item.put("categoryName", "욕실용품");
        item.put("replacementCount", 999L);
        item.put("imgUrl", "/images/toothbrush.png");
        largeCountData.add(item);

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
                .willReturn(largeCountData);

        List<MostReplacedItemResponse> result =
                itemStatisticsService.getMostReplacedItems(testUserId, limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).replacementCount()).isEqualTo(999L);
    }
}