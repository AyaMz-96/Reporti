package com.projet.signalement.model

import jakarta.persistence.*

@Entity
@Table(name = "sectors")
class Sector(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var zone: String
)