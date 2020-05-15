package com.srsssms.covidtracker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_karantina.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@Suppress("KDocUnresolvedReference")
class Karantina : AppCompatActivity() {


    @Suppress("UNNECESSARY_SAFE_CALL")
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_karantina)

        btDaftar.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (Objects.requireNonNull(imm).isAcceptingText) {
                imm.hideSoftInputFromWindow(Objects.requireNonNull(currentFocus)?.windowToken, 0)
            }
            if (etNama.text.isEmpty() || etAlamat.text.isEmpty()){
                AlertDialogUtility.alertDialog(this, "Diwajibkan untuk mengisi kolom Nama dan Alamat!", "warning.json")
            } else {
                try {
                    AlertDialogUtility.withTwoActions(this, "BELUM", "SUDAH", "Apakah anda yakin Nama:${etNama.text} dan Alamat:${etAlamat.text} sudah benar?", "warning.json"){
                        Log.d("registerdebug", "Device ID:${Settings.Secure.getString(this@Karantina.contentResolver, Settings.Secure.ANDROID_ID)} || Nama:${etNama.text} || Alamat:${etAlamat.text}")
                        checkRegister(etNama.text.toString(), etAlamat.text.toString())
                        CURL(etNama.text.toString().replace(" ", "%20"), etAlamat.text.toString().replace(" ", "%20")).execute()
                    }
                } catch (e: Exception) {
                    Toasty.error(this, "Terjadi kesalahan, hubungi pengembang", Toasty.LENGTH_LONG, true).show()
                }
            }
        }
    }

    override fun onBackPressed() {
    }

    private fun checkRegister(namaLengkap: String, alamat: String) {
        progressBarHolderRegister.visibility = View.VISIBLE
        @Suppress("UNUSED_ANONYMOUS_PARAMETER") val strReq: StringRequest = object : StringRequest(Method.POST, "${Db.TAG_URL}register.php", Response.Listener { response ->
            try {
                val jObj = JSONObject(response)
                val success = jObj.getInt(Db.TAG_SUCCESS)

                if (success == 1) {
                    progressBarHolderRegister.visibility = View.GONE
                    AlertDialogUtility.withSingleAction(this,"Masuk","Data telah masuk!","success.json"){
                        val intent = Intent(this@Karantina, Splash::class.java)
                        startActivity(intent)
                    }
                } else {
                    progressBarHolderRegister.visibility = View.GONE
                    Toast.makeText(applicationContext, jObj.getString(Db.TAG_MESSAGE), Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                AlertDialogUtility.withSingleAction(this,"Ulang", "Error: $e", "warning.json") {
                    val intent = Intent(this@Karantina, Splash::class.java)
                    startActivity(intent)
                }
                progressBarHolderRegister.visibility = View.GONE
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            AlertDialogUtility.withSingleAction(this,"Ulang", "Registrasi gagal, gunakan jaringan yang stabil untuk registrasi!", "network_error.json") {
                val intent = Intent(this@Karantina, Karantina::class.java)
                startActivity(intent)
            }
            progressBarHolderRegister.visibility = View.GONE
        }) {
            @SuppressLint("HardwareIds")
            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params: MutableMap<String, String> = HashMap()
                params[Db.TAG_NAMA] = namaLengkap
                params[Db.TAG_ALAMAT] = alamat
                params[Db.TAG_DEVICEID] = Settings.Secure.getString(this@Karantina.contentResolver, Settings.Secure.ANDROID_ID)
                return params
            }
        }
        // Adding request to request queue
        val queue = Volley.newRequestQueue(this)
        queue.add(strReq)
    }

    class CURL(
        etNamaLengkap: String,
        etDepartemen: String
    ) : AsyncTask<Void, Void, String>() {

        private val namaLengkap = "Nama=$etNamaLengkap"
        private val departemen = "%0ADepartemen=$etDepartemen"
        private var returnValue: Int? = null
        private val baseURL = "https://api.telegram.org/bot1115531097:AAHOgChELkW3Kk2PtC1VvOt4BbhlZYju8l8/sendMessage?parse_mode=markdown&chat_id=-397663601&text="
        private val command = "curl POST $baseURL$namaLengkap$departemen"
        private val process: Process = Runtime.getRuntime().exec(command)

        override fun doInBackground(vararg params: Void?): String? {
            process.inputStream.read()
            process.waitFor()
            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(result: String?) {
            process.inputStream.close()
            returnValue = process.exitValue()
            super.onPostExecute(result)
        }
    }
}