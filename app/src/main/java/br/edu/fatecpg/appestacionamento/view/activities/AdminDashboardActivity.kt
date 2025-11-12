package br.edu.fatecpg.appestacionamento.view.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.appestacionamento.Injector
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.databinding.ActivityAdminDashboardBinding
import br.edu.fatecpg.appestacionamento.databinding.DialogAddSpaceBinding
import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import br.edu.fatecpg.appestacionamento.view.adapters.AdminSpaceAdapter
import br.edu.fatecpg.appestacionamento.viewmodel.AdminViewModel
import br.edu.fatecpg.appestacionamento.viewmodel.AuthViewModel
import java.text.NumberFormat
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val authViewModel: AuthViewModel by viewModels {
        Injector.provideAuthViewModelFactory()
    }
    private val adminViewModel: AdminViewModel by viewModels {
        Injector.provideAdminViewModelFactory()
    }

    private lateinit var adminAdapter: AdminSpaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // RN04: Verifica vagas expiradas ao abrir o painel admin
        adminViewModel.checkExpired()
    }

    private fun setupRecyclerView() {
        adminAdapter = AdminSpaceAdapter(
            onEditClicked = { space -> showAddEditDialog(space) },
            onDeleteClicked = { space -> showDeleteDialog(space) }
        )
        binding.rvAdminSpaces.apply {
            layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
            adapter = adminAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddSpace.setOnClickListener {
            showAddEditDialog(null) // RF03
        }

        // RN03 - Simplificado: Mostra ocupação
        binding.btnShowReports.setOnClickListener {
            showOccupancyReport()
        }
    }

    private fun setupObservers() {
        // Observa vagas
        adminViewModel.parkingSpaces.observe(this) { spaces ->
            binding.progressBar.visibility = View.GONE
            binding.tvEmptyList.visibility = if (spaces.isEmpty()) View.VISIBLE else View.GONE
            adminAdapter.submitList(spaces)

            // Atualiza status de ocupação (RN03 - Simplificado)
            val occupied = spaces.count { it.isOccupied }
            val total = spaces.size
            binding.tvOccupancyStatus.text = "Ocupação: $occupied / $total"
        }

        // Observa resultado das operações
        adminViewModel.operationResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, result.getOrNull(), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddEditDialog(space: ParkingSpace?) {
        val dialogBinding = DialogAddSpaceBinding.inflate(LayoutInflater.from(this))
        val title = if (space == null) "Adicionar Vaga" else "Editar Vaga ${space.spaceNumber}"

        // Preenche dados se for edição
        if (space != null) {
            dialogBinding.etSpaceNumber.setText(space.spaceNumber)
            dialogBinding.etSpaceNumber.isEnabled = false // Não permite editar o número
            dialogBinding.etHourlyRate.setText(space.hourlyRate.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton(if (space == null) "Adicionar" else "Salvar") { _, _ ->
                val number = dialogBinding.etSpaceNumber.text.toString().trim()
                val rateStr = dialogBinding.etHourlyRate.text.toString().trim()

                if (number.isEmpty() || rateStr.isEmpty()) {
                    Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val rate = rateStr.toDoubleOrNull()
                if (rate == null || rate <= 0) {
                    Toast.makeText(this, "Tarifa inválida.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (space == null) {
                    // Adicionar
                    adminViewModel.addSpace(number, rate)
                } else {
                    // Editar
                    val updatedSpace = space.copy(hourlyRate = rate)
                    adminViewModel.updateSpace(updatedSpace)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteDialog(space: ParkingSpace) {
        AlertDialog.Builder(this)
            .setTitle("Remover Vaga")
            .setMessage("Tem certeza que deseja remover a vaga ${space.spaceNumber}? (Não pode ser removida se estiver ocupada)")
            .setPositiveButton("Remover") { _, _ ->
                adminViewModel.deleteSpace(space)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // RN03 - Relatório Simplificado de Ocupação
    private fun showOccupancyReport() {
        adminViewModel.reports.observe(this) { reservations ->
            val totalReservations = reservations.size
            val completed = reservations.count { it.status == "completed" }
            val expired = reservations.count { it.status == "expired" }
            val active = reservations.count { it.status == "active" }

            // Calcula receita (simplificado)
            val ptBr = Locale("pt", "BR")
            val currencyFormat = NumberFormat.getCurrencyInstance(ptBr)
            val totalRevenue = reservations.filter { it.status == "completed" }.sumOf { res ->
                // Cálculo simples, 5.0 por reserva (em app real, calcularia por hora)
                5.0
            }

            val reportMessage = """
             Relatório dos últimos 30 dias (Simplificado):
             
             Total de Reservas: $totalReservations
             Completadas: $completed
             Expiradas: $expired
             Ativas: $active
             
             Receita Estimada: ${currencyFormat.format(totalRevenue)}
             """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Relatório de Ocupação (RN03)")
                .setMessage(reportMessage)
                .setPositiveButton("OK", null)
                .show()

            // Remove o observador para não mostrar o dialog toda hora
            adminViewModel.reports.removeObservers(this)
        }
    }


    // Menu de Logout
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                handleLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleLogout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}