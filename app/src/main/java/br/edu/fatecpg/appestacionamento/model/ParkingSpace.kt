package br.edu.fatecpg.appestacionamento.model

import android.os.Parcelable
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingSpace(
    val id: String = "",
    val spaceNumber: String = "", // Ex: "A-01"
    val isOccupied: Boolean = false,
    val reservedByUid: String? = null,
    val reservationExpiry: Date? = null,
    val hourlyRate: Double = 5.0 // Tarifa por hora
) : Parcelable