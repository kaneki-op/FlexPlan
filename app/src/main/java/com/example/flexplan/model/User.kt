package com.example.flexplan.model

data class User(
    val id: Int? = null,
    val name: String,
    val email: String,
    val password: String,
    val age: Int,
    val createdAt: String? = null
)
