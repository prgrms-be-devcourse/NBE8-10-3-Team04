package com.back.global.initData

import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.item.item.dto.ItemCreateRequest
import com.back.domain.item.item.service.ItemService
import com.back.domain.user.user.service.UserService
import com.back.global.app.AppConfig.Companion.isNotProd
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Configuration
// Lombok의 @RequiredArgsConstructor를 제거하고 코틀린의 주 생성자를 활용
// Nullable(?) 타입을 모두 Non-Null 타입으로 변경하여 불필요한 null 체크와 !! 연산자를 제거
class BaseInitData(
    private val categoryRepository: CategoryRepository,
    private val itemService: ItemService,
    private val userService: UserService,
) {
    // self 주입 시 생성자 순환 참조 문제가 발생할 수 있으므로 필드 주입(lateinit)
    @Autowired
    @Lazy
    private lateinit var self: BaseInitData

    @Bean
    fun baseInitDataApplicationRunner(): ApplicationRunner = ApplicationRunner {
        self.createDefaultCategory()
        self.createDefaultUsers()
        self.initItems()
    }

    @Transactional
    fun createDefaultCategory() {
        if (categoryRepository.count() > 0) return

        // 단건 save를 반복하는 대신 Collection API의 map과 Spring Data JPA의 saveAll을 활용
        val categories = listOf("집/생활", "욕실", "주방", "뷰티", "반려동물", "자동차", "전자기기", "업무", "기타")
            .map { Category(it) }

        categoryRepository.saveAll(categories)
    }

    @Transactional
    fun createDefaultUsers() {
        // 자바의 Supplier를 사용하던 복잡한 Optional 처리를 코틀린 람다식(orElseGet { })으로 변경
        userService.findByLoginId("user1")
            ?: userService.join("user1", "1234", "hhyukk1273@gmail.com")

        userService.findByLoginId("user2")
            ?: userService.join("user2", "1234", "user2@test.com")
    }

    @Transactional
    fun initItems() {
        if (itemService.count() > 0) return

        val user1 = userService.findByLoginId("user1")?: error("user1을 찾을 수 없습니다.")
        val user2 = userService.findByLoginId("user2")?: error("user2을 찾을 수 없습니다.")

        // Objects.requireNonNull 대신 코틀린의 엘비스 연산자(?:)와 error() 함수를 사용해 예외를 처리
        val bathroom = categoryRepository.findByName("욕실") ?: error("욕실 카테고리를 찾을 수 없습니다.")
        val kitchen = categoryRepository.findByName("주방") ?: error("주방 카테고리를 찾을 수 없습니다.")
        val car = categoryRepository.findByName("자동차") ?: error("자동차 카테고리를 찾을 수 없습니다.")

        // user1.getId() 같은 Getter 메서드 대신 프로퍼티 접근 구문(user1.id)을 사용
        itemService.createItem(
            user1.persistedId,
            ItemCreateRequest(
                categoryId = bathroom.id!!,
                name = "칫솔",
                imgUrl = "https://example.com/toothbrush.png",
                image = null,
                startDate = LocalDate.of(2026, 1, 1),
                cycleDays = "3m"
            )
        )

        itemService.createItem(
            user1.persistedId,
            ItemCreateRequest(
                categoryId = kitchen.id!!,
                name = "수세미",
                imgUrl = "https://example.com/sponge.png",
                image = null,
                startDate = LocalDate.of(2026, 1, 5),
                cycleDays = "21d"
            )
        )

        itemService.createItem(
            user2.persistedId,
            ItemCreateRequest(
                categoryId = car.id!!,
                name = "엔진오일",
                imgUrl = "https://example.com/engineoil.png",
                image = null,
                startDate = LocalDate.of(2025, 12, 1),
                cycleDays = "6m"
            )
        )

        itemService.createItem(
            user1.persistedId,
            ItemCreateRequest(
                categoryId = bathroom.id!!,
                name = "테스트용 칫솔 (D-Day 0)",
                imgUrl = "https://example.com/test-toothbrush.png",
                image = null,
                startDate = LocalDate.now().minusMonths(3),
                cycleDays = "3m"
            )
        )
    }
}