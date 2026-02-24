package com.back.domain.category.category.entity

import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    @Column(nullable = false)
    var name: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
    //id는 외부에서 임의 변경 못 하게 막고
    //JPA(하이버네이트)만 내부적으로 세팅 가능하게 둡니다.

    protected constructor() : this("")
    // JPA는 기본 생성자가 필요하기 때문에, protected로 접근 제한을 둔 기본 생성자를 추가합니다.
}
