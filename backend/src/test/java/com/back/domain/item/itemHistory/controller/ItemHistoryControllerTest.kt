package com.back.domain.item.itemHistory.controller

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.item.itemHistory.service.ItemHistoryService
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.service.UserService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
internal class ItemHistoryControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var itemHistoryService: ItemHistoryService

    private lateinit var user: User
    private lateinit var item: Item
    private lateinit var authCookie: Cookie

    // 로그인 후 인증 쿠키를 발급받는 헬퍼 메서드
    private fun loginAndGetCookie(loginId: String, password: String): Cookie {
        val result = mvc.post("/api/v1/user/login") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "loginId": "$loginId",
                    "password": "$password"
                }
            """.trimIndent()
        }.andReturn()

        return result.response.getCookie("accessToken")
            ?: throw AssertionError("인증 쿠키(accessToken)를 찾을 수 없습니다.")
    }

    // 다른 유저 및 해당 유저의 아이템 생성 헬퍼 메서드
    private fun createOtherUserAndItem(
        loginId: String,
        email: String,
        categoryName: String,
        itemName: String
    ): Item {
        val otherUser = userService.join(loginId, "1234", email)
        val category = categoryRepository.save(Category(categoryName))
        val item = itemRepository.save(
            Item(
                user = otherUser,
                category = category,
                name = itemName,
                imgUrl = null,
                startDate = LocalDate.of(2024, 2, 1),
                cycleDays = "30",
                nextReplacementDate = LocalDate.of(2024, 3, 2),
                isActive = true
            )
        )
        return item
    }

    // 각 테스트 실행 전 기본 사용자, 카테고리, 아이템 데이터 초기화
    @BeforeEach
    fun setUp() {
        user = userService.join("historyUser", "1234", "history@test.com")
        //테스트에서 공통으로 사용하는 쿠키 세팅
        authCookie = loginAndGetCookie("historyUser", "1234")
        val category = categoryRepository.save(Category("칫솔"))

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
    @DisplayName("전체 이력 조회 - 성공: 이력이 없어도 빈 배열로 응답")
    fun getAllHistories_empty() {
        // 이력이 없는 상태에서 전체 조회 요청 시 200 OK와 빈 배열 반환 검증
        mvc.get("/api/v1/items/histories") {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.resultCode") { value("200-1") }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(0) }
        }
    }

    @Test
    @DisplayName("전체 이력 조회 - 성공: 이력 존재 시 데이터 반환")
    fun getAllHistories_withData() {
        // 아이템 이력 생성
        itemHistoryService.createItemHistory(item)

        // 조회 요청 시 생성된 이력 데이터가 올바르게 반환되는지 검증
        mvc.get("/api/v1/items/histories") {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].itemName") { value(item.name) }
            jsonPath("$.data[0].categoryName") { value(item.category?.name) }
            jsonPath("$.data[0].startDate") { value(item.startDate.toString()) }
        }
    }

    @Test
    @DisplayName("전체 이력 조회 - 성공: 다른 유저의 이력은 포함되지 않는다")
    fun getAllHistories_isolatedByUser() {
        // 다른 유저 및 해당 유저의 아이템 생성
        val otherItem = createOtherUserAndItem(
            loginId = "otherUser",
            email = "other@test.com",
            categoryName = "기타 카테고리",
            itemName = "다른 칫솔"
        )
        // 다른 유저 아이템 이력 생성
        itemHistoryService.createItemHistory(otherItem)
        // 내 아이템 이력 생성
        itemHistoryService.createItemHistory(item)

        // 내 이력만 조회되고 다른 유저의 이력은 포함되지 않는지 검증 (데이터 격리)
        mvc.get("/api/v1/items/histories") {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].itemName") { value(item.name) }
        }
    }

    @Test
    @DisplayName("전체 이력 조회 - 실패: 비로그인 시 403 Forbidden")
    fun getAllHistories_unauthorized() {
        // 쿠키 없이 요청 시 403 에러 발생 검증
        mvc.get("/api/v1/items/histories")
            .andDo { print() }
            .andExpect { status { isForbidden() } }
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: 이력이 없으면 빈 배열 반환")
    fun getItemHistories_empty() {
        // 특정 아이템 ID로 조회했으나 이력이 없을 경우 빈 배열 반환 검증
        mvc.get("/api/v1/items/{itemId}/histories", item.id) {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(0) }
        }
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: 이력 존재 시 데이터 반환")
    fun getItemHistories_withData() {
        // 이력 생성
        itemHistoryService.createItemHistory(item)

        // 특정 아이템 조회 시 이력 정보가 올바르게 매핑되는지 검증
        mvc.get("/api/v1/items/{itemId}/histories", item.id) {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].itemId") { value(item.id) }
            jsonPath("$.data[0].startDate") { value(item.startDate.toString()) }
        }
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: 여러 이력이 startDate 내림차순으로 반환된다")
    fun getItemHistories_sorted() {
        // 동일 아이템에 대해 2개의 이력 생성
        itemHistoryService.createItemHistory(item)
        itemHistoryService.createItemHistory(item)

        // 2개의 데이터가 반환되는지 확인 (내림차순 정렬 로직은 Service 테스트에서 상세 검증)
        mvc.get("/api/v1/items/{itemId}/histories", item.id) {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
        }
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 실패: 존재하지 않는 itemId이면 404 예외 발생")
    fun getItemHistories_notExistItem() {
        // DB에 없는 ID 조회 시 404 Not Found 에러 발생 검증
        mvc.get("/api/v1/items/{itemId}/histories", 999999L) {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.resultCode") { exists() }
        }
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 실패: 다른 유저의 아이템을 조회하면 404/403 예외 발생 (데이터 격리)")
    fun getItemHistories_otherUserItem() {
        // 다른 유저와 그의 아이템 및 이력 생성
        val otherItem = createOtherUserAndItem(
            loginId = "otherUser",
            email = "other@test.com",
            categoryName = "기타",
            itemName = "남의 아이템"
        )
        itemHistoryService.createItemHistory(otherItem)

        // 내 계정으로 남의 아이템 이력을 조회 시도 시 예외(404 Not Found 등) 발생 검증
        mvc.get("/api/v1/items/{itemId}/histories", otherItem.id) {
            cookie(authCookie)
        }.andDo {
            print()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.resultCode") { exists() }
        }
    }
}