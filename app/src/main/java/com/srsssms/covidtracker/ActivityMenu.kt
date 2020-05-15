package com.srsssms.covidtracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.BuildConfig
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_menu.*
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("MemberVisibilityCanBePrivate", "KDocUnresolvedReference")
class ActivityMenu : AppCompatActivity() {

    /**
     * [location] */
    var mFusedLocationClient: FusedLocationProviderClient? = null
    var mSettingsClient: SettingsClient? = null
    var mLocationRequest: LocationRequest? = null
    var mLocationSettingsRequest: LocationSettingsRequest? = null
    var mLocationCallback: LocationCallback? = null
    var mCurrentLocation: Location? = null
    var mRequestingLocationUpdates: Boolean? = true
    var mLastUpdateTime: String? = null
    var lat: Double? = null
    var dist: Double? = null
    var lon: Double? = null
    val radius = 15.0

    /**
     * [NOTIFICATION] */
    private val fcmAPI = "https://fcm.googleapis.com/fcm/send"
    @Suppress("SpellCheckingInspection")
    private val serverKey =
        "key=" + "AAAANTO6s7k:APA91bERL6Mfh5vbz__nqQedaDq_Gvd29bZwFUd8VzHy3HWV2SJQJWmWDwL4ocHLFqKzaOndqzfG-WQo9QzvTPiCkawDKsCq9mC9HP-e8QXjfu0AhW6tpQwqcDnxA7nW6RFUG-UfvX6m"
    private val contentType = "application/json"
    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this.applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        val prefMan = PrefManager(this)
        tvNamaMain.text = prefMan.name
        if (prefMan.statusAkun == 2){
            btKarantina.visibility = View.GONE
            btMonitor.visibility = View.GONE
            AlertDialogUtility.alertDialog(this, "Terima kasih telah berpartisipasi dalam pengendalian Covid 19!!", "success.json")
        } else if (prefMan.statusAkun <= 3){
            btMonitor.visibility = View.GONE
        }

        updateValuesFromBundle(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        btKarantina.setOnClickListener {
            AlertDialogUtility.withTwoActions(this,"nanti","daftarkan","Anda harus berada di luar ruangan ketika mengaktifkan fitur ini. Apakah anda ingin mendaftarkan lokasi anda?", "checking.json"){
                if (lat != null && lon != null){
                    registerLocation()
                } else {
                    AlertDialogUtility.alertDialog(this,"Lokasi belum terkunci, tunggu sampai indikator lokasi berubah menjadi hijau, lalu ulangi kembali", "warning.json")
                }
            }
        }
        btMonitor.setOnClickListener {
            val intent = Intent(this@ActivityMenu, ActivityMonitor::class.java)
            startActivity(intent)
        }

        if (prefMan.logged){
            createLocationCallback()
            createLocationRequest()
            buildLocationSettingsRequest()
            mRequestingLocationUpdates = true
        }

        dirumahaja.setOnClickListener {
            triggerNotification()
        }
    }

