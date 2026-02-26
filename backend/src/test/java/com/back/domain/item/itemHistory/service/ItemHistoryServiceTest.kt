package com.back.domain.item.itemHistory.service

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.repository.UserRepository
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
internal class ItemHistoryServiceTest {
    @Autowired
    lateinit var itemHistoryService: ItemHistoryService

    @Autowired
    lateinit var itemHistoryRepository: ItemHistoryRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    private lateinit var user: User
    private lateinit var category: Category
    private lateinit var item: Item

    // 각 테스트 실행 전 필요한 유저, 카테고리, 아이템 데이터를 미리 생성 및 저장
    @BeforeEach
    fun setUp() {
        user = userRepository.save(User("testuser", "1234", "test@test.com"))
        category = categoryRepository.save(Category("칫솔"))
        item = itemRepository.save(
            Item(
                user = user,
                category = category,
                name = "테스트 칫솔",
                imgUrl = "https://img.example.com/test.jpg",
                startDate = LocalDate.of(2024, 1, 1),
                cycleDays = "30",
                nextReplacementDate = LocalDate.of(2024, 1, 31),
                isActive = true
            )
        )
    }


    @Test
    @DisplayName("이력 생성 - 성공: item의 startDate로 이력이 생성된다")
    fun createItemHistory_success() {
        // 아이템 이력 생성 요청
        itemHistoryService.createItemHistory(item)

        // 생성된 이력을 조회하여 시작일이 아이템의 시작일과 일치하고, 종료일은 null인지 검증
        val histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.id!!)
        assertThat(histories).hasSize(1)

