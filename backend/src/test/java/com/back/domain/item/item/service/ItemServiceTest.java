package com.back.domain.item.item.service;

import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.item.item.dto.ItemCreateRequest;
import com.back.domain.item.item.dto.ItemUpdateRequest;
import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository;
import com.back.domain.item.itemHistory.service.ItemHistoryService;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.service.UserService;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3ImageService;
import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService 테스트")
class ItemServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ItemHistoryService itemHistoryService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private S3ImageService s3ImageService;

    @Mock
    private ItemHistoryRepository itemHistoryRepository;

    @Mock
    private Client genAiClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ItemService itemService;

    private User testUser;
    private Category testCategory;
    private Item testItem;
    private ItemCreateRequest createRequest;
    private ItemUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 테스트 실행 전 공통으로 사용할 User, Category, Item, Request 객체 초기화
        testUser = User.builder()
                .id(1L)
                .password("password123")
                .email("test@example.com")
                .build();

        testCategory = new Category("생활용품");

        testItem = new Item(
                testUser,
                testCategory,
                "칫솔",
                "/images/toothbrush.png",
                LocalDate.of(2024, 1, 1),
                "90d",
                LocalDate.of(2024, 4, 1),
                true
        );

        createRequest = new ItemCreateRequest(
                1L,
                "칫솔",
                "/images/toothbrush.png",
                null,
                LocalDate.of(2024, 1, 1),
                "90d"
        );

        updateRequest = new ItemUpdateRequest(
                1L,
                "전동칫솔",
                "/images/electric_toothbrush.png",
                null,
                "120d",
                true
        );
    }

    // == 조회 테스트 ==

    @Test
    @DisplayName("아이템 ID로 조회 성공")
    void findById_Success() {
        // Repository 동작 모킹: ID 1로 조회 시 testItem 반환
        given(itemRepository.findById(1L)).willReturn(Optional.of(testItem));

        // 서비스 메서드 호출
        Optional<Item> result = itemService.findById(1L);

        // 반환된 객체가 존재하고 이름이 일치하는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("칫솔");
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("사용자 ID로 아이템 목록 조회 성공")
    void findAllByUserIdOrderByNextReplacementDateAsc_Success() {
        // Repository 동작 모킹: 사용자 ID로 조회 시 아이템 리스트 반환
        List<Item> items = List.of(testItem);
        given(itemRepository.findAllByUserIdOrderByNextReplacementDateAsc(1L))
                .willReturn(items);

        // 서비스 메서드 호출
        List<Item> result = itemService.findAllByUserIdOrderByNextReplacementDateAsc(1L);

        // 리스트 크기와 첫 번째 요소의 이름 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("칫솔");
        verify(itemRepository, times(1)).findAllByUserIdOrderByNextReplacementDateAsc(1L);
    }

    @Test
    @DisplayName("아이템 ID와 사용자 ID로 조회 성공")
    void findByIdAndUserId_Success() {
        // Repository 동작 모킹: 아이템 ID와 사용자 ID로 조회 성공 시 testItem 반환
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.of(testItem));

        // 서비스 메서드 호출
        Item result = itemService.findByIdAndUserId(1L, 1L);

        // 결과 이름 검증 및 Repository 호출 확인
        assertThat(result.getName()).isEqualTo("칫솔");
        verify(itemRepository, times(1)).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("아이템 ID와 사용자 ID로 조회 실패 - 권한 없음")
    void findByIdAndUserId_Failure_NoPermission() {
        // Repository 동작 모킹: 조회 실패(권한 없음 또는 존재하지 않음) 시 Empty 반환
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.empty());

        // 예외 발생 검증: ITEM_NOT_FOUND_OR_NO_PERMISSION 메시지 포함 여부 확인
        assertThatThrownBy(() -> itemService.findByIdAndUserId(1L, 1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("카테고리별 아이템 목록 조회 성공")
    void findAllByUserIdAndCategoryId_Success() {
        // 카테고리 존재 여부 확인 및 조회 동작 모킹
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(itemRepository.findAllByUserIdAndCategoryId(1L, 1L))
                .willReturn(List.of(testItem));

        // 서비스 메서드 호출
        List<Item> result = itemService.findAllByUserIdAndCategoryId(1L, 1L);

        // 리스트 크기 및 요소 검증
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("칫솔");
    }

    @Test
    @DisplayName("카테고리별 아이템 목록 조회 실패 - 카테고리 없음")
    void findAllByUserIdAndCategoryId_Failure_CategoryNotFound() {
        // 카테고리가 존재하지 않는 상황 모킹
        given(categoryRepository.existsById(1L)).willReturn(false);

        // 예외 발생 검증: CATEGORY_NOT_FOUND
        assertThatThrownBy(() -> itemService.findAllByUserIdAndCategoryId(1L, 1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    // == 생성 테스트 ==

    @Test
    @DisplayName("아이템 생성 성공")
    void createItem_Success() {
        // 사용자 및 카테고리 조회, 아이템 저장, 히스토리 생성 동작 모킹
        given(userService.findById(1L)).willReturn(Optional.of(testUser));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        given(itemRepository.save(any(Item.class))).willReturn(testItem);
        doNothing().when(itemHistoryService).createItemHistory(any(Item.class));

        // 서비스 메서드 호출
        Item result = itemService.createItem(1L, createRequest);

        // 생성된 아이템 검증 및 의존성 메서드 호출 횟수 확인
        assertThat(result.getName()).isEqualTo("칫솔");
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(itemHistoryService, times(1)).createItemHistory(any(Item.class));
    }

    @Test
    @DisplayName("아이템 생성 실패 - 사용자 없음")
    void createItem_Failure_UserNotFound() {
        // 카테고리는 존재하지만 사용자가 없는 상황 모킹
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        given(userService.findById(1L)).willReturn(Optional.empty());

        // 예외 발생 검증: USER_NOT_FOUND
        assertThatThrownBy(() -> itemService.createItem(1L, createRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("아이템 생성 실패 - 카테고리 없음")
    void createItem_Failure_CategoryNotFound() {
        // 카테고리 조회 실패 상황 모킹
        given(categoryRepository.findById(1L)).willReturn(Optional.empty());

        // 예외 발생 검증: CATEGORY_NOT_FOUND (사용자 조회 전에 발생해야 함)
        assertThatThrownBy(() -> itemService.createItem(1L, createRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("아이템 생성 성공 - 이미지 업로드")
    void createItem_Success_WithImageUpload() throws IOException {
        // 이미지 파일이 포함된 요청 생성 및 업로드 동작 모킹
        MultipartFile mockFile = mock(MultipartFile.class);
        given(mockFile.isEmpty()).willReturn(false);

        ItemCreateRequest requestWithImage = new ItemCreateRequest(
                1L, "칫솔", null, mockFile, LocalDate.of(2024, 1, 1), "90d"
        );

        given(userService.findById(1L)).willReturn(Optional.of(testUser));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        given(s3ImageService.upload(mockFile)).willReturn("https://s3.amazonaws.com/uploaded-image.png");
        given(itemRepository.save(any(Item.class))).willReturn(testItem);

        // 서비스 메서드 호출
        Item result = itemService.createItem(1L, requestWithImage);

        // S3 업로드 호출 및 저장 검증
        verify(s3ImageService, times(1)).upload(mockFile);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    // == 수정 테스트 ==

    @Test
    @DisplayName("아이템 수정 성공")
    void modify_Success() {
        // 아이템 소유권 확인, 카테고리 조회, 기존 이미지 삭제 동작 모킹
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.of(testItem));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory));
        doNothing().when(s3ImageService).delete(anyString());

        // 서비스 메서드 호출
        Item result = itemService.modify(1L, 1L, updateRequest);

        // 수정된 아이템의 이름과 주기가 업데이트되었는지 검증
        assertThat(result.getName()).isEqualTo("전동칫솔");
        assertThat(result.getCycleDays()).isEqualTo("120d");
    }

    @Test
    @DisplayName("아이템 수정 실패 - 권한 없음")
    void modify_Failure_NoPermission() {
        // 소유권 확인 실패 상황 모킹
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.empty());

        // 예외 발생 검증
        assertThatThrownBy(() -> itemService.modify(1L, 1L, updateRequest))
                .isInstanceOf(ServiceException.class);
    }

    // == 교체 테스트 ==

    @Test
    @DisplayName("아이템 교체 성공")
    void replaceItem_Success() {
        // 아이템 조회 및 히스토리 종료/생성 동작 모킹
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.of(testItem));
        doNothing().when(itemHistoryService).endHistory(anyLong(), any(LocalDate.class));
        doNothing().when(itemHistoryService).createItemHistory(any(Item.class));

        // 서비스 메서드 호출
        Item result = itemService.replaceItem(1L, 1L);

        // 시작일이 오늘 날짜로 갱신되었는지 확인 및 히스토리 서비스 호출 검증
        assertThat(result.getStartDate()).isEqualTo(LocalDate.now());
        verify(itemHistoryService, times(1)).endHistory(anyLong(), any(LocalDate.class));
        verify(itemHistoryService, times(1)).createItemHistory(any(Item.class));
    }

    @Test
    @DisplayName("아이템 교체 실패 - 비활성 아이템")
    void replaceItem_Failure_InactiveItem() {
        Item inactiveItem = new Item(
                testUser,
                testCategory,
                "칫솔",
                "/images/toothbrush.png",
                LocalDate.of(2024, 1, 1),
                "90d",
                LocalDate.of(2024, 4, 1),
                false
        );


        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.of(inactiveItem));

        // 비활성 아이템 교체 시도 시 예외 발생 검증
        assertThatThrownBy(() -> itemService.replaceItem(1L, 1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INACTIVE_ITEM_CANNOT_REPLACE.getMessage());
    }

    // == 활성화 토글 테스트 ==

    @Test
    @DisplayName("아이템 활성화 토글 성공")
    void toggleActive_Success() {
        // 아이템 조회 모킹
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.of(testItem));

        // 토글 메서드 호출 (기존 true -> false)
        Item result = itemService.toggleActive(1L, 1L);

        // 상태 변경 검증
        assertThat(result.isActive()).isFalse();
    }

    // == 삭제 테스트 ==

    @Test
    @DisplayName("아이템 삭제 성공")
    void deleteItem_Success() {
        // 아이템 조회 및 삭제 동작 모킹
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.of(testItem));
        doNothing().when(itemRepository).delete(any(Item.class));

        // 서비스 메서드 호출
        itemService.deleteItem(1L, 1L);

        // Repository의 delete 메서드 호출 여부 검증
        verify(itemRepository, times(1)).delete(testItem);
    }

    @Test
    @DisplayName("아이템 삭제 실패 - 권한 없음")
    void deleteItem_Failure_NoPermission() {
        // 조회 실패 상황 모킹
        given(itemRepository.findByIdAndUserId(1L, 1L))
                .willReturn(Optional.empty());

        // 예외 발생 검증
        assertThatThrownBy(() -> itemService.deleteItem(1L, 1L))
                .isInstanceOf(ServiceException.class);
    }
}
