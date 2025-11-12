package br.edu.fatecpg.appestacionamento.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import br.edu.fatecpg.appestacionamento.model.Reservation
import br.edu.fatecpg.appestacionamento.view.receivers.ReservationAlarmReceiver
import java.util.Date

/**
 * Classe utilitária para agendar e cancelar os alarmes de aviso de expiração (RF05)
 */
object AlarmScheduler {

    // Avisa 10 minutos antes de expirar
    private const val WARNING_TIME_MS_BEFORE_EXPIRY = 10 * 60 * 1000

    /**
     * Agenda um alarme para notificar o usuário 10 minutos antes da reserva expirar.
     */
    fun scheduleReservationWarning(context: Context, reservation: Reservation) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reservationExpiryTime = reservation.endTime?.time
        if (reservationExpiryTime == null) {
            Log.e("AlarmScheduler", "Data de expiração da reserva é nula. Alarme não agendado.")
            return
        }

        // Calcula a hora do alarme (10 min antes de expirar)
        val triggerAtMillis = reservationExpiryTime - WARNING_TIME_MS_BEFORE_EXPIRY
        val now = System.currentTimeMillis()

        // Se o horário de aviso já passou (ex: reserva < 10 min), não agenda
        if (triggerAtMillis <= now) {
            Log.d("AlarmScheduler", "Horário do alarme ($triggerAtMillis) já passou. Não agendando.")
            return
        }

        val pendingIntent = createPendingIntent(context, reservation)

        try {
            // Verifica se o app pode agendar alarmes exatos (necessário no Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Não é possível agendar alarmes exatos. A notificação pode atrasar.")
                // Fallback para alarme não-exato
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            }

            // Agenda o alarme exato
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )

            Log.d("AlarmScheduler", "Alarme (RF05) agendado para ${Date(triggerAtMillis)} (Vaga: ${reservation.spaceNumber})")

        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Falha ao agendar alarme. Permissão 'SCHEDULE_EXACT_ALARM' está faltando?", e)
        }
    }

    /**
     * Cancela um alarme agendado previamente (ex: quando o usuário libera a vaga).
     */
    fun cancelReservationWarning(context: Context, reservation: Reservation) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createPendingIntent(context, reservation)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Alarme (RF05) cancelado para vaga ${reservation.spaceNumber}")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Erro ao cancelar alarme", e)
        }
    }

    /**
     * Cria o PendingIntent para o BroadcastReceiver.
     * Usamos o ID da reserva para garantir que cada alarme seja único.
     */
    private fun createPendingIntent(context: Context, reservation: Reservation): PendingIntent {
        val intent = Intent(context, ReservationAlarmReceiver::class.java).apply {
            // Passa o número da vaga para o Receiver
            putExtra(ReservationAlarmReceiver.EXTRA_SPACE_NUMBER, reservation.spaceNumber)
            // Usa o ID da reserva como "action" para garantir que o PendingIntent seja único
            action = reservation.id
        }

        // Usamos o hashCode do ID da reserva como requestCode
        val requestCode = reservation.id.hashCode()

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}