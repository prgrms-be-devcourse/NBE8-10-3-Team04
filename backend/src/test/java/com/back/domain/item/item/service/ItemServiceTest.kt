package com.back.domain.item.item.service

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.item.item.dto.ItemCreateRequest
import com.back.domain.item.item.dto.ItemUpdateRequest
import com.back.domain.item.item.entity.Item
import com.back.domain.item.item.repository.ItemRepository
import com.back.domain.item.itemHistory.repository.ItemHistoryRepository
import com.back.domain.item.itemHistory.service.ItemHistoryService
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.service.UserService
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.s3.S3ImageService
import com.google.genai.Client
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import java.io.IOException
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("ItemService 테스트")
internal class ItemServiceTest {

    @Mock private lateinit var userService: UserService
    @Mock private lateinit var itemHistoryService: ItemHistoryService
    @Mock private lateinit var itemRepository: ItemRepository
    @Mock private lateinit var categoryRepository: CategoryRepository
    @Mock private lateinit var s3ImageService: S3ImageService
    @Mock private lateinit var itemHistoryRepository: ItemHistoryRepository
    @Mock private lateinit var genAiClient: Client
    @Mock private lateinit var objectMapper: ObjectMapper

    @InjectMocks private lateinit var itemService: ItemService

    private lateinit var testUser: User
    private lateinit var testCategory: Category
    private lateinit var testItem: Item
    private lateinit var createRequest: ItemCreateRequest
    private lateinit var updateRequest: ItemUpdateRequest

    @BeforeEach
    fun setUp() {
        testUser = mock(User::class.java).apply {
            lenient().`when`(id).thenReturn(1L)
            lenient().`when`(email).thenReturn("test@example.com")
            lenient().`when`(password).thenReturn("password123")
        }

        testCategory = Category("생활용품")

        testItem = Item(
            testUser,
            testCategory,
            "칫솔",
            "/images/toothbrush.png",
            LocalDate.of(2024, 1, 1),
            "90d",
            LocalDate.of(2024, 4, 1),
            true
        )

        createRequest = ItemCreateRequest(
            1L,
            "칫솔",
            "/images/toothbrush.png",
            null,
            LocalDate.of(2024, 1, 1),
            "90d"
        )

        updateRequest = ItemUpdateRequest(
            1L,
            "전동칫솔",
            "/images/electric_toothbrush.png",
            null,
            "120d",
            true
        )
    }

    // == 조회 테스트 ==
    @Test
    @DisplayName("아이템 ID로 조회 성공")
    fun findById_Success() {
        given(itemRepository.findById(1L)).willReturn(Optional.of(testItem))

        val result = itemService.findById(1L)

        assertThat(result).isPresent
        assertThat(result.get().name).isEqualTo("칫솔")
        verify(itemRepository, times(1)).findById(1L)
    }

