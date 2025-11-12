package br.edu.fatecpg.appestacionamento.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.fatecpg.appestacionamento.model.User
import br.edu.fatecpg.appestacionamento.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // LiveData para o status da autenticação
    private val _authResult = MutableLiveData<Result<FirebaseUser?>>()
    val authResult: LiveData<Result<FirebaseUser?>> = _authResult

    // LiveData para os dados do usuário do Firestore
    private val _userData = MutableLiveData<Result<User?>>()
    val userData: LiveData<Result<User?>> = _userData

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                _authResult.value = Result.success(result.getOrNull()?.user)
            } else {
                _authResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun register(email: String, pass: String, userType: String) {
        viewModelScope.launch {
            val result = repository.register(email, pass, userType)
            if (result.isSuccess) {
                // Se o registro foi sucesso, já loga o usuário
                _authResult.value = Result.success(repository.getCurrentUser())
            } else {
                _authResult.value = Result.failure(result.exceptionOrNull()!!)
            }
        }
    }

    fun fetchUserData(uid: String) {
        viewModelScope.launch {
            val result = repository.getUserData(uid)
            _userData.value = result
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return repository.getCurrentUser()
    }

    fun logout() {
        repository.logout()
    }
}

// Factory para criar o AuthViewModel com o repositório
class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}