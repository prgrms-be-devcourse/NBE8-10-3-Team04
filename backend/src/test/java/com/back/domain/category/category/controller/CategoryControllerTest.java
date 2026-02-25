package com.back.domain.category.category.controller;

import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    // UserService 주입
    @Autowired
    private UserService userService;

    // 인증 헤더를 만들기 위한 편의 메서드 추가
    private String getAuthHeader(User user) {
        return "Bearer " + user.getApiKey();
    }

    @Test
    @DisplayName("카테고리 조회 - BaseInitData 기본 카테고리 8개 조회 성공")
    void getCategories_success_withBaseInitData() throws Exception {
        // BaseInitData 등에 의해 생성된 기존 유저 조회
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));

        // 헤더에 인증 정보 포함하여 요청
        mvc.perform(get("/api/v1/categories")
                        .header("Authorization", getAuthHeader(user)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("카테고리 목록 조회 성공")) // CategoryController의 응답 메시지와 일치하도록 수정

                // BaseInitData에서 8개 생성
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(8))))

                // 특정 카테고리 이름이 포함되는지 확인 (순서 의존 X)
                .andExpect(jsonPath("$.data[*].name", hasItems(
                        "집/생활", "욕실", "주방", "뷰티", "반려동물", "자동차", "전자기기", "업무"
                )))

                // 각 요소에 id, name 존재
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty());
    }
}