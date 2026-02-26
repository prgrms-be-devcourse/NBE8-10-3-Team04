package com.back.domain.email.controller

import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.email.service.EmailService
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.repository.UserRepository
import com.back.domain.user.user.service.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
internal class EmailControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @MockitoBean
    lateinit var emailService: EmailService

    private fun getAuthHeader(user: User): String = "Bearer ${user.apiKey}"

    @Test
    @DisplayName("테스트 D-Day 이메일 발송 - 성공")
    fun sendTestDdayEmail_success() {
        val authUser = userService.findByLoginId("user1") ?: error("user1 not found")

        val item = itemRepository.findAll()
            .firstOrNull { it.user?.email?.isNotBlank() == true }
            ?: error("email user item not found")

        val itemUser = item.user ?: error("item user not found")
        val itemId = item.id ?: error("item id is null")

        doReturn(itemUser.email)
            .`when`(emailService)
            .sendDDayNotification(itemUser.email, item)

        mvc.perform(post("/api/v1/email/test/dday/{itemId}", itemId)
            .header("Authorization", getAuthHeader(authUser)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("테스트 D-Day 이메일 발송 성공"))
            .andExpect(jsonPath("$.recipientEmail").value(itemUser.email))
            .andExpect(jsonPath("$.itemId").value(itemId))
            .andExpect(jsonPath("$.itemName").value(item.name))
            .andExpect(jsonPath("$.userId").value(itemUser.id))
            .andExpect(jsonPath("$.userLoginId").value(itemUser.loginId))

        verify(emailService).sendDDayNotification(itemUser.email, item)
    }

    @Test
    @DisplayName("테스트 D-Day 이메일 발송 - 아이템 없음")
    fun sendTestDdayEmail_itemNotFound() {
        val authUser = userService.findByLoginId("user1") ?: error("user1 not found")

        mvc.perform(post("/api/v1/email/test/dday/{itemId}", 999999L)
            .header("Authorization", getAuthHeader(authUser)))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("이메일 발송 실패: 아이템을 찾을 수 없습니다."))

        verifyNoInteractions(emailService)
    }

    @Test
    @DisplayName("테스트 D-Day 이메일 발송 - 사용자 이메일 없음")
    fun sendTestDdayEmail_userEmailMissing() {
        val authUser = userService.findByLoginId("user1") ?: error("user1 not found")
        val category = categoryRepository.findByName("집/생활")
            ?: throw IllegalStateException("집/생활 category not found")

        val noEmailUser = userRepository.save(User("noEmailUser", "pw", ""))
        val item = itemRepository.save(
            Item(
                noEmailUser,
                category,
                "이메일없는 사용자 아이템",
                "https://example.com/no-email.png",
                LocalDate.now().minusDays(1),
                "30d",
                LocalDate.now(),
                true
            )
        )
        val itemId = item.id ?: error("item id is null")

        mvc.perform(post("/api/v1/email/test/dday/{itemId}", itemId)
            .header("Authorization", getAuthHeader(authUser)))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("이메일 발송 실패: 사용자의 이메일 주소가 없습니다."))

        verifyNoInteractions(emailService)
    }
}
