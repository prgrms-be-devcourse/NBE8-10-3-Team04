package com.back.domain.email.scheduler;

import com.back.domain.category.category.entity.Category;
import com.back.domain.email.service.EmailService;
import com.back.domain.item.item.entity.Item;
import com.back.domain.item.item.repository.ItemRepository;
import com.back.domain.user.user.entity.User;
import com.back.domain.user.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DdayEmailScheduler 테스트")
class DdayEmailSchedulerTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DdayEmailScheduler ddayEmailScheduler;

    @Test
    @DisplayName("오늘 교체 대상 아이템이 없으면 이메일을 발송하지 않는다")
    void checkAndSendDdayEmails_noItemsDueToday() {
        given(itemRepository.findAllByNextReplacementDateAndIsActive(any(LocalDate.class), eq(true)))
                .willReturn(List.of());

        ddayEmailScheduler.checkAndSendDdayEmails();

        verify(itemRepository, times(1))
                .findAllByNextReplacementDateAndIsActive(any(LocalDate.class), eq(true));
        verify(emailService, never()).sendDDayNotification(any(), any());
    }

    @Test
    @DisplayName("오늘 교체 대상 아이템이 있으면 사용자 이메일로 발송한다")
    void checkAndSendDdayEmails_sendNotifications() {
        User user1 = User.builder()
                .id(1L)
                .loginId("user1")
                .email("user1@test.com")
                .build();

        User user2 = User.builder()
                .id(2L)
                .loginId("user2")
                .email("user2@test.com")
                .build();

        Category category = new Category("욕실");

        Item item1 = Item.builder()
                .id(1L)
                .user(user1)
                .category(category)
                .name("칫솔")
                .startDate(LocalDate.now().minusDays(90))
                .cycleDays("90d")
                .nextReplacementDate(LocalDate.now())
                .isActive(true)
                .build();

        Item item2 = Item.builder()
                .id(2L)
                .user(user2)
                .category(category)
                .name("수세미")
                .startDate(LocalDate.now().minusDays(30))
                .cycleDays("30d")
                .nextReplacementDate(LocalDate.now())
                .isActive(true)
                .build();

        given(itemRepository.findAllByNextReplacementDateAndIsActive(any(LocalDate.class), eq(true)))
                .willReturn(List.of(item1, item2));

        ddayEmailScheduler.checkAndSendDdayEmails();

        verify(itemRepository, times(1))
                .findAllByNextReplacementDateAndIsActive(any(LocalDate.class), eq(true));
        verify(emailService, times(1)).sendDDayNotification(user1.getEmail(), item1);
        verify(emailService, times(1)).sendDDayNotification(user2.getEmail(), item2);
    }
}
