package com.back.domain.email.controller;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.email.service.EmailService;
import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.repository.UserRepository;
import com.back.domain.user.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private String getAuthHeader(User user) {
        return "Bearer " + user.getApiKey();
    }

    @Test
    @DisplayName("테스트 D-Day 이메일 발송 - 성공")
    void sendTestDdayEmail_success() throws Exception {
        User authUser = userService.findByLoginId("user1");
        if (authUser == null) {
            throw new IllegalStateException("user1 not found");
        }

        Item item = itemRepository.findAll().stream()
                .filter(it -> it.getUser() != null)
                .filter(it -> it.getUser().getEmail() != null && !it.getUser().getEmail().isBlank())
                .findFirst()
                .orElseThrow();

        given(emailService.sendDDayNotification(eq(item.getUser().getEmail()), eq(item)))
                .willReturn(item.getUser().getEmail());

        mvc.perform(post("/api/v1/email/test/dday/{itemId}", item.getId())
                        .header("Authorization", getAuthHeader(authUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("테스트 D-Day 이메일 발송 성공"))
                .andExpect(jsonPath("$.recipientEmail").value(item.getUser().getEmail()))
                .andExpect(jsonPath("$.itemId").value(item.getId()))
                .andExpect(jsonPath("$.itemName").value(item.getName()))
                .andExpect(jsonPath("$.userId").value(item.getUser().getId()))
                .andExpect(jsonPath("$.userLoginId").value(item.getUser().getLoginId()));

        verify(emailService).sendDDayNotification(item.getUser().getEmail(), item);
    }

    @Test
    @DisplayName("테스트 D-Day 이메일 발송 - 아이템 없음")
    void sendTestDdayEmail_itemNotFound() throws Exception {
        User authUser = userService.findByLoginId("user1");
        if (authUser == null) {
            throw new IllegalStateException("user1 not found");
        }

        mvc.perform(post("/api/v1/email/test/dday/{itemId}", 999999L)
                        .header("Authorization", getAuthHeader(authUser)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 발송 실패: 아이템을 찾을 수 없습니다."));

        verify(emailService, never()).sendDDayNotification(any(), any());
    }

    @Test
    @DisplayName("테스트 D-Day 이메일 발송 - 사용자 이메일 없음")
    void sendTestDdayEmail_userEmailMissing() throws Exception {
        User authUser = userService.findByLoginId("user1");
        if (authUser == null) {
            throw new IllegalStateException("user1 not found");
        }

        Category category = Objects.requireNonNull(categoryRepository.findByName("집/생활"));

        User noEmailUser = userRepository.save(new User("noEmailUser", "pw", ""));
        Item item = itemRepository.save(new Item(
                noEmailUser,
                category,
                "이메일없는 사용자 아이템",
                "https://example.com/no-email.png",
                LocalDate.now().minusDays(1),
                "30d",
                LocalDate.now(),
                true
        ));

        mvc.perform(post("/api/v1/email/test/dday/{itemId}", item.getId())
                        .header("Authorization", getAuthHeader(authUser)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 발송 실패: 사용자의 이메일 주소가 없습니다."));

        verify(emailService, never()).sendDDayNotification(any(), any());
    }
}
