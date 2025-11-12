package br.edu.fatecpg.appestacionamento.view.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.appestacionamento.Injector
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.databinding.ActivitySplashBinding
import br.edu.fatecpg.appestacionamento.utils.Constants
import br.edu.fatecpg.appestacionamento.viewmodel.AuthViewModel

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authViewModel: AuthViewModel by viewModels {
        Injector.provideAuthViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Delay para mostrar a splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 2000) // 2 segundos
    }

    private fun checkUserStatus() {
        val currentUser = authViewModel.getCurrentUser()
        if (currentUser == null) {
            // Ninguém logado, vai para Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Usuário logado, verifica o tipo (Motorista/Admin)
            authViewModel.fetchUserData(currentUser.uid)
            observeUserData()
        }
    }

    private fun observeUserData() {
        authViewModel.userData.observe(this) { result ->
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user?.userType == Constants.USER_TYPE_ADMIN) {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    // Default é motorista
                    startActivity(Intent(this, DriverDashboardActivity::class.java))
                }
            } else {
                // Se falhar (ex: usuário deletado), vai para Login
                authViewModel.logout() // Garante que saia
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }
    }
}