package com.back.global.initData;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.category.category.service.CategoryService;
import com.back.domain.item.item.dto.ItemCreateRequest;
import com.back.domain.item.item.service.ItemService;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import com.back.global.app.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;

    private final CategoryRepository categoryRepository;
    private final ItemService itemService;
    private final UserService userService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            self.createDefaultCategory();
            self.createDefaultUsers();
            self.initItems();
        };
    }

    // 서버 실행 시 카테고리 생성
    @Transactional
    public void createDefaultCategory() {
        // 이미 카테고리가 있으면 스킵 (원하면 조건 변경 가능)
        if (categoryRepository.count() > 0) return;

        categoryRepository.save(new Category("집/생활"));
        categoryRepository.save(new Category("욕실"));
        categoryRepository.save(new Category("주방"));
        categoryRepository.save(new Category("뷰티"));
        categoryRepository.save(new Category("반려동물"));
        categoryRepository.save(new Category("자동차"));
        categoryRepository.save(new Category("전자기기"));
        categoryRepository.save(new Category("업무"));
        categoryRepository.save(new Category("기타"));
    }

    @Transactional
    public void createDefaultUsers() {
        // 이미 있으면 스킵, 없으면 생성
        userService.findByLoginId("user1")
                .orElseGet(() -> userService.join("user1", "1234", "hhyukk1273@gmail.com"));

        userService.findByLoginId("user2")
                .orElseGet(() -> userService.join("user2", "1234", "user2@test.com"));
    }

    @Transactional
    public void initItems() {
        // 이미 아이템이 있으면 스킵
        if (itemService.count() > 0) return;

        // TODO: User 엔티티 붙으면 userId 대신 userId/user 엔티티로 교체
        User user1 = userService.findByLoginId("user1").orElseThrow();
        User user2 = userService.findByLoginId("user2").orElseThrow();

        Category bathroom = Objects.requireNonNull(categoryRepository.findByName("욕실"));
        Category kitchen = Objects.requireNonNull(categoryRepository.findByName("주방"));
        Category car = Objects.requireNonNull(categoryRepository.findByName("자동차"));


        itemService.createItem(user1.getId(), new ItemCreateRequest(
                bathroom.getId(),
                "칫솔",
                "https://example.com/toothbrush.png",
                null,
                LocalDate.of(2026, 1, 1),
                "3m"
        ));

        itemService.createItem(user1.getId(), new ItemCreateRequest(
                kitchen.getId(),
                "수세미",
                "https://example.com/sponge.png",
                null,
                LocalDate.of(2026, 1, 5),
                "21d"
        ));

        itemService.createItem(user2.getId(), new ItemCreateRequest(
                car.getId(),
                "엔진오일",
                "https://example.com/engineoil.png",
                null,
                LocalDate.of(2025, 12, 1),
                "6m"
        ));

        // 테스트용: 교체일이 오늘인 아이템 추가
        itemService.createItem(user1.getId(), new ItemCreateRequest(
                bathroom.getId(),
                "테스트용 칫솔 (D-Day 0)",
                "https://example.com/test-toothbrush.png",
                null,
                LocalDate.now().minusMonths(3),
                "3m"
        ));
    }

    @Transactional
    public void work1() {
        if (userService.count() > 0) return;

        User user1 = userService.join("user1", "1234", "유저1");
        if (AppConfig.isNotProd()) user1.modifyApiKey(user1.getLoginId());
    }
}
