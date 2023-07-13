package com.pnj.vaksin_firebase.pasien

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.pnj.vaksin_firebase.MainActivity
import com.pnj.vaksin_firebase.R
import com.pnj.vaksin_firebase.databinding.ActivityAddPasienBinding
import java.io.ByteArrayOutputStream
import java.util.Calendar

class AddPasienActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPasienBinding
    private val firestoreDatabase = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri: Uri
    private var dataGambar : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddPasienBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtAddTglLahir.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener{ view, year, monthOfYear, dayOfMonth ->
                    binding.TxtAddTglLahir.setText("" + year + "-" + monthOfYear + "-" + dayOfMonth)
                }, year, month, day)

            dpd.show()
        }

        binding.BtnAddPasien.setOnClickListener {
            addPasien()
        }

        binding.BtnImgPasien.setOnClickListener {
            openCamera()
        }
    }

    fun addPasien() {
        var nik : String = binding. TxtAddNIK.text.toString()
        var nama : String = binding.TxtAddNama.text.toString()
        var tgl_lahir : String = binding.TxtAddTglLahir.text.toString()

        var jk : String = ""
        if(binding.RdnEditJKL.isChecked) {
            jk = "Laki - laki"
        }
        else if(binding.RdnEditJKP.isChecked) {
            jk = "Perempuan"
        }

        var penyakit = ArrayList<String>()
        if(binding.ChkDiabetes.isChecked) {
            penyakit.add("diabetes")
        }
        if(binding.ChkJantung.isChecked) {
            penyakit.add("jantung")
        }
        if(binding.ChkAsma.isChecked) {
            penyakit.add("asma")
        }

        val penyakit_string = penyakit.joinToString("|")

        // masukin ke data class
        val pasien: MutableMap<String, Any> = HashMap()
        pasien["nik"] = nik
        pasien["nama"] = nama
        pasien["tgl_lahir"] = tgl_lahir
        pasien["jenis_kelamin"] = jk
        pasien["penyakit_bawaan"] = penyakit_string

//        firestoreDatabase.collection("pasien").add(pasien)
//            .addOnSuccessListener {
//                val intentMain = Intent(this, MainActivity::class.java)
//                startActivity(intentMain)
//            }

//        binding.BtnAddPasien.setOnClickListener {
//            addPasien()
//        } // ngasih error duplikat

        if (dataGambar != null) {
            // upload gambar ke storage
            uploadPictFirebase(dataGambar!!, "${nik}_${nama}")

            // upload data classnya ke firebase
            firestoreDatabase.collection("pasien").add(pasien)
                .addOnSuccessListener {
                    val intentMain = Intent(this, MainActivity::class.java)
                    startActivity(intentMain)
                }
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgPasien.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_pasien/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let { Uri ->
                            imgUri = Uri
                            binding.BtnImgPasien.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }
}