    private fun sendNotification(notification: JSONObject) {
        Log.d("notifdebug", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(fcmAPI, notification,
            Response.Listener { response ->
                Log.i("notifdebug", "onResponse: $response")
                Toasty.success(this, "Berhasil").show()
            },
            Response.ErrorListener {
                Toast.makeText(this@ActivityMenu, "Request error", Toast.LENGTH_LONG).show()
                Log.d("notifdebug", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    fun triggerNotification(){
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/alert")
        val topic = "/topics/alert" //topic has to match what the receiver subscribed to
        val notification = JSONObject()
        val notifcationBody = JSONObject()
        try {
            notifcationBody.put("title", "${PrefManager(this).name} tidak berada di dalam rumah")
            notifcationBody.put("message", "${PrefManager(this).name} berada ${dist}m dari rumahnya")   //Enter your notification message
            notification.put("to", topic)
            notification.put("data", notifcationBody)
            Log.d("notifdebug", "try")
        } catch (e: JSONException) {
            Log.d("notifdebug", "onCreate: " + e.message)
        }
        sendNotification(notification)
    }

    fun execute(){
        startLocationUpdates()
    }

    override fun onBackPressed() {

    }

    @Suppress("LocalVariableName")
    fun distance(latA:Double, lonA:Double, latB: Double, lonB: Double):Double{
        val R = 6371e3 // metres
        val phase1 = latA * Math.PI/180 // φ, λ in radians
        val phase2 = latB * Math.PI/180
        val deltaPhase = (latB-latA) * Math.PI/180
        val deltaLambda = (lonB-lonA) * Math.PI/180

        val a = sin(deltaPhase/2) * sin(deltaPhase/2) +
                cos(phase1) * cos(phase2) *
                sin(deltaLambda/2) * sin(deltaLambda/2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return R * c
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun trackLocation(namaLengkap: String, latitude: Double, longitude: Double,
                              waktu: String, alamat: String) {
        triggerNotification()
        val strReq: StringRequest = object : StringRequest(Method.POST, "https://covid.srs-ssms.com/apk/track.php", Response.Listener { response ->
            try {
                val jObj = JSONObject(response)
                val success = jObj.getInt(Db.TAG_SUCCESS)
                if (success == 1) {
                    Toasty.error(this, "Anda berjalan terlalu jauh dari tempat tinggal anda (${dist}m))", Toasty.LENGTH_LONG).show()
                } else{
                    Toast.makeText(applicationContext,
                        jObj.getString(success.toString()), Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                Toast.makeText(this@ActivityMenu, "error response $e", Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            Toasty.error(this, "Tidak ada internet! Anda berjalan terlalu jauh dari tempat tinggal anda (${dist}m))", Toasty.LENGTH_LONG).show()
        }) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params[Db.TAG_NAMA] = namaLengkap
                params[Db.TAG_WAKTU] = waktu
                params[Db.TAG_LATITUDE] = latitude.toString()
                params[Db.TAG_LONGITUDE] = longitude.toString()
                params[Db.TAG_ALAMAT] = alamat
                return params
            }
        }
        // Adding request to request queue
        val queue = Volley.newRequestQueue(this)
        queue.add(strReq)
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    KEY_REQUESTING_LOCATION_UPDATES
                )
            }
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            }
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING)
            }
            updateUI()
        }
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult.lastLocation
                mLastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
    }
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i(
                    TAG,
                    "User agreed to make required location settings changes."
                )
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                    updateUI()
                }
            }
        }
    }
    private fun startLocationUpdates() {
        mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this@ActivityMenu) {
                Log.i(TAG, "All location settings are satisfied.")
                mFusedLocationClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()
                )
                updateUI()
            }
            .addOnFailureListener(this@ActivityMenu) { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings "
                        )
                        try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(this@ActivityMenu, REQUEST_CHECK_SETTINGS)
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate, and cannot be " +
                                "fixed here. Fix in Settings."
                        Log.e(TAG, errorMessage)
                        Toast.makeText(this@ActivityMenu, errorMessage, Toast.LENGTH_LONG).show()
                        mRequestingLocationUpdates = false
                    }
                }
                updateUI()
            }
    }

    private fun updateUI() {
        updateLocationUI()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun updateLocationUI() {
        val prefManager = PrefManager(this)
        if (mCurrentLocation != null){
            /*Toasty.success(this, "Base location(${prefManager.latA},${prefManager.latA})", Toasty.LENGTH_SHORT).show()*/
            val dateFormatted = SimpleDateFormat("yyyy-M-dd HH:mm:ss").format(Calendar.getInstance().time)
            lat = String.format(
                Locale.ENGLISH, "%s",
                mCurrentLocation!!.latitude).toDouble()
            lon = String.format(
                Locale.ENGLISH, "%s",
                mCurrentLocation!!.longitude).toDouble()
            dist = distance(prefManager.latA.toDouble(), prefManager.lonA.toDouble(), lat!!.toDouble(), lon!!.toDouble())
            locationIndicator.setImageResource(R.drawable.ic_location_on_black_24dp)
            locationIndicator.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.green_basiccolor))
            if (prefManager.latA != 0f && dist!! > radius){
                trackLocation(prefManager.name!!, lat!!, lon!!, dateFormatted, prefManager.alamat!!)
            }
        }
    }

    @Suppress("unused")
    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates!!) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.")
            return
        }
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(this) {
                mRequestingLocationUpdates = false
            }
    }

    public override fun onResume() {
        super.onResume()
        if (PrefManager(this).logged){
            if (!mRequestingLocationUpdates!! && checkPermissions()) {
                startLocationUpdates()
            } else if (!checkPermissions()) {
                requestPermissions()
            }
            updateUI()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(
            KEY_REQUESTING_LOCATION_UPDATES,
            mRequestingLocationUpdates!!
        )
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation)
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime)
        super.onSaveInstanceState(savedInstanceState)
    }

    @Suppress("SameParameterValue")
    private fun showSnackbar(
        mainTextStringId: Int, actionStringId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mainTextStringId.toString(),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(actionStringId), listener).show()
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this@ActivityMenu,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this@ActivityMenu,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            ActivityCompat.requestPermissions(
                this@ActivityMenu, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            Log.i(TAG, "Requesting permission")
            ActivityCompat.requestPermissions(
                this@ActivityMenu, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates!!) {
                    Log.i(
                        TAG,
                        "Permission granted, updates requested, starting location updates"
                    )
                    startLocationUpdates()
                }
            } else {
                showSnackbar(R.string.permission_denied_explanation,
                    R.string.settings, View.OnClickListener {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
            }
        }
    }

    companion object {
        private val TAG = Karantina::class.java.simpleName
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_CHECK_SETTINGS = 0x1
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private const val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
        private const val KEY_LOCATION = "location"
        private const val KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string"
    }

    @SuppressLint("HardwareIds", "CheckResult")
    private fun registerLocation() {
        val hid = Settings.Secure.getString(this@ActivityMenu.contentResolver, Settings.Secure.ANDROID_ID)
        Toasty.success(this, "lat:$lat || lon:$lon || id:${hid}")
        progressBarHolderMain.visibility = View.VISIBLE
        @Suppress("UNUSED_ANONYMOUS_PARAMETER") val strReq: StringRequest = object : StringRequest(Method.POST, "${Db.TAG_URL}loc.php", Response.Listener { response ->
            try {
                val jObj = JSONObject(response)
                val success = jObj.getInt(Db.TAG_SUCCESS)
                // Check for error node in json
                if (success == 1) {
                    progressBarHolderMain.visibility = View.GONE
                    AlertDialogUtility.alertDialog(this, "Data telah masuk!", "success.json")
                    PrefManager(this).latA = lat!!.toFloat()
                    PrefManager(this).lonA = lon!!.toFloat()
                } else {
                    progressBarHolderMain.visibility = View.GONE
                    Toast.makeText(applicationContext, jObj.getString(Db.TAG_MESSAGE), Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                AlertDialogUtility.withSingleAction(this,"Ulang", "Error: $e || lat:$lat || lon:$lon || id:${Settings.Secure.getString(this@ActivityMenu.contentResolver, Settings.Secure.ANDROID_ID)}", "warning.json") {
                    val intent = Intent(this@ActivityMenu, Splash::class.java)
                    startActivity(intent)
                }
                progressBarHolderMain.visibility = View.GONE
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            AlertDialogUtility.withSingleAction(this,"Ulang", "Registrasi gagal, gunakan jaringan yang stabil untuk registrasi!", "network_error.json") {
                val intent = Intent(this@ActivityMenu, ActivityMenu::class.java)
                startActivity(intent)
            }
            progressBarHolderMain.visibility = View.GONE
        }) {
            @SuppressLint("HardwareIds")
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params[Db.TAG_LATA] = lat.toString()
                params[Db.TAG_LONA] = lon.toString()
                params[Db.TAG_DEVICEID] = hid
                params[Db.TAG_STATUSAKUN] = "2"
                PrefManager(this@ActivityMenu).statusAkun = 2
                return params
            }
        }
        // Adding request to request queue
        val queue = Volley.newRequestQueue(this)
        queue.add(strReq)
    }
}
