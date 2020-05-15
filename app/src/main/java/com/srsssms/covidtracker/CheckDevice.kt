@file:Suppress("PropertyName", "DEPRECATION")

package com.srsssms.covidtracker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_check_device.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class CheckDevice : AppCompatActivity() {

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_device)
        Log.d("registerdebug", "Device ID: ${Settings.Secure.getString(this@CheckDevice.contentResolver, Settings.Secure.ANDROID_ID)}")
        animCheck.setAnimation("checking.json")//ANIMATION WITH LOTTIE FOR CHECKING DEVICE
        animCheck.loop(true)
        animCheck.playAnimation()

        val connectivityManager: ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).state === NetworkInfo.State.CONNECTED || //atur kondisi parameter connected atau ga
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).state === NetworkInfo.State.CONNECTED
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager //hilangkan keyboard
        if (Objects.requireNonNull(imm).isAcceptingText) {
            @Suppress("UNNECESSARY_SAFE_CALL")
            imm.hideSoftInputFromWindow(Objects.requireNonNull(currentFocus)?.windowToken, 0)
        }
        //penentuan kondisi online/offline
        if (connected) { //konek tp belum login > Online Auth
            onlineAuth(Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID))
        } else if (!connected && !PrefManager(this).logged) { //ga konek belum login > Offline Auth
            AlertDialogUtility.withSingleAction(this,"Ulang", "Silahkan hubungkan dengan koneksi yang stabil untuk mendaftar", "network_error.json") {
                val intent = Intent(this@CheckDevice, Splash::class.java)
                startActivity(intent)
            }
        } else if (!connected && PrefManager(this).logged) { //konek tp udah login > Bypass
            val intent = Intent(this@CheckDevice, ActivityMenu::class.java)
            startActivity(intent)
        } else {
            TODO() //ADD ERROR
        }
    }

    fun checkGoner(){
        animCheck.visibility = View.GONE
        tvCheckDesc.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    fun onlineAuth(deviceID: String) {
        animCheck.setAnimation("checking.json")//ANIMATION WITH LOTTIE FOR CHECKING DEVICE
        animCheck.loop(true)
        animCheck.playAnimation()
        //method volley buat login (POST data to PHP)
                @Suppress("UNUSED_ANONYMOUS_PARAMETER") val strReq: StringRequest = object : StringRequest(Method.POST, "${Db.TAG_URL}login.php",
            Response.Listener { response ->
                try {
                    val jObj = JSONObject(response)
                    val success = jObj.getInt(Db.TAG_SUCCESS)
                    Log.d("registerdebug", "success code: $success")
                    // Check for error node in json
                    if (success == 1) {
                        checkGoner()
                        //pengaturan shared preferences
                        val prefMan = PrefManager(this)
                        prefMan.logged = true
                        prefMan.userID = jObj.getInt(Db.TAG_IDTRACK).toString()
                        prefMan.statusAkun = jObj.getInt(Db.TAG_STATUSAKUN)
                        prefMan.name = jObj.getString(Db.TAG_NAMA)
                        prefMan.alamat = jObj.getString(Db.TAG_ALAMAT)
                        prefMan.latA = jObj.getString(Db.TAG_LATA).toFloat()
                        prefMan.lonA = jObj.getString(Db.TAG_LONA).toFloat()
                        AlertDialogUtility.withSingleAction(this, "OK", "Gawai anda telah terdaftar!", "check_success.json"
                        ) {
                            val intent = Intent(this@CheckDevice, ActivityMenu::class.java)
                            startActivity(intent)
                        }
                    } else {
                        checkGoner()
                        AlertDialogUtility.withSingleAction(this, "OK", jObj.getString(Db.TAG_MESSAGE), "warning.json"
                        ) {
                            val intent = Intent(this@CheckDevice, Karantina::class.java)
                            startActivity(intent)
                        }
                    }
                } catch (e: JSONException) {
                    checkGoner()
                    AlertDialogUtility.withSingleAction(this, "Ulang", "Data error, hubungi pengembang: $e", "warning.json"
                    ) {
                        val intent = Intent(this@CheckDevice, Splash::class.java)
                        startActivity(intent)
                    }
                    e.printStackTrace()
                }
            }, Response.ErrorListener { error ->
                checkGoner()
                AlertDialogUtility.withSingleAction(this,"Ulang", "Terjadi kesalahan koneksi", "network_error.json") {
                    val intent = Intent(this@CheckDevice, Splash::class.java)
                    startActivity(intent)
                }
            }) {
            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params: MutableMap<String, String> = HashMap()
                params[Db.TAG_DEVICEID] = deviceID
                return params
            }
        }
        Volley.newRequestQueue(this).add(strReq)
    }
}
