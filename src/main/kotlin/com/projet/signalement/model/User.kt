package com.projet.signalement.model
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var surname: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "phone_nb")
    var phoneNb: String? = null,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var role: String,

    @Column
    var status: String? = "ACTIVE",

    @Column
    var specialty: String? = null,

    @Column(name = "is_available")
    var isAvailable: Boolean = true,

    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: OffsetDateTime? = null
)