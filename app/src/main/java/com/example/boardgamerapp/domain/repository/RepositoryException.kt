package com.example.boardgamerapp.domain.repository

class RepositoryException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
