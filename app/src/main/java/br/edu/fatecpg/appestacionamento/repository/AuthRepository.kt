package br.edu.fatecpg.appestacionamento.repository

import br.edu.fatecpg.appestacionamento.model.User
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): FirebaseUser?
    suspend fun login(email: String, pass: String): Result<AuthResult>
    suspend fun register(email: String, pass: String, userType: String): Result<Boolean>
    suspend fun getUserData(uid: String): Result<User?>
    fun logout()
}