package com.srsssms.covidtracker

import android.app.IntentService
import android.content.Intent

class ReminderIntentService: IntentService(ReminderIntentService::class.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        Reminder().executeTask()
    }

}