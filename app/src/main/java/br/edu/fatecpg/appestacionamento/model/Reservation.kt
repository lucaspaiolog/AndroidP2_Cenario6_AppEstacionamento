package br.edu.fatecpg.appestacionamento.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Reservation(
    val id: String = "",
    val userId: String = "",
    val spaceId: String = "",
    val spaceNumber: String = "",
    @ServerTimestamp
    val startTime: Date? = null,
    val endTime: Date? = null,
    val status: String = "active" // "active", "expired", "completed"
)