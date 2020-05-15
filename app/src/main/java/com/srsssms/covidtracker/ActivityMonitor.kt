package com.srsssms.covidtracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_monitor.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class ActivityMonitor : AppCompatActivity() {

    //inisialisasi fingerprint
    var promptInfo: BiometricPrompt.PromptInfo? = null
    private var biometricPrompt: BiometricPrompt? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor)
        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = false
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://covid.srs-ssms.com/admin/login")
        lottieMonitor.setAnimation("covid.json")//ANIMATION WITH LOTTIE FOR CHECKING DEVICE
        lottieMonitor.loop(true)
        lottieMonitor.playAnimation()
        btHomeMonitor.setOnClickListener {
            val intent = Intent(this@ActivityMonitor, ActivityMenu::class.java)
            startActivity(intent)
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                progressMonitor.visibility = View.VISIBLE
                if (progress == 100){
                    progressMonitor.visibility = View.GONE
                    Handler().postDelayed({ // Stop animation (This will be after 3 seconds)
                        swipeRefresh.isRefreshing = false
                    }, 3000) // Delay in millis
                }
            }
        }
        swipeRefresh.setOnRefreshListener {
            webView.reload()
            webView.loadUrl("https://covid.srs-ssms.com/admin/home")
        }
        //fungsi fingerprint
        val prefManager = PrefManager(this) //inisialisasi shared preference
        promptInfo = BiometricPrompt.PromptInfo.Builder() //DIALOG BUILDER FOR BIOMETRIC AUTHENTIFICATION
            .setTitle("Selamat datang " + prefManager.name + "!")
            .setSubtitle("Gunakan sidik jari untuk melanjutkan ke aplikasi")
            .setDescription("Covid Tracker")
            .setNegativeButtonText("Gunakan password saja")
            .build()
        val executor = Executors.newSingleThreadExecutor()
        val activity = this
        biometricAuth(activity, executor)
        biometricPrompt!!.authenticate(promptInfo!!)
    }
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    //fungsi finger print
    private fun biometricAuth(activity: FragmentActivity, executor: Executor){
        biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    promptInfo!!.isConfirmationRequired
                } else {
                    //TODO: Called when an unrecoverable error has been encountered and the operation is complete.
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                biometricSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Looper.prepare() //Call looper.prepare()
                Toasty.error(this@ActivityMonitor, "Mohon gunakan jari yang teregistrasi", Toast.LENGTH_SHORT, true).show()
                val snack = Snackbar.make(monitorParent, "Mohon gunakan jari yang teregistrasi", Snackbar.LENGTH_SHORT)
                val view = snack.view
                val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(view.layoutParams)
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snack.show()
                Looper.loop()
            }
        })
    }

    //biometricAuth success
    fun biometricSuccess(){
        val refresh = Handler(Looper.getMainLooper())
        refresh.post {  if (progressMonitor.visibility == View.GONE){
            progressMonitor.visibility = View.VISIBLE
        } }
        //method volley buat login (POST data to PHP)
        @Suppress("UNUSED_ANONYMOUS_PARAMETER") val strReq: StringRequest = object : StringRequest(Method.POST, "${Db.TAG_URL}biometric.php",
            Response.Listener { response ->
                try {
                    val jObj = JSONObject(response)
                    val success = jObj.getInt(Db.TAG_SUCCESS)
                    Log.d("registerdebug", "success code: $success")
                    // Check for error node in json
                    if (success == 1) {
                        progressMonitor.visibility = View.GONE
                        //pengaturan shared preferences
                        val prefMan = PrefManager(this)
                        prefMan.logged = true
                        prefMan.userID = jObj.getInt(Db.TAG_IDTRACK).toString()
                        prefMan.statusAkun = jObj.getInt(Db.TAG_STATUSAKUN)
                        prefMan.name = jObj.getString(Db.TAG_NAMA)
                        prefMan.alamat = jObj.getString(Db.TAG_ALAMAT)
                        prefMan.latA = jObj.getString(Db.TAG_LATA).toFloat()
                        prefMan.lonA = jObj.getString(Db.TAG_LONA).toFloat()
                    } else {
                        progressMonitor.visibility = View.GONE
                        AlertDialogUtility.withSingleAction(this, "OK", jObj.getString(Db.TAG_MESSAGE), "warning.json"
                        ) {
                            val intent = Intent(this, ActivityMenu::class.java)
                            startActivity(intent)
                        }
                    }
                } catch (e: JSONException) {
                    progressMonitor.visibility = View.GONE
                    AlertDialogUtility.withSingleAction(this, "Ulang", "Data error, hubungi pengembang: $e", "warning.json"
                    ) {
                        val intent = Intent(this, ActivityMenu::class.java)
                        startActivity(intent)
                    }
                    e.printStackTrace()
                }
            }, Response.ErrorListener { error ->
                progressMonitor.visibility = View.GONE
                AlertDialogUtility.withSingleAction(this,"Ulang", "Terjadi kesalahan koneksi", "network_error.json") {
                    val intent = Intent(this, ActivityMenu::class.java)
                    startActivity(intent)
                }
            }) {
            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params: MutableMap<String, String> = HashMap()
                params[Db.TAG_DEVICEID] = Settings.Secure.getString(this@ActivityMonitor.contentResolver, Settings.Secure.ANDROID_ID)
                return params
            }
        }
        Volley.newRequestQueue(this).add(strReq)
    }
}