    @Test
    @DisplayName("사용자 ID로 아이템 목록 조회 성공")
    fun findAllByUserIdOrderByNextReplacementDateAsc_Success() {
        val items = mutableListOf(testItem)
        given(itemRepository.findAllByUserIdOrderByNextReplacementDateAsc(1L)).willReturn(items)

        val result = itemService.findAllByUserIdOrderByNextReplacementDateAsc(1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("칫솔")
        verify(itemRepository, times(1)).findAllByUserIdOrderByNextReplacementDateAsc(1L)
    }

    @Test
    @DisplayName("아이템 ID와 사용자 ID로 조회 성공")
    fun findByIdAndUserId_Success() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testItem))

        val result = itemService.findByIdAndUserId(1L, 1L)

        assertThat(result.name).isEqualTo("칫솔")
        verify(itemRepository, times(1)).findByIdAndUserId(1L, 1L)
    }

    @Test
    @DisplayName("아이템 ID와 사용자 ID로 조회 실패 - 권한 없음")
    fun findByIdAndUserId_Failure_NoPermission() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.empty())

        assertThatThrownBy { itemService.findByIdAndUserId(1L, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.ITEM_NOT_FOUND_OR_NO_PERMISSION.message)
    }

    @Test
    @DisplayName("카테고리별 아이템 목록 조회 성공")
    fun findAllByUserIdAndCategoryId_Success() {
        given(categoryRepository.existsById(1L)).willReturn(true)
        given(itemRepository.findAllByUserIdAndCategoryId(1L, 1L)).willReturn(mutableListOf(testItem))

        val result = itemService.findAllByUserIdAndCategoryId(1L, 1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("칫솔")
    }

    @Test
    @DisplayName("카테고리별 아이템 목록 조회 실패 - 카테고리 없음")
    fun findAllByUserIdAndCategoryId_Failure_CategoryNotFound() {
        given(categoryRepository.existsById(1L)).willReturn(false)

        assertThatThrownBy { itemService.findAllByUserIdAndCategoryId(1L, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.CATEGORY_NOT_FOUND.message)
    }

    // == 생성 테스트 ==
    @Test
    @DisplayName("아이템 생성 성공")
    fun createItem_Success() {
        given(userService.findById(1L)).willReturn(testUser)
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory))

        // safeAny(Item::class.java) 대신 mockito-kotlin의 any() 적용
        given(itemRepository.save(any())).willReturn(testItem)
        doNothing().`when`(itemHistoryService).createItemHistory(any())

        val result = itemService.createItem(1L, createRequest)

        assertThat(result.name).isEqualTo("칫솔")
        // verify 검증 시에도 mockito-kotlin의 any() 적용
        verify(itemRepository, times(1)).save(any())
        verify(itemHistoryService, times(1)).createItemHistory(any())
    }

    @Test
    @DisplayName("아이템 생성 실패 - 사용자 없음")
    fun createItem_Failure_UserNotFound() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory))
        given(userService.findById(1L)).willReturn(null)

        assertThatThrownBy { itemService.createItem(1L, createRequest) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.message)
    }

    @Test
    @DisplayName("아이템 생성 실패 - 카테고리 없음")
    fun createItem_Failure_CategoryNotFound() {
        given(categoryRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy { itemService.createItem(1L, createRequest) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.CATEGORY_NOT_FOUND.message)
    }

    @Test
    @DisplayName("아이템 생성 성공 - 이미지 업로드")
    @Throws(IOException::class)
    fun createItem_Success_WithImageUpload() {
        val mockFile = mock(MultipartFile::class.java)
        given(mockFile.isEmpty).willReturn(false)

        val requestWithImage = ItemCreateRequest(
            1L, "칫솔", null, mockFile, LocalDate.of(2024, 1, 1), "90d"
        )

        given(userService.findById(1L)).willReturn(testUser)
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory))
        given(s3ImageService.upload(mockFile)).willReturn("https://s3.amazonaws.com/uploaded-image.png")

        // safeAny(Item::class.java) 대신 mockito-kotlin의 any() 적용
        given(itemRepository.save(any())).willReturn(testItem)

        itemService.createItem(1L, requestWithImage)

        verify(s3ImageService, times(1)).upload(mockFile)
        // verify 검증 시에도 mockito-kotlin의 any() 적용
        verify(itemRepository, times(1)).save(any())
    }

    // == 수정 테스트 ==
    @Test
    @DisplayName("아이템 수정 성공")
    fun modify_Success() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testItem))
        given(categoryRepository.findById(1L)).willReturn(Optional.of(testCategory))
        doNothing().`when`(s3ImageService).delete(anyString())

        val result = itemService.modify(1L, 1L, updateRequest)

        assertThat(result.name).isEqualTo("전동칫솔")
        assertThat(result.cycleDays).isEqualTo("120d")
    }

    @Test
    @DisplayName("아이템 수정 실패 - 권한 없음")
    fun modify_Failure_NoPermission() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.empty())

        assertThatThrownBy { itemService.modify(1L, 1L, updateRequest) }
            .isInstanceOf(ServiceException::class.java)
    }

    // == 교체 테스트 ==
    @Test
    @DisplayName("아이템 교체 성공")
    fun replaceItem_Success() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testItem))

        // safeAny를 사용했던 부분들을 모두 mockito-kotlin의 any()로 변경
        doNothing().`when`(itemHistoryService).endHistory(anyLong(), any())
        doNothing().`when`(itemHistoryService).createItemHistory(any())

        val result = itemService.replaceItem(1L, 1L)

        assertThat(result.startDate).isEqualTo(LocalDate.now())
        // verify 검증 시에도 mockito-kotlin의 any() 적용
        verify(itemHistoryService, times(1)).endHistory(anyLong(), any())
        verify(itemHistoryService, times(1)).createItemHistory(any())
    }

    @Test
    @DisplayName("아이템 교체 실패 - 비활성 아이템")
    fun replaceItem_Failure_InactiveItem() {
        val inactiveItem = Item(
            testUser, testCategory, "칫솔", "/images/toothbrush.png",
            LocalDate.of(2024, 1, 1), "90d", LocalDate.of(2024, 4, 1), false
        )

        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(inactiveItem))

        assertThatThrownBy { itemService.replaceItem(1L, 1L) }
            .isInstanceOf(ServiceException::class.java)
            .hasMessageContaining(ErrorCode.INACTIVE_ITEM_CANNOT_REPLACE.message)
    }

    // == 활성화 토글 테스트 ==
    @Test
    @DisplayName("아이템 활성화 토글 성공")
    fun toggleActive_Success() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testItem))

        val result = itemService.toggleActive(1L, 1L)

        assertThat(result.isActive).isFalse()
    }

    // == 삭제 테스트 ==
    @Test
    @DisplayName("아이템 삭제 성공")
    fun deleteItem_Success() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testItem))

        // safeAny(Item::class.java) 대신 mockito-kotlin의 any() 적용
        doNothing().`when`(itemRepository).delete(any())

        itemService.deleteItem(1L, 1L)

        verify(itemRepository, times(1)).delete(testItem)
    }

    @Test
    @DisplayName("아이템 삭제 실패 - 권한 없음")
    fun deleteItem_Failure_NoPermission() {
        given(itemRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.empty())

        assertThatThrownBy { itemService.deleteItem(1L, 1L) }
            .isInstanceOf(ServiceException::class.java)
    }
}