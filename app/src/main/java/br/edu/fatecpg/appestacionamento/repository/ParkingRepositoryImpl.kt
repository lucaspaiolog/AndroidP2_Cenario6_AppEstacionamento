package br.edu.fatecpg.appestacionamento.repository

import android.util.Log
import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import br.edu.fatecpg.appestacionamento.model.Reservation
import br.edu.fatecpg.appestacionamento.utils.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class ParkingRepositoryImpl(private val db: FirebaseFirestore) : ParkingRepository {

    override fun getParkingSpacesFlow(): Flow<List<ParkingSpace>> {
        // Listener em tempo real (RF02)
        return db.collection(Constants.COLLECTION_SPACES)
            .orderBy("spaceNumber") // Ordena pelo número da vaga
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<ParkingSpace>()
            }
    }

    override fun getActiveReservationFlow(userId: String): Flow<Reservation?> {
        // Listener para reserva ativa do motorista
        return db.collection(Constants.COLLECTION_RESERVATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "active")
            .limit(1)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toObject<Reservation>()
            }
    }

    override suspend fun reserveSpace(space: ParkingSpace, userId: String): Result<Boolean> {
        return try {
            val now = Calendar.getInstance().time
            // RN02: Limite de 2 horas
            val expiryTime = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 2)
            }.time

            val reservationId = db.collection(Constants.COLLECTION_RESERVATIONS).document().id
            val newReservation = Reservation(
                id = reservationId,
                userId = userId,
                spaceId = space.id,
                spaceNumber = space.spaceNumber,
                startTime = now,
                endTime = expiryTime, // Define a expiração
                status = "active"
            )

            // Transação para garantir consistência
            db.runTransaction { transaction ->
                val spaceRef = db.collection(Constants.COLLECTION_SPACES).document(space.id)
                val currentSpace = transaction.get(spaceRef).toObject<ParkingSpace>()

                // Verifica se a vaga ainda está livre
                if (currentSpace != null && !currentSpace.isOccupied) {
                    // Atualiza a vaga
                    transaction.update(
                        spaceRef,
                        mapOf(
                            "occupied" to true,
                            "reservedByUid" to userId,
                            "reservationExpiry" to expiryTime
                        )
                    )

                    // Cria a reserva
                    val reservationRef = db.collection(Constants.COLLECTION_RESERVATIONS).document(reservationId)
                    transaction.set(reservationRef, newReservation)
                } else {
                    throw Exception("Vaga não está mais disponível.")
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e("ParkingRepo", "Erro ao reservar vaga", e)
            Result.failure(e)
        }
    }

    override suspend fun releaseSpace(reservation: Reservation): Result<Boolean> {
        return try {
            // Transação para liberar a vaga e completar a reserva
            db.runTransaction { transaction ->
                val spaceRef = db.collection(Constants.COLLECTION_SPACES).document(reservation.spaceId)
                val reservationRef = db.collection(Constants.COLLECTION_RESERVATIONS).document(reservation.id)

                // Atualiza a vaga
                transaction.update(
                    spaceRef,
                    mapOf(
                        "occupied" to false,
                        "reservedByUid" to null,
                        "reservationExpiry" to null
                    )
                )

                // Atualiza a reserva
                transaction.update(
                    reservationRef,
                    mapOf(
                        "status" to "completed",
                        "endTime" to FieldValue.serverTimestamp() // Marca a hora real de saída
                    )
                )
            }.await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("ParkingRepo", "Erro ao liberar vaga", e)
            Result.failure(e)
        }
    }

    override suspend fun addParkingSpace(spaceNumber: String, hourlyRate: Double): Result<Boolean> {
        return try {
            val spaceId = db.collection(Constants.COLLECTION_SPACES).document().id
            val newSpace = ParkingSpace(
                id = spaceId,
                spaceNumber = spaceNumber,
                isOccupied = false,
                hourlyRate = hourlyRate
            )
            db.collection(Constants.COLLECTION_SPACES).document(spaceId).set(newSpace).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateParkingSpace(space: ParkingSpace): Result<Boolean> {
        return try {
            db.collection(Constants.COLLECTION_SPACES).document(space.id).set(space).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteParkingSpace(spaceId: String): Result<Boolean> {
        return try {
            db.collection(Constants.COLLECTION_SPACES).document(spaceId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // RN04 - Implementação simplificada
    override suspend fun checkAndReleaseExpiredReservations(): Result<Int> {
        return try {
            val now = Date()
            val expiredReservations = db.collection(Constants.COLLECTION_RESERVATIONS)
                .whereEqualTo("status", "active")
                .whereLessThan("endTime", now)
                .get()
                .await()

            if (expiredReservations.isEmpty) {
                return Result.success(0) // Nenhuma vaga expirada
            }

            var releasedCount = 0
            val batch = db.batch()

            for (doc in expiredReservations.documents) {
                val reservation = doc.toObject<Reservation>()
                if (reservation != null) {
                    val spaceRef = db.collection(Constants.COLLECTION_SPACES).document(reservation.spaceId)
                    val reservationRef = doc.reference

                    // Libera a vaga
                    batch.update(spaceRef, mapOf(
                        "occupied" to false,
                        "reservedByUid" to null,
                        "reservationExpiry" to null
                    ))

                    // Atualiza a reserva para "expired"
                    batch.update(reservationRef, "status", "expired")
                    releasedCount++
                }
            }
            batch.commit().await()
            Result.success(releasedCount)
        } catch (e: Exception) {
            Log.e("ParkingRepo", "Erro ao verificar vagas expiradas", e)
            Result.failure(e)
        }
    }

    // RN03 - Simplificado
    override fun getReservationsForReport(): Flow<List<Reservation>> {
        // Busca reservas dos últimos 30 dias (simplificado)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30)

        return db.collection(Constants.COLLECTION_RESERVATIONS)
            .whereGreaterThanOrEqualTo("startTime", calendar.time)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Reservation>()
            }
    }
}