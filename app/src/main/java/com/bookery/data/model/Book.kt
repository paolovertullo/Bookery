package com.bookery.data.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val language: String,
    val coverUrl: String? = null,
    val coverPath: String?= null
)
