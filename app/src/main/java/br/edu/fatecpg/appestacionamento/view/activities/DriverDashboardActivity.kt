package br.edu.fatecpg.appestacionamento.view.activities

import android.Manifest // Importação necessária
import android.content.Intent
import android.content.pm.PackageManager // Importação necessária
import android.os.Build // Importação necessária
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // Importação necessária
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Importação necessária
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.fatecpg.appestacionamento.Injector
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.databinding.ActivityDriverDashboardBinding
import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import br.edu.fatecpg.appestacionamento.model.Reservation
import br.edu.fatecpg.appestacionamento.utils.AlarmScheduler // Importação necessária
import br.edu.fatecpg.appestacionamento.utils.NotificationUtils // Importação necessária
import br.edu.fatecpg.appestacionamento.view.adapters.ParkingSpaceAdapter
import br.edu.fatecpg.appestacionamento.viewmodel.AuthViewModel
import br.edu.fatecpg.appestacionamento.viewmodel.DriverViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverDashboardBinding
    private val authViewModel: AuthViewModel by viewModels {
        Injector.provideAuthViewModelFactory()
    }
    private val driverViewModel: DriverViewModel by viewModels {
        Injector.provideDriverViewModelFactory()
    }

    private lateinit var parkingAdapter: ParkingSpaceAdapter
    private var currentUserId: String? = null
    private var activeReservation: Reservation? = null

    // RF05: ActivityResultLauncher para permissão de notificação (Android 13+)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permissão de notificação concedida.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão de notificação negada. Você não receberá avisos.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = authViewModel.getCurrentUser()?.uid
        if (currentUserId == null) {
            handleLogout()
            return
        }

        // RF05: Cria o canal de notificação (para Android 8.0+)
        NotificationUtils.createNotificationChannel(this)
        // RF05: Pede permissão de notificação (para Android 13+)
        askForNotificationPermission()

        setupRecyclerView()
        setupObservers()

        binding.btnReleaseSpace.setOnClickListener {
            handleReleaseSpace()
        }
    }

    private fun setupRecyclerView() {
        parkingAdapter = ParkingSpaceAdapter { space ->
            // Clique em uma vaga livre
            handleReserveSpace(space)
        }
        binding.rvParkingSpaces.apply {
            layoutManager = LinearLayoutManager(this@DriverDashboardActivity)
            adapter = parkingAdapter
        }
    }

    private fun setupObservers() {
        // Observa vagas
        driverViewModel.parkingSpaces.observe(this) { spaces ->
            binding.progressBar.visibility = View.GONE
            binding.tvEmptyList.visibility = if (spaces.isEmpty()) View.VISIBLE else View.GONE
            parkingAdapter.submitList(spaces)
        }

        // Observa reserva ativa
        currentUserId?.let { uid ->
            driverViewModel.getActiveReservation(uid).observe(this) { reservation ->

                // RF05: Lógica de agendamento/cancelamento de alarme
                // Verifica se a reserva mudou (foi criada ou removida)
                if (reservation != null && activeReservation == null) {
                    // Reserva foi CRIADA
                    AlarmScheduler.scheduleReservationWarning(this, reservation)
                } else if (reservation == null && activeReservation != null) {
                    // Reserva foi REMOVIDA (liberada ou expirada)
                    AlarmScheduler.cancelReservationWarning(this, activeReservation!!)
                }

                activeReservation = reservation
                updateReservationUI(reservation)
            }
        }

        // Observa resultado das operações (reservar/liberar)
        driverViewModel.operationResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, result.getOrNull(), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Atualiza a UI se o motorista tem uma reserva ativa
    private fun updateReservationUI(reservation: Reservation?) {
        if (reservation != null) {
            // Tem reserva ativa: Mostra o card de reserva, esconde a lista de vagas
            binding.cardActiveReservation.visibility = View.VISIBLE
            binding.rvParkingSpaces.visibility = View.GONE
            binding.tvEmptyList.visibility = View.GONE
            binding.tvLabelAvailable.visibility = View.GONE

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvReservedSpace.text = "Vaga: ${reservation.spaceNumber}"
            binding.tvReservationStart.text = "Início: ${reservation.startTime?.let { dateFormat.format(it) } ?: "N/A"}"
            binding.tvReservationEnd.text = "Expira em (RN02): ${reservation.endTime?.let { dateFormat.format(it) } ?: "N/A"}"

        } else {
            // Não tem reserva ativa: Esconde o card, mostra a lista
            binding.cardActiveReservation.visibility = View.GONE
            binding.rvParkingSpaces.visibility = View.VISIBLE
            binding.tvLabelAvailable.visibility = View.VISIBLE
        }
    }

    private fun handleReserveSpace(space: ParkingSpace) {
        if (activeReservation != null) {
            Toast.makeText(this, "Você já possui uma reserva ativa.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Reserva")
            .setMessage("Deseja reservar a vaga ${space.spaceNumber}? (Limite de 2h - RN02)")
            .setPositiveButton("Reservar") { _, _ ->
                currentUserId?.let { uid ->
                    driverViewModel.reserveSpace(space, uid)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleReleaseSpace() {
        activeReservation?.let { res ->
            AlertDialog.Builder(this)
                .setTitle("Liberar Vaga")
                .setMessage("Deseja liberar a vaga ${res.spaceNumber}?")
                .setPositiveButton("Liberar") { _, _ ->
                    driverViewModel.releaseSpace(res)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun askForNotificationPermission() {
        // Verifica se é Android 13 (TIRAMISU) ou superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Verifica se a permissão AINDA NÃO foi concedida
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Pede a permissão
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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