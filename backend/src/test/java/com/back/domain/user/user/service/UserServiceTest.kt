package com.back.domain.user.user.service

import com.back.domain.item.item.entity.Item
import com.back.domain.user.user.entity.User
import com.back.domain.user.user.repository.UserRepository
import com.back.global.event.S3ImageDeleteEvent
import com.back.global.exception.ErrorCode
import com.back.global.exception.ServiceException
import com.back.global.s3.S3ImageService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("UserService 테스트")
internal class UserServiceTest {
    @Mock
    private lateinit var authTokenService: AuthTokenService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var s3ImageService: S3ImageService

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var userService: UserService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 각 테스트 실행 전 상태를 초기화하여 독립적인 테스트 환경 보장
        testUser = User("testUser", "encodedPassword", "test@example.com")
    }

    @Test
    @DisplayName("count() - 전체 사용자 수를 반환")
    fun count() {
        val expectedCount = 10L
        // Repository 동작 모킹: 사용자 수 요청 시 10 반환 설정
        BDDMockito.given(userRepository.count()).willReturn(expectedCount)

        val actualCount = userService.count()

        // 서비스가 반환한 값이 Repository의 반환값과 일치하는지 확인
        assertThat(actualCount).isEqualTo(expectedCount)
        BDDMockito.then(userRepository).should().count()
    }

    @Test
    @DisplayName("join() - 정상적으로 회원가입 (Captor로 저장값 검증)")
    fun join_Success() {
        val loginId = "newUser"
        val rawPassword = "password123"
        val email = "new@example.com"
        val encodedPassword = "encodedPassword123"

        // 중복 ID가 없고, 비밀번호 암호화가 정상 수행되도록 설정
        BDDMockito.given(userRepository.findByLoginId(loginId)).willReturn(null)
        BDDMockito.given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword)
        // save 호출 시 전달된 객체를 그대로 반환하도록 설정 (메서드 체이닝 등 대비)
        BDDMockito.given(userRepository.save(any<User>())).willAnswer { it.getArgument(0) }

        val savedUser = userService.join(loginId, rawPassword, email)

        // ArgumentCaptor를 사용하여 Repository.save() 메서드에 실제로 전달된 User 객체를 가로챔
        val captor = argumentCaptor<User>()
        BDDMockito.then(userRepository).should().save(captor.capture())

        val capturedUser = captor.firstValue

        // 가로챈 객체 내부에 암호화된 비밀번호가 올바르게 설정되었는지 정밀 검증
        assertThat(capturedUser.loginId).isEqualTo(loginId)
        assertThat(capturedUser.password).isEqualTo(encodedPassword)
        assertThat(capturedUser.email).isEqualTo(email)

        // 최종 반환된 객체가 저장된 객체와 동일한지 확인
        assertThat(savedUser).isEqualTo(capturedUser)
    }

    @Test
    @DisplayName("join() - 중복된 아이디로 가입 시 예외가 발생")
    fun join_DuplicateLoginId_ThrowsException() {
        val loginId = "duplicateUser"
        val password = "password123"
        val email = "duplicate@example.com"

        // 이미 가입된 사용자가 존재하는 상황 설정
        BDDMockito.given(userRepository.findByLoginId(loginId)).willReturn(testUser)

        // 예외 발생 및 에러 코드 검증
        assertThatThrownBy { userService.join(loginId, password, email) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_LOGIN_ID)

        // 예외 발생 시 암호화나 DB 저장이 실행되지 않았음을 보장
        BDDMockito.then(userRepository).should().findByLoginId(loginId)
        BDDMockito.then(passwordEncoder).shouldHaveNoInteractions()
        BDDMockito.then(userRepository).should(BDDMockito.never()).save(any<User>())
    }

    @Test
    @DisplayName("findByLoginId() - 로그인 아이디로 사용자를 찾는다")
    fun findByLoginId() {
        val loginId = "testUser"
        BDDMockito.given(userRepository.findByLoginId(loginId)).willReturn(testUser)

        val foundUser = requireNotNull(userService.findByLoginId(loginId))

        // 반환된 Optional 객체 내 데이터 검증
        assertThat(foundUser.loginId).isEqualTo(loginId)
        BDDMockito.then(userRepository).should().findByLoginId(loginId)
    }

    @Test
    @DisplayName("deleteById() - 사용자와 관련 이미지를 삭제 (Captor로 필터링 검증)")
    fun deleteById_WithImages() {
        val userId = 1L

        // 다양한 상태(정상, null, 빈 문자열)를 가진 이미지 Mock 객체 생성
        val item1 = mock<Item>()
        val item2 = mock<Item>()
        val item3 = mock<Item>()
        val item4 = mock<Item>()

        BDDMockito.given(item1.imgUrl).willReturn("https://s3.amazonaws.com/bucket/image1.jpg")
        BDDMockito.given(item2.imgUrl).willReturn("https://s3.amazonaws.com/bucket/image2.jpg")
        BDDMockito.given(item3.imgUrl).willReturn(null)
        BDDMockito.given(item4.imgUrl).willReturn("")

        // User.items가 Kotlin 프로퍼티면 spy 대신 testUser.items를 직접 세팅 가능할 수도 있음
        val spyUser = BDDMockito.spy(testUser)
        BDDMockito.given(spyUser.items).willReturn(mutableListOf(item1, item2, item3, item4))
        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(spyUser))

        userService.deleteById(userId)

        BDDMockito.then(userRepository).should().findById(userId)
        val expectedEvent = S3ImageDeleteEvent(
            listOf(
                "https://s3.amazonaws.com/bucket/image1.jpg",
                "https://s3.amazonaws.com/bucket/image2.jpg"
            )
        )
        BDDMockito.then(eventPublisher).should().publishEvent(expectedEvent)
        BDDMockito.then(userRepository).should().deleteById(userId)
    }

    @Test
    @DisplayName("deleteById() - 이미지가 없는 사용자를 삭제")
    fun deleteById_WithoutImages() {
        val userId = 1L
        val spyUser = BDDMockito.spy(testUser)

        // 아이템이 없는 빈 리스트 반환 설정
        BDDMockito.given(spyUser.items).willReturn(mutableListOf())
        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(spyUser))

        userService.deleteById(userId)

        BDDMockito.then(userRepository).should().findById(userId)
        // 이미지가 없으므로 S3 삭제 로직은 호출되지 않아야 함
        BDDMockito.then(eventPublisher).should(BDDMockito.never()).publishEvent(any<Any>())
        BDDMockito.then(userRepository).should().deleteById(userId)

    }

    @Test
    @DisplayName("deleteById() - 존재하지 않는 사용자 삭제 시 예외가 발생")
    fun deleteById_UserNotFound_ThrowsException() {
        val userId = 999L
        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.empty())

        assertThatThrownBy { userService.deleteById(userId) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)

        // 예외 발생 시 후속 삭제 작업들이 실행되지 않아야 함
        BDDMockito.then(userRepository).should().findById(userId)
        BDDMockito.then(s3ImageService).should(BDDMockito.never()).deleteMultiple(any())
        BDDMockito.then(userRepository).should(BDDMockito.never()).deleteById(any())
    }

    @Test
    @DisplayName("checkPassword() - 올바른 비밀번호를 검증")
    fun checkPassword_Success() {
        val rawPassword = "correctPassword"
        // 비밀번호 인코더가 일치한다고 응답하도록 설정
        BDDMockito.given(passwordEncoder.matches(rawPassword, testUser.password)).willReturn(true)

        userService.checkPassword(testUser, rawPassword)

        BDDMockito.then(passwordEncoder).should().matches(rawPassword, testUser.password)
    }

    @Test
    @DisplayName("checkPassword() - 잘못된 비밀번호 시 예외가 발생")
    fun checkPassword_WrongPassword_ThrowsException() {
        val wrongPassword = "wrongPassword"
        // 비밀번호 인코더가 불일치한다고 응답하도록 설정
        BDDMockito.given(passwordEncoder.matches(wrongPassword, testUser.password)).willReturn(false)

        assertThatThrownBy { userService.checkPassword(testUser, wrongPassword) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD)

        BDDMockito.then(passwordEncoder).should().matches(wrongPassword, testUser.password)
    }

    @Test
    @DisplayName("findByApiKey() - API 키로 사용자를 찾는다")
    fun findByApiKey() {
        val apiKey = "test-api-key"
        BDDMockito.given(userRepository.findByApiKey(apiKey)).willReturn(testUser)

        val foundUser = requireNotNull(userService.findByApiKey(apiKey))

        assertThat(foundUser.loginId).isEqualTo(testUser.loginId)
        BDDMockito.then(userRepository).should().findByApiKey(apiKey)
    }

    @Test
    @DisplayName("genAccessToken() - 액세스 토큰을 생성")
    fun genAccessToken() {
        val expectedToken = "generated-access-token"
        // 토큰 서비스 동작 모킹
        BDDMockito.given(authTokenService.genAccessToken(testUser)).willReturn(expectedToken)

        val actualToken = userService.genAccessToken(testUser)

        assertThat(actualToken).isEqualTo(expectedToken)
        BDDMockito.then(authTokenService).should().genAccessToken(testUser)
    }

    @Test
    @DisplayName("findById() - ID로 사용자를 찾는다")
    fun findById() {
        //repository는 Optional이고 Service는 nullable에 맞춰서 변경
        val userId = 1L
        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

        val foundUser = userService.findById(userId)

        assertThat(foundUser).isNotNull
        BDDMockito.then(userRepository).should().findById(userId)

    }

    @Test
    @DisplayName("updateProfile() - 프로필을 수정하고 토큰을 무효화 (상태 검증)")
    fun updateProfile_Success() {
        val userId = 1L
        val newEmail = "updated@example.com"

        // Spy를 사용하지 않고 실제 User 객체를 반환하여 상태 변경을 직접 검증
        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

        val updatedUser = userService.updateProfile(userId, newEmail)

        BDDMockito.then(userRepository).should().findById(userId)

        // 메서드 호출 여부(verify) 대신 실제 객체의 필드 값이 변경되었는지(state) 확인
        assertThat(updatedUser.email).isEqualTo(newEmail)
        assertThat(updatedUser.tokenVersion).isGreaterThan(0L)
        assertThat(updatedUser.apiKey).isNotNull
    }

    @Test
    @DisplayName("updateProfile() - 존재하지 않는 사용자 수정 시 예외가 발생")
    fun updateProfile_UserNotFound_ThrowsException() {
        val userId = 999L
        val newEmail = "updated@example.com"
        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.empty())

        assertThatThrownBy{ userService.updateProfile(userId, newEmail) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)

        BDDMockito.then(userRepository).should().findById(userId)
    }

    @Test
    @DisplayName("changePassword() - 비밀번호를 변경하고 토큰을 무효화 (행위 및 상태 검증)")
    fun changePassword_Success() {
        val userId = 1L
        val currentPassword = "currentPassword"
        val newPassword = "newPassword123"
        val encodedNewPassword = "encodedNewPassword123"

        // 검증을 위해 변경 전 비밀번호 상태 저장
        val oldEncodedPassword = testUser.password

        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(testUser))

        // 암호화 관련 로직 모킹: 현재 비밀번호 일치 확인 -> 새 비밀번호 중복 확인 -> 새 비밀번호 암호화
        BDDMockito.given(passwordEncoder.matches(currentPassword, oldEncodedPassword)).willReturn(true)
        BDDMockito.given(passwordEncoder.matches(newPassword, oldEncodedPassword)).willReturn(false)
        BDDMockito.given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword)

        val updatedUser = userService.changePassword(userId, currentPassword, newPassword)

        BDDMockito.then(userRepository).should().findById(userId)

        // 1. 행위 검증: 서비스가 올바른 인자로 암호화 관련 메서드들을 호출했는지 확인
        BDDMockito.then(passwordEncoder).should().matches(currentPassword, oldEncodedPassword)
        BDDMockito.then(passwordEncoder).should().matches(newPassword, oldEncodedPassword)
        BDDMockito.then(passwordEncoder).should().encode(newPassword)

        // 2. 상태 검증: 결과적으로 객체의 비밀번호가 변경되고 부수 효과(토큰 버전 등)가 발생했는지 확인
        assertThat(updatedUser.password).isEqualTo(encodedNewPassword)
        assertThat(updatedUser.tokenVersion).isGreaterThan(0L)
        assertThat(updatedUser.apiKey).isNotNull
    }

    @Test
    @DisplayName("changePassword() - 존재하지 않는 사용자의 비밀번호 변경 시 예외가 발생")
    fun changePassword_UserNotFound_ThrowsException() {
        val userId = 999L
        val currentPassword = "currentPassword"
        val newPassword = "newPassword123"

        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.empty())

        assertThatThrownBy { userService.changePassword(userId, currentPassword, newPassword) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)

        BDDMockito.then(userRepository).should().findById(userId)
        BDDMockito.then(passwordEncoder).should(BDDMockito.never()).matches(any(), any())
    }

    @Test
    @DisplayName("changePassword() - 현재 비밀번호가 틀리면 예외가 발생")
    fun changePassword_WrongCurrentPassword_ThrowsException() {
        val userId = 1L
        val wrongCurrentPassword = "wrongPassword"
        val newPassword = "newPassword123"

        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
        BDDMockito.given(passwordEncoder.matches(wrongCurrentPassword, testUser.password)).willReturn(false)

        assertThatThrownBy { userService.changePassword(userId, wrongCurrentPassword, newPassword) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH)

        BDDMockito.then(userRepository).should().findById(userId)
        BDDMockito.then(passwordEncoder).should()
            .matches(wrongCurrentPassword, testUser.password)
        BDDMockito.then(passwordEncoder).should(BDDMockito.never()).encode(any())
    }

    @Test
    @DisplayName("changePassword() - 새 비밀번호가 현재 비밀번호와 같으면 예외가 발생")
    fun changePassword_SamePassword_ThrowsException() {
        val userId = 1L
        val currentPassword = "currentPassword"
        val samePassword = "currentPassword" // 입력값과 기존 값이 같음

        BDDMockito.given(userRepository.findById(userId)).willReturn(Optional.of(testUser))
        BDDMockito.given(passwordEncoder.matches(currentPassword, testUser.password)).willReturn(true)

        assertThatThrownBy { userService.changePassword(userId, currentPassword, samePassword) }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_PASSWORD)

        BDDMockito.then(userRepository).should().findById(userId)

        // 동일한 인자(현재비번, DB비번)로 두 번 호출됨 (1.현재 비밀번호 검증 2.새 비밀번호 중복 검증) -> times(2) 확인 필요
        BDDMockito.then(passwordEncoder).should(BDDMockito.times(2)).matches(currentPassword, testUser.password)
        BDDMockito.then(passwordEncoder).should(BDDMockito.never()).encode(any())
    }
}