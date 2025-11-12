package br.edu.fatecpg.appestacionamento.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import br.edu.fatecpg.appestacionamento.model.Reservation
import br.edu.fatecpg.appestacionamento.repository.ParkingRepository
import kotlinx.coroutines.launch

class DriverViewModel(private val repository: ParkingRepository) : ViewModel() {

    // Flow de vagas em tempo real (RF02)
    val parkingSpaces: LiveData<List<ParkingSpace>> = repository.getParkingSpacesFlow().asLiveData()

    // Flow da reserva ativa do usuário
    fun getActiveReservation(userId: String) = repository.getActiveReservationFlow(userId).asLiveData()

    // Resultado das ações
    private val _operationResult = MutableLiveData<Result<String>>()
    val operationResult: LiveData<Result<String>> = _operationResult

    fun reserveSpace(space: ParkingSpace, userId: String) {
        viewModelScope.launch {
            // RN04 (Simplificado): Verifica vagas expiradas antes de reservar
            repository.checkAndReleaseExpiredReservations()

            val result = repository.reserveSpace(space, userId)
            if (result.isSuccess) {
                _operationResult.value = Result.success("Vaga ${space.spaceNumber} reservada!")
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Erro ao reservar."))
            }
        }
    }

    fun releaseSpace(reservation: Reservation) {
        viewModelScope.launch {
            val result = repository.releaseSpace(reservation)
            if (result.isSuccess) {
                _operationResult.value = Result.success("Vaga ${reservation.spaceNumber} liberada!")
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Erro ao liberar vaga."))
            }
        }
    }
}

// Factory
class DriverViewModelFactory(private val repository: ParkingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DriverViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}