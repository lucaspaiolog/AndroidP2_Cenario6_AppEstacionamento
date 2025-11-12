package br.edu.fatecpg.appestacionamento.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.view.activities.DriverDashboardActivity

object NotificationUtils {

    private const val CHANNEL_ID = "reservation_warning_channel"
    private const val CHANNEL_NAME = "Avisos de Reserva"
    private const val CHANNEL_DESC = "Notificações para reservas expirando"
    private const val NOTIFICATION_ID = 1001

    /**
     * Cria o canal de notificação (necessário para Android 8.0+)
     * É seguro chamar isso várias vezes.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Exibe a notificação de aviso.
     */
    fun showReservationWarning(context: Context, spaceNumber: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir o app ao clicar na notificação
        val intent = Intent(context, DriverDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Aviso de Reserva")
            .setContentText("Sua reserva para a vaga $spaceNumber está prestes a expirar!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}