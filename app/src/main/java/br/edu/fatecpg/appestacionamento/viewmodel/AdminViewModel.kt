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

class AdminViewModel(private val repository: ParkingRepository) : ViewModel() {

    // Flow de vagas (para gerenciamento)
    val parkingSpaces: LiveData<List<ParkingSpace>> = repository.getParkingSpacesFlow().asLiveData()

    // Flow de relatórios (RN03 - Simplificado)
    val reports: LiveData<List<Reservation>> = repository.getReservationsForReport().asLiveData()

    // Resultado das ações
    private val _operationResult = MutableLiveData<Result<String>>()
    val operationResult: LiveData<Result<String>> = _operationResult

    fun addSpace(spaceNumber: String, hourlyRate: Double) {
        viewModelScope.launch {
            val result = repository.addParkingSpace(spaceNumber, hourlyRate)
            if (result.isSuccess) {
                _operationResult.value = Result.success("Vaga $spaceNumber adicionada.")
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Erro ao adicionar vaga."))
            }
        }
    }

    fun updateSpace(space: ParkingSpace) {
        viewModelScope.launch {
            val result = repository.updateParkingSpace(space)
            if (result.isSuccess) {
                _operationResult.value = Result.success("Vaga ${space.spaceNumber} atualizada.")
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Erro ao atualizar vaga."))
            }
        }
    }

    fun deleteSpace(space: ParkingSpace) {
        viewModelScope.launch {
            if (space.isOccupied) {
                _operationResult.value = Result.failure(Exception("Não é possível remover vaga ocupada."))
                return@launch
            }
            val result = repository.deleteParkingSpace(space.id)
            if (result.isSuccess) {
                _operationResult.value = Result.success("Vaga ${space.spaceNumber} removida.")
            } else {
                _operationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Erro ao remover vaga."))
            }
        }
    }

    fun checkExpired() {
        viewModelScope.launch {
            val result = repository.checkAndReleaseExpiredReservations()
            if (result.isSuccess) {
                _operationResult.value = Result.success("${result.getOrNull()} vagas expiradas foram liberadas.")
            }
        }
    }
}

// Factory
class AdminViewModelFactory(private val repository: ParkingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}