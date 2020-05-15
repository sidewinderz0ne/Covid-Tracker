package com.srsssms.covidtracker
import android.content.Context
import android.content.SharedPreferences

class PrefManager(_context: Context) {
    var pref: SharedPreferences
    var editor: SharedPreferences.Editor
    // shared pref mode
    var privateMode = 0

    var logged: Boolean
        get() = pref.getBoolean(LOGGED, false)
        set(logged) {
            editor.putBoolean(LOGGED, logged)
            editor.commit()
        }

    var name: String?
        get() = pref.getString(NAME, "")
        set(sureName) {
            editor.putString(NAME, sureName)
            editor.commit()
        }

    var imei: String?
        get() = pref.getString(IMEI, "")
        set(imeis) {
            editor.putString(IMEI, imeis)
            editor.commit()
        }

    var alamat: String?
        get() = pref.getString(ALAMAT, "")
        set(alam) {
            editor.putString(ALAMAT, alam)
            editor.commit()
        }

    var latA: Float
        get() = pref.getFloat(LATA, 0f)
        set(lata) {
            editor.putFloat(LATA, lata)
            editor.commit()
        }

    var lonA: Float
        get() = pref.getFloat(LONA, 0f)
        set(lona) {
            editor.putFloat(LONA, lona)
            editor.commit()
        }

    var userID: String?
        get() = pref.getString(USERID, null)
        set(user) {
            editor.putString(USERID, user)
            editor.commit()
        }

    var statusAkun: Int
        get() = pref.getInt(AKUN, 0)
        set(akun) {
            editor.putInt(AKUN, akun)
            editor.commit()
        }

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "covidtracker"
        private const val LOGGED = "logged"
        private const val USERID = "user_id"
        private const val NAME = "name"
        private const val IMEI = "deviceid"
        private const val ALAMAT = "alamat"
        private const val LATA = "latitude_a"
        private const val LONA = "longitude_a"
        private const val AKUN = "status_akun"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}