package com.example.videoplayer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.databinding.ActivityFoldersBinding
import java.io.File
import java.lang.Exception
import kotlin.math.log

class FoldersActivity : AppCompatActivity() {

    companion object{
        lateinit var currentFolderVideos: ArrayList<Video>
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)




        val position = intent.getIntExtra("position",0)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].name

        currentFolderVideos = getAllVideos(MainActivity.folderList[position].id)
        val id = MainActivity.folderList[position].id
        Log.d("pppppp", "onCreate: $id")

        binding.videoRvFa.setHasFixedSize(true)
        binding.videoRvFa.setItemViewCacheSize(10)
        binding.videoRvFa.layoutManager = LinearLayoutManager(this)
        binding.videoRvFa.adapter = VideoAdapter(this, currentFolderVideos , isFolder = true)

        binding.totalVideosFa.text = "Total Videos: ${currentFolderVideos.size}"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    @SuppressLint("Recycle", "Range")
    private fun getAllVideos(folderId: String): ArrayList<Video> {
        val tempList = ArrayList<Video>()
        val selection = MediaStore.Video.Media.BUCKET_ID + " =?"
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
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(folderId),
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        Log.d("llll", "getAllVideos: $cursor")

        if (cursor != null) {
            if (cursor.moveToNext())
                do {
                    val titleC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE))
                    val idC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID))
                    val folderC =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
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


                    }catch (_: Exception){}

                } while (cursor.moveToNext())
            cursor.close()
        }else{
            Log.d("getAllVideos", "Cursor is null")
        }
        return tempList
    }
}