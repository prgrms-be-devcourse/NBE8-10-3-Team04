package com.back.domain.user.user.entity

import com.back.domain.item.item.entity.Item
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var _id: Long? = null,     //저장 전에는 id가 없어서 null로 지정

    @Column(unique = true)
    var loginId: String,

    var password: String,

    var email: String,

    @Column(unique = true)
    var apiKey: String = UUID.randomUUID().toString(),  //객체가 새로 만들어질때 apiKey가 자동으로 채워짐

    @Column(nullable = false)
    var tokenVersion: Long = 0L,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    val items: MutableList<Item> = mutableListOf()
    //기존 private List<Item> items = new ArrayList<>(); 여서 MutableList로 사용

) {
    val id: Long
        get() = _id ?: error("User is not persisted yet (id is null)")

    protected constructor() : this(
        loginId = "",
        password = "",
        email = ""
    )

    constructor(loginId: String, password: String, email: String) : this(
        loginId = loginId,
        password = password,
        email = email,
        apiKey = UUID.randomUUID().toString()
    )

    fun modifyApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    fun modifyUser(email: String, password: String) {
        this.email = email
        this.password = password
    }

    fun increaseTokenVersion() {
        this.tokenVersion++
    }
}
