package com.back.domain.category.category.entity

import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String
) {
    constructor() : this(null, "")
    // 자바 테스트 코드에서 new Category("이름")으로 쓰던 관례를 유지하기 위함
    // @JvmOverloads를 붙이면 자바에서 인자 1개짜리 생성자도 인식합니다.
    constructor(name: String) : this(null, name)
}
