package br.edu.fatecpg.appestacionamento.repository

import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import br.edu.fatecpg.appestacionamento.model.Reservation
import kotlinx.coroutines.flow.Flow

interface ParkingRepository {
    // Flow para atualizações em tempo real das vagas
    fun getParkingSpacesFlow(): Flow<List<ParkingSpace>>

    // Flow para reservas ativas do usuário
    fun getActiveReservationFlow(userId: String): Flow<Reservation?>

    // Motorista: Reservar uma vaga (RN02)
    suspend fun reserveSpace(space: ParkingSpace, userId: String): Result<Boolean>

    // Motorista: Liberar uma vaga
    suspend fun releaseSpace(reservation: Reservation): Result<Boolean>

    // Admin: Adicionar nova vaga (RF03)
    suspend fun addParkingSpace(spaceNumber: String, hourlyRate: Double): Result<Boolean>

    // Admin: Atualizar tarifa da vaga (RF03)
    suspend fun updateParkingSpace(space: ParkingSpace): Result<Boolean>

    // Admin: Remover vaga
    suspend fun deleteParkingSpace(spaceId: String): Result<Boolean>

    // (RN04) - Simplificado: Verifica e libera vagas expiradas
    suspend fun checkAndReleaseExpiredReservations(): Result<Int>

    // (RN03) - Relatórios (simplificado)
    fun getReservationsForReport(): Flow<List<Reservation>>
}