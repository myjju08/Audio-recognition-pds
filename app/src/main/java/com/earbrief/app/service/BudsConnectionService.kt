package com.earbrief.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BudsConnectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
