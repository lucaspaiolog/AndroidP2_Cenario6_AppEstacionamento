package br.edu.fatecpg.appestacionamento.view.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import br.edu.fatecpg.appestacionamento.utils.NotificationUtils

/**
 * Este BroadcastReceiver é ativado pelo AlarmManager.
 * Sua única função é receber o alarme e disparar a notificação.
 */
class ReservationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SPACE_NUMBER = "EXTRA_SPACE_NUMBER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d("AlarmReceiver", "Alarme (RF05) recebido!")

        // Garante que o canal de notificação exista
        NotificationUtils.createNotificationChannel(context)

        // Pega os dados do intent
        val spaceNumber = intent.getStringExtra(EXTRA_SPACE_NUMBER) ?: "N/A"

        // Mostra a notificação
        NotificationUtils.showReservationWarning(context, spaceNumber)
    }
}