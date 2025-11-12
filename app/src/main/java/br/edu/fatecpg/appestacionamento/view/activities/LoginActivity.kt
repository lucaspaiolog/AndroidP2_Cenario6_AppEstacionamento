package br.edu.fatecpg.appestacionamento.view.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.appestacionamento.Injector
import br.edu.fatecpg.appestacionamento.databinding.ActivityLoginBinding
import br.edu.fatecpg.appestacionamento.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels {
        Injector.provideAuthViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        authViewModel.login(email, password)
    }

    private fun setupObservers() {
        authViewModel.authResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true

            if (result.isSuccess) {
                // Sucesso no login, busca dados do usuário para redirecionar
                val uid = result.getOrNull()?.uid
                if (uid != null) {
                    authViewModel.fetchUserData(uid)
                } else {
                    Toast.makeText(this, "Erro ao obter ID do usuário.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Falha no login
                Toast.makeText(this, "Falha no login: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }

        authViewModel.userData.observe(this) { result ->
            // Observador da SplashActivity fará o redirecionamento
            // Aqui apenas garantimos que o login foi completo
            if (result.isSuccess) {
                // Chama a Splash (que agora vai redirecionar correto)
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}