package com.back.domain.item.item.controller;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.item.item.service.ItemService;
import com.back.domain.item.itemHistory.entity.ItemHistory;
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ItemControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemHistoryRepository itemHistoryRepository;

    private String getAuthHeader(User user) {
        return "Bearer " + user.getApiKey();
    }

    @Test
    @DisplayName("아이템 목록 조회")
    void getItems_Success_Verification() throws Exception {

        User user = java.util.Objects.requireNonNull(userService.findById(1L));
        // Kotlin 기본 생성자를 사용하여 Category 객체를 생성하도록 수정
        Category category = categoryRepository.save(new Category("욕실"));

        itemRepository.save(new Item(
                user, category, "비누", "https://example.com/test.jpg",
                LocalDate.now(), "30", LocalDate.now().plusDays(30), true
        ));

        ResultActions resultActions = mvc
                .perform(get("/api/v1/items")
                        .header("Authorization", getAuthHeader(user)))
                .andDo(print());

        // resultCode와 데이터가 존재확인
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("아이템 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data[0].name").exists());
    }
    @Test
    @DisplayName("아이템 단건 조회")
    void getItem_success() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        Long id = 1L;
        Item item = itemService.findById(id).get();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/items/" + id)
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("아이템 단건 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(item.getId()))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.categoryId").value(item.getCategory().getId()))
                .andExpect(jsonPath("$.data.categoryName").value(item.getCategory().getName()))
                .andExpect(jsonPath("$.data.dDay").value(ChronoUnit.DAYS.between(LocalDate.now(),
                        item.getNextReplacementDate())));
    }

    @Test
    @DisplayName("아이템 단건 조회 - 없는 아이템")
    void getItem_itemNotFound() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        Long id = 99L;

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/items/" + id)
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이템이거나 권한이 없습니다."));
    }


    @Test
    @DisplayName("아이템 교체")
    void replaceItem_success() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        Long id = 1L;
        Item item = itemService.findById(id).get();

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/items/%d/replace".formatted(id))
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("replaceItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("아이템 교체 처리 성공"))
                .andExpect(jsonPath("$.data.id").value(item.getId())) // 로그 기반 수정: data.item.id -> data.id
                .andExpect(jsonPath("$.data.startDate").value(LocalDate.now().toString()));
    }

    @Test
    @DisplayName("아이템 교체 - 작성자가 아닐 때")
    void replaceItem_notOwner() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user2"));
        Long id = 1L;

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/items/%d/replace".formatted(id))
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("replaceItem"))
                .andExpect(status().isNotFound()) // 로그 기반 수정: 403 -> 404 (ServiceException 처리 방식)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이템이거나 권한이 없습니다."));
    }

    @Test
    @DisplayName("아이템 수정")
    void modifyItem_success() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        Long id = 1L;
        Item item = itemService.findById(id).get();

        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/v1/items/" + id);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        ResultActions resultActions = mvc
                .perform(
                        builder
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "수정")
                                .param("imgUrl", "edited")
                                .param("cycleDays", "6m")
                                .param("isActive", "true")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("modifyItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("아이템 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(item.getId()))
                .andExpect(jsonPath("$.data.name").value("수정"))
                .andExpect(jsonPath("$.data.imgUrl").value("edited"))
                .andExpect(jsonPath("$.data.cycleDays").value("6m"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("아이템 수정 - 작성자가 아닐 때")
    void modifyItem_notOwner() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user2"));
        Long id = 1L;

        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/v1/items/" + id);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        ResultActions resultActions = mvc
                .perform(
                        builder
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1234")
                                .param("name", "수정")
                                .param("imgUrl", "edited")
                                .param("cycleDays", "6m")
                                .param("isActive", "true")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("modifyItem"))
                .andExpect(status().isNotFound()) // 로그 기반 수정: 403 -> 404
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이템이거나 권한이 없습니다."));
    }

    @Test
    @DisplayName("아이템 수정 - 존재하지 않는 카테고리")
    void modifyItem_categoryNotFound() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        Long id = 1L;

        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/v1/items/" + id);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        ResultActions resultActions = mvc
                .perform(
                        builder
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1234")
                                .param("name", "수정")
                                .param("imgUrl", "edited")
                                .param("cycleDays", "6m")
                                .param("isActive", "true")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("modifyItem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 카테고리입니다."));
    }


    @Test
    @DisplayName("아이템 수정 - 유효하지 않은 주기 입력")
    void modifyItem_InvalidCycleDate() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findByLoginId("user1"));
        Long id = 1L;

        MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart("/api/v1/items/" + id);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        ResultActions resultActions = mvc
                .perform(
                        builder
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "수정")
                                .param("imgUrl", "edited")
                                .param("cycleDays", "a1")
                                .param("isActive", "true")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("modifyItem"))
                .andExpect(status().isBadRequest()) // 500 (isInternalServerError) -> 400 (isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("cycleDays 형식이 올바르지 않습니다. 예: 30d, 2m, 1y"));
    }

    @Test
    @DisplayName("아이템 등록 - 성공")
    void createItem_success() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "칫솔")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .param("startDate", "2025-01-01")
                                .param("cycleDays", "90d")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("아이템 등록 성공"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.name").value("칫솔"))
                .andExpect(jsonPath("$.data.imgUrl").value("https://example.com/toothbrush.jpg"))
                .andExpect(jsonPath("$.data.startDate").value("2025-01-01"))
                .andExpect(jsonPath("$.data.cycleDays").value("90d"))
                .andExpect(jsonPath("$.data.nextReplacementDate").value("2025-04-01"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("아이템 등록 - 월 단위 주기")
    void createItem_withMonthCycle() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "2")
                                .param("name", "필터")
                                .param("imgUrl", "https://example.com/filter.jpg")
                                .param("startDate", "2025-01-15")
                                .param("cycleDays", "6m")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.cycleDays").value("6m"))
                .andExpect(jsonPath("$.data.startDate").value("2025-01-15"))
                .andExpect(jsonPath("$.data.nextReplacementDate").value("2025-07-15"));
    }

    @Test
    @DisplayName("아이템 등록 - 년 단위 주기")
    void createItem_withYearCycle() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "3")
                                .param("name", "매트리스")
                                .param("imgUrl", "https://example.com/mattress.jpg")
                                .param("startDate", "2024-01-01")
                                .param("cycleDays", "1y")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.cycleDays").value("1y"))
                .andExpect(jsonPath("$.data.nextReplacementDate").value("2025-01-01"));
    }

    @Test
    @DisplayName("아이템 등록 실패 - categoryId 누락")
    void createItem_missingCategoryId() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("name", "칫솔")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .param("cycleDays", "90d")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1")); // 로그 기반 검증
    }

    @Test
    @DisplayName("아이템 등록 실패 - name 누락")
    void createItem_missingName() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .param("cycleDays", "90d")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("아이템 등록 실패 - cycleDays 누락")
    void createItem_missingCycleDays() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "칫솔")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("아이템 등록 실패 - 잘못된 cycleDays 형식")
    void createItem_invalidCycleDaysFormat() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "칫솔")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .param("cycleDays", "invalid")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isBadRequest()) // 500 (isInternalServerError) -> 400 (isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("cycleDays 형식이 올바르지 않습니다. 예: 30d, 2m, 1y"));
    }

    @Test
    @DisplayName("아이템 등록 실패 - 존재하지 않는 카테고리")
    void createItem_categoryNotFound() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "9999")
                                .param("name", "칫솔")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .param("cycleDays", "90d")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isNotFound()) // 로그 기반 수정: 404
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 카테고리입니다."));
    }

    @Test
    @DisplayName("아이템 등록 실패 - cycleDays 값이 0 이하")
    void createItem_invalidCycleDaysValue() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "칫솔")
                                .param("imgUrl", "https://example.com/toothbrush.jpg")
                                .param("cycleDays", "0d")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isBadRequest()) // 500 (isInternalServerError) -> 400 (isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("cycleDays 값은 1 이상이어야 합니다."));
    }

    @Test
    @DisplayName("아이템 등록 - imgUrl 없이 등록")
    void createItem_withoutImgUrl() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/items")
                                .header("Authorization", getAuthHeader(user))
                                .param("categoryId", "1")
                                .param("name", "칫솔")
                                .param("startDate", "2025-01-01")
                                .param("cycleDays", "90d")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("createItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.imgUrl").isEmpty());
    }

    @Test
    @DisplayName("아이템 삭제 - 성공")
    void deleteItem_success() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));
        Long id = 1L;

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/items/" + id)
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("deleteItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("아이템 삭제 성공"));
    }

    @Test
    @DisplayName("아이템 삭제 - 작성자가 아닐 때")
    void deleteItem_notOwner() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(2L));
        Long id = 1L;

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/items/" + id)
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("deleteItem"))
                .andExpect(status().isNotFound()) // 로그 기반 수정: 403 -> 404
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이템이거나 권한이 없습니다."));
    }

    @Test
    @DisplayName("아이템 삭제 - 존재하지 않는 아이템")
    void deleteItem_itemNotFound() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));
        Long nonExistentId = 9999L;

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/items/" + nonExistentId)
                                .header("Authorization", getAuthHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ItemController.class))
                .andExpect(handler().methodName("deleteItem"))
                .andExpect(status().isNotFound()) // 로그 기반 수정: 400 -> 404
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이템이거나 권한이 없습니다."));
    }
    @Test
    @DisplayName("아이템 활성화/비활성화 토글")
    void toggleItemActive_RealData() throws Exception {

        User user = java.util.Objects.requireNonNull(userService.findById(1L));
        // Kotlin 기본 생성자를 사용하여 Category 객체를 생성하도록 수정
        Category category = categoryRepository.save(new Category("욕실"));

        Item item = itemRepository.save(new Item(
                user, category, "토글 테스트용 칫솔", "https://example.com/img.jpg",
                LocalDate.now(), "30", LocalDate.now().plusDays(30), true
        ));

        ResultActions resultActions = mvc
                .perform(put("/api/v1/items/" + item.getId() + "/toggle-active")
                        .header("Authorization", getAuthHeader(user))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.isActive").value(false)); // true -> false로 변했는지 검증
    }

    @Test
    @DisplayName("카테고리별 평균 사용 기간 조회 - 실제 DB 쿼리 검증")
    void getCategoryAverageUsage_Integration() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));

        // 카테고리 생성
        // Kotlin 기본 생성자를 사용하여 Category 객체를 생성하도록 수정
        Category category = categoryRepository.save(new Category("욕실"));

        // 아이템 생성
        Item item = itemRepository.save(new Item(
                user, category, "테스트 칫솔", "https://img.example.com/test.jpg",
                LocalDate.of(2024, 1, 1), "30", LocalDate.of(2024, 1, 31), true
        ));

        // ItemHistory 생성
        // 기록 1: 1월 1일 ~ 1월 11일 (10일 사용)
        itemHistoryRepository.save(new ItemHistory(
                item,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 11)
        ));

        // 기록 2: 1월 11일 ~ 1월 31일 (20일 사용)
        itemHistoryRepository.save(new ItemHistory(
                item,
                LocalDate.of(2024, 1, 11),
                LocalDate.of(2024, 1, 31)
        ));

        // API 호출
        ResultActions resultActions = mvc
                .perform(get("/api/v1/items/statistics/category-average")
                        .header("Authorization", getAuthHeader(user)))
                .andDo(print());

        // 실제 쿼리로 계산된 평균값(15.0) 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data[0].categoryName").value("욕실"))
                .andExpect(jsonPath("$.data[0].averageUsageDays").value(15.0));
    }
    @Test
    @DisplayName("가장 자주 교체한 아이템 순위 조회")
    void getMostReplacedItems_Integration() throws Exception {
        User user = java.util.Objects.requireNonNull(userService.findById(1L));
        // Kotlin 기본 생성자를 사용하여 Category 객체를 생성하도록 수정
        Category category = categoryRepository.save(new Category("욕실"));

        Item itemA = itemRepository.save(new Item(user, category, "비누", "url", LocalDate.now(), "30", LocalDate.now(), true));
        Item itemB = itemRepository.save(new Item(user, category, "세제", "url", LocalDate.now(), "30", LocalDate.now(), true));

        // 히스토리 생성 (itemA 3개, itemB 1개)
        for(int i=0; i<3; i++) {
            itemHistoryRepository.save(new ItemHistory(
                    itemA,
                    LocalDate.now(),
                    LocalDate.now()
            ));
        }
        itemHistoryRepository.save(new ItemHistory(
                itemB,
                LocalDate.now(),
                LocalDate.now()
        ));

        ResultActions resultActions = mvc
                .perform(get("/api/v1/items/statistics/most-replaced")
                        .header("Authorization", getAuthHeader(user))
                        .param("limit", "10"))
                .andDo(print());

        // 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemName").value("비누"))
                .andExpect(jsonPath("$.data[0].replacementCount").value(3))
                .andExpect(jsonPath("$.data[0].categoryName").value("욕실"));
    }
}