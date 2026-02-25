package com.back.domain.user.user.service;

import com.back.domain.item.item.entity.Item;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.repository.UserRepository;
import com.back.global.exception.ErrorCode;
import com.back.global.exception.ServiceException;
import com.back.global.s3.S3ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전 상태를 초기화하여 독립적인 테스트 환경 보장
        testUser = new User("testUser", "encodedPassword", "test@example.com");
    }

    @Test
    @DisplayName("count() - 전체 사용자 수를 반환")
    void count() {
        long expectedCount = 10L;
        // Repository 동작 모킹: 사용자 수 요청 시 10 반환 설정
        given(userRepository.count()).willReturn(expectedCount);

        long actualCount = userService.count();

        // 서비스가 반환한 값이 Repository의 반환값과 일치하는지 확인
        assertThat(actualCount).isEqualTo(expectedCount);
        verify(userRepository).count();
    }

    @Test
    @DisplayName("join() - 정상적으로 회원가입 (Captor로 저장값 검증)")
    void join_Success() {
        String loginId = "newUser";
        String rawPassword = "password123";
        String email = "new@example.com";
        String encodedPassword = "encodedPassword123";

        // 중복 ID가 없고, 비밀번호 암호화가 정상 수행되도록 설정
        given(userRepository.findByLoginId(loginId)).willReturn(null);
        given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
        // save 호출 시 전달된 객체를 그대로 반환하도록 설정 (메서드 체이닝 등 대비)
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.join(loginId, rawPassword, email);

        // ArgumentCaptor를 사용하여 Repository.save() 메서드에 실제로 전달된 User 객체를 가로챔
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User capturedUser = captor.getValue();

        // 가로챈 객체 내부에 암호화된 비밀번호가 올바르게 설정되었는지 정밀 검증
        assertThat(capturedUser.getLoginId()).isEqualTo(loginId);
        assertThat(capturedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(capturedUser.getEmail()).isEqualTo(email);

        // 최종 반환된 객체가 저장된 객체와 동일한지 확인
        assertThat(savedUser).isEqualTo(capturedUser);
    }

    @Test
    @DisplayName("join() - 중복된 아이디로 가입 시 예외가 발생")
    void join_DuplicateLoginId_ThrowsException() {
        String loginId = "duplicateUser";
        String password = "password123";
        String email = "duplicate@example.com";

        // 이미 가입된 사용자가 존재하는 상황 설정
        given(userRepository.findByLoginId(loginId)).willReturn(testUser);

        // 예외 발생 및 에러 코드 검증
        assertThatThrownBy(() -> userService.join(loginId, password, email))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_LOGIN_ID);

        // 예외 발생 시 암호화나 DB 저장이 실행되지 않았음을 보장
        verify(userRepository).findByLoginId(loginId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("findByLoginId() - 로그인 아이디로 사용자를 찾는다")
    void findByLoginId() {
        String loginId = "testUser";
        given(userRepository.findByLoginId(loginId)).willReturn(testUser);

        User foundUser = userService.findByLoginId(loginId);

        // 반환된 Optional 객체 내 데이터 검증
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getLoginId()).isEqualTo(loginId);
        verify(userRepository).findByLoginId(loginId);
    }

    @Test
    @DisplayName("deleteById() - 사용자와 관련 이미지를 삭제 (Captor로 필터링 검증)")
    void deleteById_WithImages() {
        Long userId = 1L;

        // 다양한 상태(정상, null, 빈 문자열)를 가진 이미지 Mock 객체 생성
        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);
        Item item3 = mock(Item.class);
        Item item4 = mock(Item.class);

        given(item1.getImgUrl()).willReturn("https://s3.amazonaws.com/bucket/image1.jpg");
        given(item2.getImgUrl()).willReturn("https://s3.amazonaws.com/bucket/image2.jpg");
        given(item3.getImgUrl()).willReturn(null); // 삭제 대상에서 제외되어야 함
        given(item4.getImgUrl()).willReturn("");   // 삭제 대상에서 제외되어야 함

        List<Item> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);
        items.add(item4);

        // User 객체의 getItems() 동작을 제어하기 위해 Spy 사용
        User spyUser = spy(testUser);
        given(spyUser.getItems()).willReturn(items);
        given(userRepository.findById(userId)).willReturn(Optional.of(spyUser));

        userService.deleteById(userId);

        verify(userRepository).findById(userId);

        // S3 서비스 삭제 메서드에 전달된 URL 리스트를 ArgumentCaptor로 포착
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(s3ImageService).deleteMultiple(captor.capture());

        List<String> capturedUrls = captor.getValue();

        // 로직 내부에서 null과 빈 문자열이 필터링되고 유효한 URL 2개만 전달되었는지 검증
        assertThat(capturedUrls)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        "https://s3.amazonaws.com/bucket/image1.jpg",
                        "https://s3.amazonaws.com/bucket/image2.jpg"
                );

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("deleteById() - 이미지가 없는 사용자를 삭제")
    void deleteById_WithoutImages() {
        Long userId = 1L;
        User userWithoutItems = spy(testUser);

        // 아이템이 없는 빈 리스트 반환 설정
        given(userWithoutItems.getItems()).willReturn(new ArrayList<>());
        given(userRepository.findById(userId)).willReturn(Optional.of(userWithoutItems));

        userService.deleteById(userId);

        verify(userRepository).findById(userId);
        // 이미지가 없으므로 S3 삭제 로직은 호출되지 않아야 함
        verify(s3ImageService, never()).deleteMultiple(anyList());
        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("deleteById() - 존재하지 않는 사용자 삭제 시 예외가 발생")
    void deleteById_UserNotFound_ThrowsException() {
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteById(userId))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        // 예외 발생 시 후속 삭제 작업들이 실행되지 않아야 함
        verify(userRepository).findById(userId);
        verify(s3ImageService, never()).deleteMultiple(anyList());
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("checkPassword() - 올바른 비밀번호를 검증")
    void checkPassword_Success() {
        String rawPassword = "correctPassword";
        // 비밀번호 인코더가 일치한다고 응답하도록 설정
        given(passwordEncoder.matches(rawPassword, testUser.getPassword())).willReturn(true);

        userService.checkPassword(testUser, rawPassword);

        verify(passwordEncoder).matches(rawPassword, testUser.getPassword());
    }

    @Test
    @DisplayName("checkPassword() - 잘못된 비밀번호 시 예외가 발생")
    void checkPassword_WrongPassword_ThrowsException() {
        String wrongPassword = "wrongPassword";
        // 비밀번호 인코더가 불일치한다고 응답하도록 설정
        given(passwordEncoder.matches(wrongPassword, testUser.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.checkPassword(testUser, wrongPassword))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);

        verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
    }

    @Test
    @DisplayName("findByApiKey() - API 키로 사용자를 찾는다")
    void findByApiKey() {
        String apiKey = "test-api-key";
        given(userRepository.findByApiKey(apiKey)).willReturn(testUser);

        User foundUser = userService.findByApiKey(apiKey);

        assertThat(foundUser).isNotNull();
        verify(userRepository).findByApiKey(apiKey);
    }

    @Test
    @DisplayName("genAccessToken() - 액세스 토큰을 생성")
    void genAccessToken() {
        String expectedToken = "generated-access-token";
        // 토큰 서비스 동작 모킹
        given(authTokenService.genAccessToken(testUser)).willReturn(expectedToken);

        String actualToken = userService.genAccessToken(testUser);

        assertThat(actualToken).isEqualTo(expectedToken);
        verify(authTokenService).genAccessToken(testUser);
    }

    @Test
    @DisplayName("findById() - ID로 사용자를 찾는다")
    void findById() {
        //repository는 Optional이고 Service는 nullable에 맞춰서 변경
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        User foundUser = userService.findById(userId);

        assertThat(foundUser).isNotNull();
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("updateProfile() - 프로필을 수정하고 토큰을 무효화 (상태 검증)")
    void updateProfile_Success() {
        Long userId = 1L;
        String newEmail = "updated@example.com";

        // Spy를 사용하지 않고 실제 User 객체를 반환하여 상태 변경을 직접 검증
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        User updatedUser = userService.updateProfile(userId, newEmail);

        verify(userRepository).findById(userId);

        // 메서드 호출 여부(verify) 대신 실제 객체의 필드 값이 변경되었는지(state) 확인
        assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
        assertThat(updatedUser.getTokenVersion()).isGreaterThan(0L); // 토큰 버전 증가 확인
        assertThat(updatedUser.getApiKey()).isNotNull(); // API 키 재생성 확인
    }

    @Test
    @DisplayName("updateProfile() - 존재하지 않는 사용자 수정 시 예외가 발생")
    void updateProfile_UserNotFound_ThrowsException() {
        Long userId = 999L;
        String newEmail = "updated@example.com";
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(userId, newEmail))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("changePassword() - 비밀번호를 변경하고 토큰을 무효화 (행위 및 상태 검증)")
    void changePassword_Success() {
        Long userId = 1L;
        String currentPassword = "currentPassword";
        String newPassword = "newPassword123";
        String encodedNewPassword = "encodedNewPassword123";

        // 검증을 위해 변경 전 비밀번호 상태 저장
        String oldEncodedPassword = testUser.getPassword();

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // 암호화 관련 로직 모킹: 현재 비밀번호 일치 확인 -> 새 비밀번호 중복 확인 -> 새 비밀번호 암호화
        given(passwordEncoder.matches(currentPassword, oldEncodedPassword)).willReturn(true);
        given(passwordEncoder.matches(newPassword, oldEncodedPassword)).willReturn(false);
        given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

        User updatedUser = userService.changePassword(userId, currentPassword, newPassword);

        verify(userRepository).findById(userId);

        // 1. 행위 검증: 서비스가 올바른 인자로 암호화 관련 메서드들을 호출했는지 확인
        verify(passwordEncoder).matches(currentPassword, oldEncodedPassword);
        verify(passwordEncoder).matches(newPassword, oldEncodedPassword);
        verify(passwordEncoder).encode(newPassword);

        // 2. 상태 검증: 결과적으로 객체의 비밀번호가 변경되고 부수 효과(토큰 버전 등)가 발생했는지 확인
        assertThat(updatedUser.getPassword()).isEqualTo(encodedNewPassword);
        assertThat(updatedUser.getTokenVersion()).isGreaterThan(0L);
        assertThat(updatedUser.getApiKey()).isNotNull();
    }

    @Test
    @DisplayName("changePassword() - 존재하지 않는 사용자의 비밀번호 변경 시 예외가 발생")
    void changePassword_UserNotFound_ThrowsException() {
        Long userId = 999L;
        String currentPassword = "currentPassword";
        String newPassword = "newPassword123";

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(userId, currentPassword, newPassword))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword() - 현재 비밀번호가 틀리면 예외가 발생")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        Long userId = 1L;
        String wrongCurrentPassword = "wrongPassword";
        String newPassword = "newPassword123";

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(wrongCurrentPassword, testUser.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(userId, wrongCurrentPassword, newPassword))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(wrongCurrentPassword, testUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("changePassword() - 새 비밀번호가 현재 비밀번호와 같으면 예외가 발생")
    void changePassword_SamePassword_ThrowsException() {
        Long userId = 1L;
        String currentPassword = "currentPassword";
        String samePassword = "currentPassword"; // 입력값과 기존 값이 같음

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);

        assertThatThrownBy(() -> userService.changePassword(userId, currentPassword, samePassword))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_PASSWORD);

        verify(userRepository).findById(userId);

        // 동일한 인자(현재비번, DB비번)로 두 번 호출됨 (1.현재 비밀번호 검증 2.새 비밀번호 중복 검증) -> times(2) 확인 필요
        verify(passwordEncoder, times(2)).matches(currentPassword, testUser.getPassword());

        verify(passwordEncoder, never()).encode(anyString());
    }
}