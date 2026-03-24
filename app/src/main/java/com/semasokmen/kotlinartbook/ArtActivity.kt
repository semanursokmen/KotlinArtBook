package com.semasokmen.kotlinartbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.semasokmen.kotlinartbook.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap: Bitmap? = null
    private lateinit var database: SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")) {
            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.imageView.setImageResource(R.drawable.selectimage)
            binding.button.visibility = View.VISIBLE



        } else {
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            //? -> selectedId ile eşleştiriliyor.
            val cursor = database.rawQuery("SELECT*FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
//bu sefer tüm verileri çekilir:
            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                //imageview icin:
                //kullanıcıya göstermek istiyosak: bitmap
                //databasede saklamak istiyosak: bytearray
                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)



            }
            cursor.close()
        }

    }

    fun save(view: View) {
    //sqlite kaydetmek için görseli küçültmek gereklidir.
    //yerel veri tabanlarına kıyasla cloud sunucuya kaydetmek telefon hafızasında daha az yer kaplar.
    //SQLite içerisinde 1 MB aşan raw oluşturulamaz.
        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()

        if(selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {

                //val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null) başka yerde de kullanılacağı için oncreate'e aldık
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR,image BLOB)")
                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            } catch (e: Exception) {

            }

            val intent = Intent (this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int) : Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = ((width / height).toDouble())
        if (bitmapRatio>1) {
            //landscape görsel
            width = maximumSize
            val scaledHeight = width/bitmapRatio
            height = scaledHeight.toInt()
        } else {
            //portrait görsel
            height = maximumSize
            val scaledWidth = height*bitmapRatio
            width = scaledWidth.toInt()

        }

        return Bitmap.createScaledBitmap(image,width,height,true)

    }

    fun selectImage(view: View) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Android sdk>33 -> READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //rationale: mantığı gösterdikten sonra izin istenir
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                ) {
                    Snackbar.make(
                        view,
                        "Permission needed for gallery!",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give permission", View.OnClickListener {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()

                } else {
                    //request permission: direkt mantığı göstermeden izin istenir.
                    //bu iki kavama android kendi karar verir. (direkt mi yoksa önce mantık sonra izin mi)
                    //bizim daha önceden izin verildiğine dair kontrol etmemize gerek yok.
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    println("sema3")

                }


            } else {

                val intentToGallery =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //intent:
                activityResultLauncher.launch(intentToGallery)
                println("sema2")


            }


        } else {
            //Android sdk<33 -> READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //rationale: mantığı gösterdikten sonra izin istenir
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    Snackbar.make(
                        view,
                        "Permission needed for gallery!",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give permission", View.OnClickListener {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()

                } else {
                    //request permission: direkt mantığı göstermeden izin istenir.
                    //bu iki kavama android kendi karar verir. (direkt mi yoksa önce mantık sonra izin mi)
                    //bizim daha önceden izin verildiğine dair kontrol etmemize gerek yok.
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            else {

                    val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                //intent:
                activityResultLauncher.launch(intentToGallery)
                println("sema")


            }

        }


    }

    private fun registerLauncher() {
        //galeriye gitmek ve galeriden görseli seçmek ile ilgili işlemler:
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        val imageData = intentFromResult.data
                        //binding.imageView.setImageURI(imageData)
                        //Bitmap: Görselleri tutmak için oluşturulan obje (android içinde oluşturulmuş görselin kendisi).
                        //URI (Uniform Resource Identifier): bir kaynağı benzersiz şekilde tanımlayan karakter dizisi.
                        // Bitmap i SQLite a kaydedicez. Kaydetmeden önce küçültme işlemini uygulayacağız.

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(
                                    this@ArtActivity.contentResolver,
                                    imageData!!
                                )
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            } else {
                                selectedBitmap =
                                    MediaStore.Images.Media.getBitmap(contentResolver, imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                                println("sema")

                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }


        //izni istemek:

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->

                if (result) {
                    //permission granted
                    //galeriye intent:
                    val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)

                } else {
                    //permission denied

                    Toast.makeText(this@ArtActivity, "Permission needed!", Toast.LENGTH_LONG).show()
                }

            }
    }
}

