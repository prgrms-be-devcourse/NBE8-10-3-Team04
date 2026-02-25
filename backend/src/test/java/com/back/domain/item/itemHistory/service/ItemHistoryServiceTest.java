package com.back.domain.item.itemHistory.service;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.item.itemHistory.dto.ItemAllHistoryResponse;
import com.back.domain.item.itemHistory.dto.ItemHistoryResponse;
import com.back.domain.item.itemHistory.entity.ItemHistory;
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.repository.UserRepository;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemHistoryServiceTest {

    @Autowired ItemHistoryService itemHistoryService;
    @Autowired ItemHistoryRepository itemHistoryRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;

    private User user;
    private Category category;
    private Item item;

    // 각 테스트 실행 전 필요한 유저, 카테고리, 아이템 데이터를 미리 생성 및 저장
    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("testuser", "1234", "test@test.com"));
        category = categoryRepository.save(new Category("칫솔"));
        item = itemRepository.save(new Item(
                user, category, "테스트 칫솔", "https://img.example.com/test.jpg",
                LocalDate.of(2024, 1, 1), "30", LocalDate.of(2024, 1, 31), true
        ));
    }


    @Test
    @DisplayName("이력 생성 - 성공: item의 startDate로 이력이 생성된다")
    void createItemHistory_success() {
        // 아이템 이력 생성 요청
        itemHistoryService.createItemHistory(item);

        // 생성된 이력을 조회하여 시작일이 아이템의 시작일과 일치하고, 종료일은 null인지 검증
        List<ItemHistory> histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getStartDate()).isEqualTo(item.getStartDate());
        assertThat(histories.get(0).getEndDate()).isNull();
    }

    @Test
    @DisplayName("이력 생성 - 성공: 여러 번 호출 시 각각 저장된다")
    void createItemHistory_multiple() {
        // 이력 생성을 2회 호출
        itemHistoryService.createItemHistory(item);
        itemHistoryService.createItemHistory(item);

        // 각각 별도의 레코드로 저장되어 총 2개의 이력이 존재하는지 확인
        List<ItemHistory> histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.getId());
        assertThat(histories).hasSize(2);
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: 이력이 없으면 빈 리스트 반환")
    void getItemHistories_empty() {
        // 이력이 없는 상태에서 조회 시 빈 리스트가 반환되는지 확인
        List<ItemHistoryResponse> responses = itemHistoryService.getItemHistories(item.getId(), user.getId());        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: DTO에 올바른 데이터가 담긴다")
    void getItemHistories_dto_mapping() {
        // 이력 생성 후 조회
        itemHistoryService.createItemHistory(item);
        List<ItemHistoryResponse> responses = itemHistoryService.getItemHistories(item.getId(), user.getId());

        // 조회된 DTO가 아이템 ID, 시작일 등 데이터를 올바르게 매핑했는지 검증
        assertThat(responses).hasSize(1);
        ItemHistoryResponse response = responses.get(0);
        assertThat(response.itemId).isEqualTo(item.getId());
        assertThat(response.startDate).isEqualTo(item.getStartDate());
        assertThat(response.endDate).isNull();
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 성공: startDate 내림차순으로 정렬된다")
    void getItemHistories_sorted() {
        // 시간차를 두고 이력 2개 생성
        itemHistoryService.createItemHistory(item);
        itemHistoryService.createItemHistory(item);

        // 조회 시 최신순(시작일 내림차순)으로 정렬되어 반환되는지 확인
        List<ItemHistoryResponse> responses = itemHistoryService.getItemHistories(item.getId(), user.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).startDate)
                .isAfterOrEqualTo(responses.get(1).startDate);
    }

    @Test
    @DisplayName("전체 아이템 이력 조회 - 성공: 이력이 없으면 빈 리스트 반환")
    void getAllItemHistories_empty() {
        // 유저의 모든 아이템 이력 조회 시 데이터가 없으면 빈 리스트 반환 검증
        List<ItemAllHistoryResponse> responses = itemHistoryService.getAllItemHistories(user.getId());
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("전체 아이템 이력 조회 - 성공: 해당 유저의 이력만 반환된다")
    void getAllItemHistories_onlyMine() {
        // 내 아이템 이력 생성
        itemHistoryService.createItemHistory(item);

        // 다른 유저 및 아이템 이력 생성
        User otherUser = userRepository.save(new User("other", "1234", "other@test.com"));
        Item otherItem = itemRepository.save(new Item(
                otherUser, category, "다른유저 칫솔", null,
                LocalDate.of(2024, 2, 1), "30", LocalDate.of(2024, 3, 2), true
        ));
        itemHistoryService.createItemHistory(otherItem);

        // 내 이력 조회 시, 다른 유저의 데이터는 제외되고 내 데이터만 조회되는지 확인
        List<ItemAllHistoryResponse> responses = itemHistoryService.getAllItemHistories(user.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).itemName).isEqualTo(item.getName());
        assertThat(responses.get(0).itemId).isEqualTo(item.getId());
    }

    @Test
    @DisplayName("전체 아이템 이력 조회 - 성공: DTO에 카테고리명·아이템명·imgUrl이 담긴다")
    void getAllItemHistories_dto_mapping() {
        itemHistoryService.createItemHistory(item);

        List<ItemAllHistoryResponse> responses = itemHistoryService.getAllItemHistories(user.getId());

        // 전체 이력 DTO에 카테고리 이름, 이미지 URL 등이 포함되어 있는지 검증
        assertThat(responses).hasSize(1);
        ItemAllHistoryResponse res = responses.get(0);

        assertThat(res.itemId).isEqualTo(item.getId());
        assertThat(res.itemName).isEqualTo(item.getName());
        assertThat(res.categoryName).isEqualTo(category.getName());
        assertThat(res.imgUrl).isEqualTo(item.getImgUrl());
        assertThat(res.startDate).isEqualTo(item.getStartDate());
        assertThat(res.endDate).isNull();
    }

    @Test
    @DisplayName("이력 종료 - 성공: endDate가 올바르게 설정된다")
    void endHistory_success() {
        // 이력 생성 및 종료일 설정
        itemHistoryService.createItemHistory(item);
        LocalDate endDate = item.getStartDate().plusDays(30);

        // 이력 종료 처리
        itemHistoryService.endHistory(item.getId(), endDate);

        // DB에서 해당 이력의 종료일이 올바르게 업데이트되었는지 확인
        List<ItemHistory> histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.getId());
        assertThat(histories.get(0).getEndDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("이력 종료 - 성공: 종료 후 usedDays가 올바르게 계산된다")
    void endHistory_usedDays() {
        // 이력 생성
        itemHistoryService.createItemHistory(item);
        LocalDate endDate = item.getStartDate().plusDays(10);

        // 10일 뒤 날짜로 이력 종료
        itemHistoryService.endHistory(item.getId(), endDate);

        // 사용 일수(usedDays)가 10일로 계산되어 저장되었는지 검증
        List<ItemHistory> histories = itemHistoryRepository.findByItemIdOrderByStartDateDesc(item.getId());
        assertThat(histories.get(0).getUsedDays()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이력 종료 - 실패: 진행 중인 이력이 없으면 ServiceException 발생")
    void endHistory_fail_noOngoing() {
        // 진행 중인 이력이 없는 상태에서 종료를 시도하면 예외 발생 확인
        assertThatThrownBy(() -> itemHistoryService.endHistory(item.getId(), LocalDate.now()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("이력 종료 - 실패: 이미 종료된 이력만 있을 때 ServiceException 발생")
    void endHistory_fail_alreadyEnded() {
        // 이력을 생성하고 이미 종료된 상태로 만듦
        itemHistoryService.createItemHistory(item);
        itemHistoryService.endHistory(item.getId(), LocalDate.of(2024, 1, 15));

        // 이미 종료된 상태에서 다시 종료를 시도하면 예외 발생 확인
        assertThatThrownBy(() -> itemHistoryService.endHistory(item.getId(), LocalDate.now()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("특정 아이템 이력 조회 - 실패: 내 아이템이 아니면 ServiceException 발생")
    void getItemHistories_fail_notMyItem() {
        User stranger = userRepository.save(new User("stranger", "1234", "stranger@test.com"));

        // 다른 유저(stranger)가 내 아이템의 이력을 조회하려 할 때 예외(권한 없음) 발생 확인
        assertThatThrownBy(() ->
                itemHistoryService.getItemHistories(item.getId(), stranger.getId())
        ).isInstanceOf(ServiceException.class);
    }
}