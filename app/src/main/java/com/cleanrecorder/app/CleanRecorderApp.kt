package com.cleanrecorder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class CleanRecorderApp : Application() {

    companion object {
        const val RECORDING_CHANNEL_ID = "recording_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RECORDING_CHANNEL_ID,
                "Clean Recorder Active Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps recording active while the phone is locked"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
