package com.back.domain.item.item.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class DDayCalculatorTest {
    @Test
    @DisplayName("다음 교체 예정일 계산")
    void calculate_WithValidDate_ReturnsDDay() {
        LocalDate today = LocalDate.now();
        LocalDate nextDate = today.plusDays(5); // 5일 뒤

        Long result = DDayCalculator.calculate(nextDate);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("다음 교체 예정일이 null인 경우 -1을 반환")
    void calculate_WithNullDate_ReturnsMinusOne() {
        Long result = DDayCalculator.calculate(null);

        assertThat(result).isEqualTo(-1L);
    }
}