package com.projet.signalement.model

import jakarta.persistence.*

@Entity
@Table(name = "categories")
class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null
)