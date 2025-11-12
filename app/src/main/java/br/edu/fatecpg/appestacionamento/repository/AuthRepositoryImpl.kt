package br.edu.fatecpg.appestacionamento.repository

import br.edu.fatecpg.appestacionamento.model.User
import br.edu.fatecpg.appestacionamento.utils.Constants
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Implementação concreta do repositório de autenticação
class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun login(email: String, pass: String): Result<AuthResult> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(authResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, pass: String, userType: String): Result<Boolean> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // Salva dados adicionais do usuário no Firestore
                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    userType = userType
                )
                db.collection(Constants.COLLECTION_USERS)
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()
                Result.success(true)
            } else {
                Result.failure(Exception("Falha ao criar usuário."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserData(uid: String): Result<User?> {
        return try {
            val document = db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
    }
}