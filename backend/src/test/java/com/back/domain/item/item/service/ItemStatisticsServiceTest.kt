package com.back.domain.item.item.service

import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

// 명시적 캐스팅으로 인한 경고를 숨기기 위해 Suppress 어노테이션 추가
@Suppress("UNCHECKED_CAST")
@ExtendWith(MockitoExtension::class)
@DisplayName("ItemStatisticsService 테스트")
internal class ItemStatisticsServiceTest {

    @Mock
    private lateinit var itemHistoryRepository: ItemHistoryRepository

    @InjectMocks
    private lateinit var itemStatisticsService: ItemStatisticsService

    private val testUserId = 1L

    // 반환 기대 타입에 맞게 Any? 대신 Any로 타입 명시
    private lateinit var categoryAverageUsageData: List<Map<String, Any>>
    private lateinit var mostReplacedItemsData: List<Map<String, Any>>

    @BeforeEach
    fun setUp() {
        // Mock 객체가 기대하는 타입으로 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        categoryAverageUsageData = listOf(
            mapOf(
                "categoryId" to 1L,
                "categoryName" to "생활용품",
                "averageUsageDays" to 45.5
            ),
            mapOf(
                "categoryId" to 2L,
                "categoryName" to "주방용품",
                "averageUsageDays" to 30.0
            ),
            mapOf(
                "categoryId" to 3L,
                "categoryName" to "욕실용품",
                "averageUsageDays" to 60.3
            )
        ) as List<Map<String, Any>>

