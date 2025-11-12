package br.edu.fatecpg.appestacionamento.view.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.edu.fatecpg.appestacionamento.Injector
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.databinding.ActivityRegisterBinding
import br.edu.fatecpg.appestacionamento.utils.Constants
import br.edu.fatecpg.appestacionamento.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModels {
        Injector.provideAuthViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            handleRegister()
        }
        binding.tvGoToLogin.setOnClickListener {
            finish() // Volta para a Login
        }
    }

    private fun handleRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres.", Toast.LENGTH_SHORT).show()
            return
        }

        // RN01: Identificar usuário
        val selectedRadioId = binding.rgUserType.checkedRadioButtonId
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Selecione o tipo de usuário.", Toast.LENGTH_SHORT).show()
            return
        }

        val userType = if (selectedRadioId == R.id.rbAdmin) {
            Constants.USER_TYPE_ADMIN
        } else {
            Constants.USER_TYPE_DRIVER
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        authViewModel.register(email, password, userType)
    }

    private fun setupObservers() {
        authViewModel.authResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.btnRegister.isEnabled = true

            if (result.isSuccess) {
                // Sucesso no registro, busca dados para redirecionar
                val uid = result.getOrNull()?.uid
                if (uid != null) {
                    authViewModel.fetchUserData(uid)
                }
            } else {
                // Falha no registro
                Toast.makeText(this, "Falha no registro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }

        authViewModel.userData.observe(this) { result ->
            if (result.isSuccess) {
                // Chama a Splash (que agora vai redirecionar correto)
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}