        assertThat(histories[0].startDate).isEqualTo(item.startDate)
        assertThat(histories[0].endDate).isNull()
    }

    @Test
    @DisplayName("이력 생성 - 성공: 여러 번 호출 시 각각 저장된다")
    fun createItemHistory_multiple() {
        // 이력 생성을 2회 호출
        itemHistoryService.createItemHistory(item)
        itemHistoryService.createItemHistory(item)

        // 각각 별도의 레코드로 저장되어 총 2개의 이력이 존재하는지 확인
        val histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.id!!)
        assertThat(histories).hasSize(2)
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: 이력이 없으면 빈 리스트 반환")
    fun getItemHistories_empty() {
        // 이력이 없는 상태에서 조회 시 빈 리스트가 반환되는지 확인
        val responses = itemHistoryService.getItemHistories(item.id!!, user.id)
        assertThat(responses).isEmpty()
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: DTO에 올바른 데이터가 담긴다")
    fun getItemHistories_dto_mapping() {
        // 이력 생성 후 조회
        itemHistoryService.createItemHistory(item)
        val responses = itemHistoryService.getItemHistories(item.id!!, user.id)

        // 조회된 DTO가 아이템 ID, 시작일 등 데이터를 올바르게 매핑했는지 검증
        assertThat(responses).hasSize(1)
        val response = responses[0]
        assertThat(response.itemId).isEqualTo(item.id)
        assertThat(response.startDate).isEqualTo(item.startDate)
        assertThat(response.endDate).isNull()
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: startDate 내림차순으로 정렬된다")
    fun getItemHistories_sorted() {
        // 시간차를 두고 이력 2개 생성
        itemHistoryService.createItemHistory(item)
        itemHistoryService.createItemHistory(item)

        // 조회 시 최신순(시작일 내림차순)으로 정렬되어 반환되는지 확인
        val responses = itemHistoryService.getItemHistories(item.id!!, user.id)

        assertThat(responses).hasSize(2)
        assertThat(responses[0].startDate).isAfterOrEqualTo(responses[1].startDate)
    }

    @Test
    @DisplayName("전체 아이템 이력 조회 - 성공: 이력이 없으면 빈 리스트 반환")
    fun getAllItemHistories_empty() {
        // 유저의 모든 아이템 이력 조회 시 데이터가 없으면 빈 리스트 반환 검증
        val responses = itemHistoryService.getAllItemHistories(user.id)
        assertThat(responses).isEmpty()
    }

    @Test
    @DisplayName("전체 아이템 이력 조회 - 성공: 해당 유저의 이력만 반환된다")
    fun getAllItemHistories_onlyMine() {
        // 내 아이템 이력 생성
        itemHistoryService.createItemHistory(item)

        // 다른 유저 및 아이템 이력 생성
        val otherUser = userRepository.save(User("other", "1234", "other@test.com"))
        val otherItem = itemRepository.save(
            Item(
                user = otherUser,
                category = category,
                name = "다른유저 칫솔",
                imgUrl = null,
                startDate = LocalDate.of(2024, 2, 1),
                cycleDays = "30",
                nextReplacementDate = LocalDate.of(2024, 3, 2),
                isActive = true
            )
        )
        itemHistoryService.createItemHistory(otherItem)

        // 내 이력 조회 시, 다른 유저의 데이터는 제외되고 내 데이터만 조회되는지 확인
        val responses = itemHistoryService.getAllItemHistories(user.id)

        assertThat(responses).hasSize(1)
        assertThat(responses[0].itemName).isEqualTo(item.name)
        assertThat(responses[0].itemId).isEqualTo(item.id)
    }

    @Test
    @DisplayName("전체 아이템 이력 조회 - 성공: DTO에 카테고리명·아이템명·imgUrl이 담긴다")
    fun getAllItemHistories_dto_mapping() {
        itemHistoryService.createItemHistory(item)

        val responses = itemHistoryService.getAllItemHistories(user.id)

        // 전체 이력 DTO에 카테고리 이름, 이미지 URL 등이 포함되어 있는지 검증
        assertThat(responses).hasSize(1)
        val res = responses[0]

        assertThat(res.itemId).isEqualTo(item.id)
        assertThat(res.itemName).isEqualTo(item.name)
        assertThat(res.categoryName).isEqualTo(category.name)
        assertThat(res.imgUrl).isEqualTo(item.imgUrl)
        assertThat(res.startDate).isEqualTo(item.startDate)
        assertThat(res.endDate).isNull()
    }

    @Test
    @DisplayName("이력 종료 - 성공: endDate가 올바르게 설정된다")
    fun endHistory_success() {
        // 이력 생성 및 종료일 설정
        itemHistoryService.createItemHistory(item)
        val endDate = item.startDate!!.plusDays(30)

        // 이력 종료 처리
        itemHistoryService.endHistory(item.id!!, endDate)

        // DB에서 해당 이력의 종료일이 올바르게 업데이트되었는지 확인
        val histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.id!!)
        assertThat(histories[0].endDate).isEqualTo(endDate)
    }

    @Test
    @DisplayName("이력 종료 - 성공: 종료 후 usedDays가 올바르게 계산된다")
    fun endHistory_usedDays() {
        // 이력 생성
        itemHistoryService.createItemHistory(item)
        val endDate = item.startDate!!.plusDays(10)

        // 10일 뒤 날짜로 이력 종료
        itemHistoryService.endHistory(item.id!!, endDate)

        // 사용 일수(usedDays)가 10일로 계산되어 저장되었는지 검증
        val histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.id!!)
        assertThat(histories[0].usedDays).isEqualTo(10L)
    }

    @Test
    @DisplayName("이력 종료 - 실패: 진행 중인 이력이 없으면 ServiceException 발생")
    fun endHistory_fail_noOngoing() {
        assertThatThrownBy { itemHistoryService.endHistory(item.id!!, LocalDate.now()) }
            .isInstanceOf(ServiceException::class.java) // 코틀린 클래스 참조 문법(::class.java)
    }

    @Test
    @DisplayName("이력 종료 - 실패: 이미 종료된 이력만 있을 때 ServiceException 발생")
    fun endHistory_fail_alreadyEnded() {
        // 이력을 생성하고 이미 종료된 상태로 만듦
        itemHistoryService.createItemHistory(item)
        itemHistoryService.endHistory(item.id!!, LocalDate.of(2024, 1, 15))

        // 이미 종료된 상태에서 다시 종료를 시도하면 예외 발생 확인
        assertThatThrownBy { itemHistoryService.endHistory(item.id!!, LocalDate.now()) }
            .isInstanceOf(ServiceException::class.java)
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 실패: 내 아이템이 아니면 ServiceException 발생")
    fun getItemHistories_fail_notMyItem() {
        val stranger = userRepository.save(User("stranger", "1234", "stranger@test.com"))

        // 다른 유저(stranger)가 내 아이템의 이력을 조회하려 할 때 예외(권한 없음) 발생 확인
        assertThatThrownBy { itemHistoryService.getItemHistories(item.id!!, stranger.id) }
            .isInstanceOf(ServiceException::class.java)
    }
}