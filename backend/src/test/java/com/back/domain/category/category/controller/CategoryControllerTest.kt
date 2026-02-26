package com.back.domain.category.category.controller

import com.back.domain.user.user.entity.User
import com.back.domain.user.user.service.UserService
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import java.util.*

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
internal class CategoryControllerTest {

    // null 허용(?) 및 초기값(null) 대신 lateinit var를 사용하여 불필요한 null 강제 호출(!!) 제거
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    // 문자열 템플릿(${})과 단일 표현식(=)을 사용
    private fun getAuthHeader(user: User) = "Bearer ${user.apiKey}"

    @Test
    @DisplayName("카테고리 조회 - BaseInitData 기본 카테고리 8개 조회 성공")
    fun getCategories_success_withBaseInitData() {
        // Java의 Objects.requireNonNull 대신 코틀린의 requireNotNull 사용
        // null일 경우 예외 메시지 지정
        val user = requireNotNull(userService.findByLoginId("user1")) { "테스트 유저(user1)가 존재하지 않습니다." }

        // 코틀린에서 제공하는 MockMvc DSL(mvc.get { ... })을 사용하여 체이닝과 가독성 향상
        mvc.get("/api/v1/categories") {
            header("Authorization", getAuthHeader(user))
        }.andDo {
            print()
        }.andExpect {
            status { isOk() }
            jsonPath("$.resultCode") { value("200-1") }
            jsonPath("$.msg") { value("카테고리 목록 조회 성공") }

            // 불필요하고 복잡하게 명시되었던 제네릭 타입(<MutableCollection<*>?> 등) 제거
            jsonPath("$.data") {
                value(hasSize<Any>(greaterThanOrEqualTo(8)))
            }
            jsonPath("$.data[*].name") {
                value(hasItems("집/생활", "욕실", "주방", "뷰티", "반려동물", "자동차", "전자기기", "업무"))
            }
            jsonPath("$.data[0].id") { exists() }
            jsonPath("$.data[0].name") { isNotEmpty() }
        }
    }
}