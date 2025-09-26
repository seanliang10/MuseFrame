package com.museframe.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isAuthenticated(): Flow<Boolean>
    suspend fun verifyToken(token: String): Result<Boolean>
    suspend fun saveAuthToken(token: String)
    suspend fun clearAuthToken()
    suspend fun logout()
}