        mostReplacedItemsData = listOf(
            mapOf(
                "itemId" to 1L,
                "itemName" to "칫솔",
                "categoryName" to "욕실용품",
                "replacementCount" to 10L,
                "imgUrl" to "/images/toothbrush.png"
            ),
            mapOf(
                "itemId" to 2L,
                "itemName" to "수세미",
                "categoryName" to "주방용품",
                "replacementCount" to 8L,
                "imgUrl" to "/images/sponge.png"
            ),
            mapOf(
                "itemId" to 3L,
                "itemName" to "마스크",
                "categoryName" to "생활용품",
                "replacementCount" to 5L,
                "imgUrl" to "/images/mask.png"
            )
        ) as List<Map<String, Any>>
    }

    // == 카테고리별 평균 사용 기간 조회 테스트 ==
    @Test
    fun `카테고리별 평균 사용 기간 조회 성공`() {
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
            .willReturn(categoryAverageUsageData)

        val result = itemStatisticsService.getCategoryAverageUsage(testUserId)

        assertThat(result).hasSize(3)

        with(result[0]) {
            assertThat(categoryId).isEqualTo(1L)
            assertThat(categoryName).isEqualTo("생활용품")
            assertThat(averageUsageDays).isEqualTo(45.5)
        }

        with(result[1]) {
            assertThat(categoryId).isEqualTo(2L)
            assertThat(categoryName).isEqualTo("주방용품")
            assertThat(averageUsageDays).isEqualTo(30.0)
        }

        with(result[2]) {
            assertThat(categoryId).isEqualTo(3L)
            assertThat(categoryName).isEqualTo("욕실용품")
            assertThat(averageUsageDays).isEqualTo(60.3)
        }

        verify(itemHistoryRepository, times(1)).findAverageUsageDaysByCategoryForUser(testUserId)
    }

    @Test
    fun `카테고리별 평균 사용 기간 조회 성공 - 빈 결과`() {
        // 빈 리스트 반환 시에도 제네릭 타입 명시
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
            .willReturn(emptyList<Map<String, Any>>())

        val result = itemStatisticsService.getCategoryAverageUsage(testUserId)

        assertThat(result).isEmpty()
        verify(itemHistoryRepository, times(1)).findAverageUsageDaysByCategoryForUser(testUserId)
    }

    @Test
    fun `카테고리별 평균 사용 기간 조회 성공 - 단일 카테고리`() {
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        val singleCategoryData = listOf(
            mapOf(
                "categoryId" to 1L,
                "categoryName" to "생활용품",
                "averageUsageDays" to 50.0
            )
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
            .willReturn(singleCategoryData)

        val result = itemStatisticsService.getCategoryAverageUsage(testUserId)

        assertThat(result).hasSize(1)
        with(result[0]) {
            assertThat(categoryId).isEqualTo(1L)
            assertThat(categoryName).isEqualTo("생활용품")
            assertThat(averageUsageDays).isEqualTo(50.0)
        }
    }

    @Test
    fun `카테고리별 평균 사용 기간 조회 - averageUsageDays가 null인 경우`() {
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가하여 null 포함 맵의 타입 에러 해결
        val dataWithNull = listOf(
            mapOf(
                "categoryId" to 1L,
                "categoryName" to "생활용품",
                "averageUsageDays" to null
            )
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
            .willReturn(dataWithNull)

        val result = itemStatisticsService.getCategoryAverageUsage(testUserId)

        assertThat(result).hasSize(1)
        assertThat(result[0].averageUsageDays).isEqualTo(0.0)
    }

    @Test
    fun `카테고리별 평균 사용 기간 조회 - 다양한 평균값 처리`() {
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        val variousData = listOf(
            mapOf("categoryId" to 1L, "categoryName" to "짧은 주기", "averageUsageDays" to 1.0),
            mapOf("categoryId" to 2L, "categoryName" to "긴 주기", "averageUsageDays" to 365.5),
            mapOf("categoryId" to 3L, "categoryName" to "소수점", "averageUsageDays" to 27.89)
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
            .willReturn(variousData)

        val result = itemStatisticsService.getCategoryAverageUsage(testUserId)

        assertThat(result).hasSize(3)
        assertThat(result[0].averageUsageDays).isEqualTo(1.0)
        assertThat(result[1].averageUsageDays).isEqualTo(365.5)
        assertThat(result[2].averageUsageDays).isEqualTo(27.89)
    }

    // == 가장 자주 교체한 아이템 순위 조회 테스트 ==
    @Test
    fun `가장 자주 교체한 아이템 조회 성공 - 5개 제한`() {
        val limit = 5
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(mostReplacedItemsData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(3)

        with(result[0]) {
            assertThat(itemId).isEqualTo(1L)
            assertThat(itemName).isEqualTo("칫솔")
            assertThat(categoryName).isEqualTo("욕실용품")
            assertThat(replacementCount).isEqualTo(10L)
            assertThat(imgUrl).isEqualTo("/images/toothbrush.png")
        }

        with(result[1]) {
            assertThat(itemId).isEqualTo(2L)
            assertThat(itemName).isEqualTo("수세미")
            assertThat(replacementCount).isEqualTo(8L)
        }

        with(result[2]) {
            assertThat(itemId).isEqualTo(3L)
            assertThat(itemName).isEqualTo("마스크")
            assertThat(replacementCount).isEqualTo(5L)
        }

        verify(itemHistoryRepository, times(1)).findMostReplacedItemsByUser(testUserId, limit)
    }

    @Test
    fun `가장 자주 교체한 아이템 조회 성공 - 3개 제한`() {
        val limit = 3
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(mostReplacedItemsData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(3)
        verify(itemHistoryRepository, times(1)).findMostReplacedItemsByUser(testUserId, limit)
    }

    @Test
    fun `가장 자주 교체한 아이템 조회 성공 - 빈 결과`() {
        // 빈 리스트 반환 시에도 제네릭 타입 명시
        val limit = 5
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(emptyList<Map<String, Any>>())

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).isEmpty()
        verify(itemHistoryRepository, times(1)).findMostReplacedItemsByUser(testUserId, limit)
    }

    @Test
    fun `가장 자주 교체한 아이템 조회 성공 - 단일 아이템`() {
        val limit = 5
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        val singleItemData = listOf(
            mapOf(
                "itemId" to 1L,
                "itemName" to "칫솔",
                "categoryName" to "욕실용품",
                "replacementCount" to 15L,
                "imgUrl" to "/images/toothbrush.png"
            )
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(singleItemData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(1L)
        assertThat(result[0].itemName).isEqualTo("칫솔")
        assertThat(result[0].replacementCount).isEqualTo(15L)
    }

    @Test
    fun `가장 자주 교체한 아이템 조회 - 교체 횟수 내림차순 정렬 확인`() {
        val limit = 5
        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(mostReplacedItemsData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(3)
        assertThat(result[0].replacementCount).isGreaterThanOrEqualTo(result[1].replacementCount)
        assertThat(result[1].replacementCount).isGreaterThanOrEqualTo(result[2].replacementCount)
    }

    @Test
    fun `가장 자주 교체한 아이템 조회 - 이미지 URL이 null인 경우`() {
        val limit = 5
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        val dataWithNullImage = listOf(
            mapOf(
                "itemId" to 1L,
                "itemName" to "칫솔",
                "categoryName" to "욕실용품",
                "replacementCount" to 10L,
                "imgUrl" to null
            )
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(dataWithNullImage)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(1)
        assertThat(result[0].imgUrl).isNull()
    }

    @Test
    fun `가장 자주 교체한 아이템 조회 - 동일한 교체 횟수를 가진 아이템들`() {
        val limit = 5
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        val sameCountData = listOf(
            mapOf(
                "itemId" to 1L,
                "itemName" to "칫솔",
                "categoryName" to "욕실용품",
                "replacementCount" to 5L,
                "imgUrl" to "/images/toothbrush.png"
            ),
            mapOf(
                "itemId" to 2L,
                "itemName" to "수세미",
                "categoryName" to "주방용품",
                "replacementCount" to 5L,
                "imgUrl" to "/images/sponge.png"
            )
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(sameCountData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(2)
        assertThat(result[0].replacementCount).isEqualTo(5L)
        assertThat(result[1].replacementCount).isEqualTo(5L)
    }

    // == 다양한 사용자 ID 테스트 ==
    @Test
    fun `다른 사용자의 통계 조회`() {
        val anotherUserId = 999L
        // 빈 리스트 반환 시에도 제네릭 타입 명시
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(anotherUserId))
            .willReturn(emptyList<Map<String, Any>>())
        given(itemHistoryRepository.findMostReplacedItemsByUser(anotherUserId, 5))
            .willReturn(emptyList<Map<String, Any>>())

        val categoryResult = itemStatisticsService.getCategoryAverageUsage(anotherUserId)
        val itemResult = itemStatisticsService.getMostReplacedItems(anotherUserId, 5)

        assertThat(categoryResult).isEmpty()
        assertThat(itemResult).isEmpty()

        verify(itemHistoryRepository, times(1)).findAverageUsageDaysByCategoryForUser(anotherUserId)
        verify(itemHistoryRepository, times(1)).findMostReplacedItemsByUser(anotherUserId, 5)
    }

    @Test
    fun `동일한 사용자로 여러 번 조회`() {
        given(itemHistoryRepository.findAverageUsageDaysByCategoryForUser(testUserId))
            .willReturn(categoryAverageUsageData)

        repeat(3) {
            itemStatisticsService.getCategoryAverageUsage(testUserId)
        }

        verify(itemHistoryRepository, times(3)).findAverageUsageDaysByCategoryForUser(testUserId)
    }

    // == 엣지 케이스 테스트 ==
    @Test
    fun `limit이 1인 경우 - 가장 많이 교체한 아이템 1개만 조회`() {
        val limit = 1
        val singleItemData = listOf(mostReplacedItemsData[0])

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(singleItemData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(1)
        assertThat(result[0].itemName).isEqualTo("칫솔")
        assertThat(result[0].replacementCount).isEqualTo(10L)
    }

    @Test
    fun `매우 큰 교체 횟수 처리`() {
        val limit = 5
        // 명시적 캐스팅 (as List<Map<String, Any>>) 추가
        val largeCountData = listOf(
            mapOf(
                "itemId" to 1L,
                "itemName" to "칫솔",
                "categoryName" to "욕실용품",
                "replacementCount" to 999L,
                "imgUrl" to "/images/toothbrush.png"
            )
        ) as List<Map<String, Any>>

        given(itemHistoryRepository.findMostReplacedItemsByUser(testUserId, limit))
            .willReturn(largeCountData)

        val result = itemStatisticsService.getMostReplacedItems(testUserId, limit)

        assertThat(result).hasSize(1)
        assertThat(result[0].replacementCount).isEqualTo(999L)
    }
}