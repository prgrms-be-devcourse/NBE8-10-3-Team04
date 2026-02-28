package com.back.domain.item.item.controller

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.item.item.service.ItemService
import com.back.domain.item.itemHistory.entity.ItemHistory
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.service.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerTest {

    // nullable(?)과 null 초기화 대신 lateinit var를 사용하여 불필요한 null 강제 호출(!!) 제거
    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var itemService: ItemService
    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var itemRepository: ItemRepository
    @Autowired private lateinit var itemHistoryRepository: ItemHistoryRepository

    // 단일 표현식(=)과 문자열 템플릿(${}) 적용
    private fun getAuthHeader(user: User) = "Bearer ${user.apiKey}"

    @Test
    @DisplayName("아이템 목록 조회")
    fun getItems_Success_Verification() {
        // Objects.requireNonNull 대신 코틀린 내장 requireNotNull 사용
        val user = requireNotNull(userService.findById(1L)) { "테스트 유저가 없습니다." }
        val category = categoryRepository.save(Category("욕실"))

        itemRepository.save(
            Item(user, category, "비누", "https://example.com/test.jpg", LocalDate.now(), "30", LocalDate.now().plusDays(30), true)
        )

        // Spring MockMvc Kotlin DSL(get {})을 사용
        mvc.get("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.msg") { value("아이템 목록 조회 성공") }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data") { isNotEmpty() }
            jsonPath("$.data[0].name") { exists() }
        }
    }

    @Test
    @DisplayName("아이템 단건 조회")
    fun getItem_success() {
        val user = requireNotNull(userService.findByLoginId("user1"))
        val id = 1L
        val item = itemService.findById(id).get()

        mvc.get("/api/v1/items/$id") { // 문자열 템플릿 사용
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // 변경사항: unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("getItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.msg") { value("아이템 단건 조회 성공") }
            jsonPath("$.data.id") { value(item.id) }
            jsonPath("$.data.userId") { value(user.id) }
            jsonPath("$.data.categoryId") { value(item.category!!.id) }
            jsonPath("$.data.categoryName") { value(item.category!!.name) }
            jsonPath("$.data.dDay") { value(ChronoUnit.DAYS.between(LocalDate.now(), item.nextReplacementDate)) }
        }
    }

    @Test
    @DisplayName("아이템 단건 조회 - 없는 아이템")
    fun getItem_itemNotFound() {
        val user = requireNotNull(userService.findByLoginId("user1"))
        val id = 99L

        mvc.get("/api/v1/items/$id") {
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("getItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I002") }
            jsonPath("$.msg") { value("존재하지 않는 아이템이거나 권한이 없습니다.") }
        }
    }

    @Test
    @DisplayName("아이템 교체")
    fun replaceItem_success() {
        val user = requireNotNull(userService.findByLoginId("user1"))
        val id = 1L
        val item = itemService.findById(id).get()

        mvc.put("/api/v1/items/$id/replace") { // [변경점] ".formatted(id)" 대신 문자열 템플릿 사용
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("replaceItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.msg") { value("아이템 교체 처리 성공") }
            jsonPath("$.data.id") { value(item.id) }
            jsonPath("$.data.startDate") { value(LocalDate.now().toString()) }
        }
    }

    @Test
    @DisplayName("아이템 교체 - 작성자가 아닐 때")
    fun replaceItem_notOwner() {
        val user = requireNotNull(userService.findByLoginId("user2"))
        val id = 1L

        mvc.put("/api/v1/items/$id/replace") {
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("replaceItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I002") }
            jsonPath("$.msg") { value("존재하지 않는 아이템이거나 권한이 없습니다.") }
        }
    }

    @Test
    @DisplayName("아이템 수정")
    fun modifyItem_success() {
        val user = requireNotNull(userService.findByLoginId("user1"))
        val id = 1L
        val item = itemService.findById(id).get()

        // [변경점] RequestPostProcessor를 람다(with { })로 직관적으로 변경
        mvc.multipart("/api/v1/items/$id") {
            with { request ->
                request.method = "PUT"
                request
            }
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "수정")
            param("imgUrl", "edited")
            param("cycleDays", "6m")
            param("isActive", "true")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("modifyItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.msg") { value("아이템 수정 성공") }
            jsonPath("$.data.id") { value(item.id) }
            jsonPath("$.data.name") { value("수정") }
            jsonPath("$.data.imgUrl") { value("edited") }
            jsonPath("$.data.cycleDays") { value("6m") }
            jsonPath("$.data.isActive") { value(true) }
        }
    }

    @Test
    @DisplayName("아이템 수정 - 작성자가 아닐 때")
    fun modifyItem_notOwner() {
        val user = requireNotNull(userService.findByLoginId("user2"))
        val id = 1L

        mvc.multipart("/api/v1/items/$id") {
            with { request ->
                request.method = "PUT"
                request
            }
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1234")
            param("name", "수정")
            param("imgUrl", "edited")
            param("cycleDays", "6m")
            param("isActive", "true")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("modifyItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I002") }
            jsonPath("$.msg") { value("존재하지 않는 아이템이거나 권한이 없습니다.") }
        }
    }

    @Test
    @DisplayName("아이템 수정 - 존재하지 않는 카테고리")
    fun modifyItem_categoryNotFound() {
        val user = requireNotNull(userService.findByLoginId("user1"))
        val id = 1L

        mvc.multipart("/api/v1/items/$id") {
            with { request ->
                request.method = "PUT"
                request
            }
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1234")
            param("name", "수정")
            param("imgUrl", "edited")
            param("cycleDays", "6m")
            param("isActive", "true")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("modifyItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I003") }
            jsonPath("$.msg") { value("존재하지 않는 카테고리입니다.") }
        }
    }

    @Test
    @DisplayName("아이템 수정 - 유효하지 않은 주기 입력")
    fun modifyItem_InvalidCycleDate() {
        val user = requireNotNull(userService.findByLoginId("user1"))
        val id = 1L

        mvc.multipart("/api/v1/items/$id") {
            with { request ->
                request.method = "PUT"
                request
            }
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "수정")
            param("imgUrl", "edited")
            param("cycleDays", "a1")
            param("isActive", "true")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("modifyItem"))
            status { isBadRequest() }
            jsonPath("$.resultCode") { value("400-C001") }
            jsonPath("$.msg") { value("cycleDays 형식이 올바르지 않습니다. 예: 30d, 2m, 1y") }
        }
    }

    @Test
    @DisplayName("아이템 등록 - 성공")
    fun createItem_success() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "칫솔")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            param("startDate", "2025-01-01")
            param("cycleDays", "90d")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.msg") { value("아이템 등록 성공") }
            jsonPath("$.data.id") { exists() }
            jsonPath("$.data.categoryId") { value(1) }
            jsonPath("$.data.name") { value("칫솔") }
            jsonPath("$.data.imgUrl") { value("https://example.com/toothbrush.jpg") }
            jsonPath("$.data.startDate") { value("2025-01-01") }
            jsonPath("$.data.cycleDays") { value("90d") }
            jsonPath("$.data.nextReplacementDate") { value("2025-04-01") }
            jsonPath("$.data.isActive") { value(true) }
        }
    }

    @Test
    @DisplayName("아이템 등록 - 월 단위 주기")
    fun createItem_withMonthCycle() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "2")
            param("name", "필터")
            param("imgUrl", "https://example.com/filter.jpg")
            param("startDate", "2025-01-15")
            param("cycleDays", "6m")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.data.cycleDays") { value("6m") }
            jsonPath("$.data.startDate") { value("2025-01-15") }
            jsonPath("$.data.nextReplacementDate") { value("2025-07-15") }
        }
    }

    @Test
    @DisplayName("아이템 등록 - 년 단위 주기")
    fun createItem_withYearCycle() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "3")
            param("name", "매트리스")
            param("imgUrl", "https://example.com/mattress.jpg")
            param("startDate", "2024-01-01")
            param("cycleDays", "1y")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.data.cycleDays") { value("1y") }
            jsonPath("$.data.nextReplacementDate") { value("2025-01-01") }
        }
    }

    @Test
    @DisplayName("아이템 등록 실패 - categoryId 누락")
    fun createItem_missingCategoryId() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("name", "칫솔")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            param("cycleDays", "90d")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isBadRequest() }
            jsonPath("$.resultCode") { value("400-C001") }
        }
    }

    @Test
    @DisplayName("아이템 등록 실패 - name 누락")
    fun createItem_missingName() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            param("cycleDays", "90d")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isBadRequest() }
            jsonPath("$.resultCode") { value("400-C001") }
        }
    }

    @Test
    @DisplayName("아이템 등록 실패 - cycleDays 누락")
    fun createItem_missingCycleDays() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "칫솔")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isBadRequest() }
            jsonPath("$.resultCode") { value("400-C001") }
        }
    }

    @Test
    @DisplayName("아이템 등록 실패 - 잘못된 cycleDays 형식")
    fun createItem_invalidCycleDaysFormat() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "칫솔")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            param("cycleDays", "invalid")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isBadRequest() }
            jsonPath("$.resultCode") { value("400-C001") }
            jsonPath("$.msg") { value("cycleDays 형식이 올바르지 않습니다. 예: 30d, 2m, 1y") }
        }
    }

    @Test
    @DisplayName("아이템 등록 실패 - 존재하지 않는 카테고리")
    fun createItem_categoryNotFound() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "9999")
            param("name", "칫솔")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            param("cycleDays", "90d")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I003") }
            jsonPath("$.msg") { value("존재하지 않는 카테고리입니다.") }
        }
    }

    @Test
    @DisplayName("아이템 등록 실패 - cycleDays 값이 0 이하")
    fun createItem_invalidCycleDaysValue() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "칫솔")
            param("imgUrl", "https://example.com/toothbrush.jpg")
            param("cycleDays", "0d")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isBadRequest() }
            jsonPath("$.resultCode") { value("400-C001") }
            jsonPath("$.msg") { value("cycleDays 값은 1 이상이어야 합니다.") }
        }
    }

    @Test
    @DisplayName("아이템 등록 - imgUrl 없이 등록")
    fun createItem_withoutImgUrl() {
        val user = requireNotNull(userService.findById(1L))

        mvc.multipart("/api/v1/items") {
            header("Authorization", getAuthHeader(user))
            param("categoryId", "1")
            param("name", "칫솔")
            param("startDate", "2025-01-01")
            param("cycleDays", "90d")
            contentType = MediaType.MULTIPART_FORM_DATA
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("createItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.data.imgUrl") { isEmpty() }
        }
    }

    @Test
    @DisplayName("아이템 삭제 - 성공")
    fun deleteItem_success() {
        val user = requireNotNull(userService.findById(1L))
        val id = 1L

        mvc.delete("/api/v1/items/$id") {
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("deleteItem"))
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.msg") { value("아이템 삭제 성공") }
        }
    }

    @Test
    @DisplayName("아이템 삭제 - 작성자가 아닐 때")
    fun deleteItem_notOwner() {
        val user = requireNotNull(userService.findById(2L))
        val id = 1L

        mvc.delete("/api/v1/items/$id") {
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("deleteItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I002") }
            jsonPath("$.msg") { value("존재하지 않는 아이템이거나 권한이 없습니다.") }
        }
    }

    @Test
    @DisplayName("아이템 삭제 - 존재하지 않는 아이템")
    fun deleteItem_itemNotFound() {
        val user = requireNotNull(userService.findById(1L))
        val nonExistentId = 9999L

        mvc.delete("/api/v1/items/$nonExistentId") {
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            // unresolved reference 'handler' 해결을 위해 match() 및 MockMvcResultMatchers 명시적 사용
            match(MockMvcResultMatchers.handler().handlerType(ItemController::class.java))
            match(MockMvcResultMatchers.handler().methodName("deleteItem"))
            status { isNotFound() }
            jsonPath("$.resultCode") { value("404-I002") }
            jsonPath("$.msg") { value("존재하지 않는 아이템이거나 권한이 없습니다.") }
        }
    }

    @Test
    @DisplayName("아이템 활성화/비활성화 토글")
    fun toggleItemActive_RealData() {
        val user = requireNotNull(userService.findById(1L))
        val category = categoryRepository.save(Category("욕실"))

        val item = itemRepository.save(
            Item(user, category, "토글 테스트용 칫솔", "https://example.com/img.jpg", LocalDate.now(), "30", LocalDate.now().plusDays(30), true)
        )

        mvc.put("/api/v1/items/${item.id}/toggle-active") {
            header("Authorization", getAuthHeader(user))
            contentType = MediaType.APPLICATION_JSON
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.data.isActive") { value(false) }
        }
    }

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 - 실제 DB 쿼리 검증")
    fun getCategoryAverageUsage_Integration() {
        val user = requireNotNull(userService.findById(1L))
        val category = categoryRepository.save(Category("욕실"))

        val item = itemRepository.save(
            Item(user, category, "테스트 칫솔", "https://img.example.com/test.jpg", LocalDate.of(2024, 1, 1), "30", LocalDate.of(2024, 1, 31), true)
        )

        itemHistoryRepository.save(ItemHistory(item, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 11)))
        itemHistoryRepository.save(ItemHistory(item, LocalDate.of(2024, 1, 11), LocalDate.of(2024, 1, 31)))

        mvc.get("/api/v1/items/statistics/category-average") {
            header("Authorization", getAuthHeader(user))
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.resultCode") { value("200") }
            jsonPath("$.data[0].categoryName") { value("욕실") }
            jsonPath("$.data[0].averageUsageDays") { value(15.0) }
        }
    }

    @Test
    @DisplayName("가장 자주 교체한 아이템 순위 조회")
    fun getMostReplacedItems_Integration() {
        val user = requireNotNull(userService.findById(1L))
        val category = categoryRepository.save(Category("욕실"))

        val itemA = itemRepository.save(Item(user, category, "비누", "url", LocalDate.now(), "30", LocalDate.now(), true))
        val itemB = itemRepository.save(Item(user, category, "세제", "url", LocalDate.now(), "30", LocalDate.now(), true))

        // [변경점] for 루프 대신 코틀린 내장 함수 repeat 사용
        repeat(3) {
            itemHistoryRepository.save(ItemHistory(itemA, LocalDate.now(), LocalDate.now()))
        }
        itemHistoryRepository.save(ItemHistory(itemB, LocalDate.now(), LocalDate.now()))

        mvc.get("/api/v1/items/statistics/most-replaced") {
            header("Authorization", getAuthHeader(user))
            param("limit", "10")
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].itemName") { value("비누") }
            jsonPath("$.data[0].replacementCount") { value(3) }
            jsonPath("$.data[0].categoryName") { value("욕실") }
        }
    }
}