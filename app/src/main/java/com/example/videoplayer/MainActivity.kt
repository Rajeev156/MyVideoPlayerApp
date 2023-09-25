package com.example.videoplayer

import android.Manifest.permission.READ_MEDIA_VIDEO
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.videoplayer.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    lateinit var toggle: ActionBarDrawerToggle
    private lateinit var binding: ActivityMainBinding

    companion object{
        lateinit var videoList: ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //for nav drawer
        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (requestRuntimePermission()){
            folderList = ArrayList()
            videoList = getAllVideos()
            setFragment(VideosFragment())
        }



        getAllVideos()

        binding.bottomNav.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.video_view -> setFragment(VideosFragment())
                R.id.folder_view -> setFragment(FoldersFragment())
            }
            return@setOnItemSelectedListener true
        }
        //on click nav items
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.feedback -> Toast.makeText(this, "feedback", Toast.LENGTH_SHORT).show()
                R.id.themes -> Toast.makeText(this, "themes", Toast.LENGTH_SHORT).show()
                R.id.sort_order -> Toast.makeText(this, "sort", Toast.LENGTH_SHORT).show()
                R.id.about_nav -> Toast.makeText(this, "about", Toast.LENGTH_SHORT).show()
                R.id.exit_nav -> Toast.makeText(this, "exit", Toast.LENGTH_SHORT).show()

            }
            return@setNavigationItemSelectedListener true
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item))
            return true
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestRuntimePermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_MEDIA_VIDEO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(READ_MEDIA_VIDEO), 13)
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
                folderList = ArrayList()
                videoList = getAllVideos()
                setFragment(VideosFragment())
                }
            else
                ActivityCompat.requestPermissions(this, arrayOf(READ_MEDIA_VIDEO), 13)
        }
    }

    private fun setFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_fl, fragment)
        transaction.disallowAddToBackStack()
        transaction.commit()
    }

    @SuppressLint("Recycle", "Range")
    private fun getAllVideos(): ArrayList<Video> {
        val tempList = ArrayList<Video>()
        val tempFolderList = ArrayList<String>()
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )

        val cursor = this.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        if (cursor != null) {
            if (cursor.moveToNext())
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                    val idC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val folderC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                    val folderIdC=
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
                    val sizeC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
                    val pathC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA))
                    val durationC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)).toLong()



                    try {
                        val file = File(pathC)
                        val artUri = Uri.fromFile(file)
                        val video = Video(title = titleC,  id = idC, folderName = folderC, duration = durationC, size = sizeC, path = pathC, artUri = artUri)
                        if (file.exists()) tempList.add(video)

                        //for adding folder
                        if (!tempFolderList.contains(folderC)){
                            tempFolderList.add(folderC)
                            folderList.add(Folder(id = folderIdC, name = folderC))

                        }
                    }catch (e: Exception){}

                } while (cursor.moveToNext())
            cursor.close()
        }
        return tempList
    }
}