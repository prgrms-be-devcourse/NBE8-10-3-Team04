package com.back.domain.item.item.util

import com.back.domain.item.item.util.DDayCalculator.calculate
import org.assertj.core.api.Assertions.assertThat // 변경사항: assertThat을 static import하여 코드를 간결하게 만듦
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DDayCalculatorTest { // 불필요한 internal 접근 제어자 제거

    @Test
    // @DisplayName 애노테이션 대신 백틱(`)을 사용해 한글 메서드명으로 직관적인 테스트명 작성
    fun `정상적인 날짜가 주어지면 D-Day를 계산하여 반환한다`() {
        val nextDate = LocalDate.now().plusDays(5)

        val result = calculate(nextDate)

        assertThat(result).isEqualTo(5L)
    }

    @Test
    fun `다음 교체 예정일이 null인 경우 -1을 반환한다`() {
        val result = calculate(null)

        assertThat(result).isEqualTo(-1L)
    }
}