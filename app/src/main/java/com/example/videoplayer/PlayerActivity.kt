package com.example.videoplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.databinding.BoosterBinding
import com.example.videoplayer.databinding.MoreFeaturesBinding
import com.example.videoplayer.databinding.SpeedLayoutBinding
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.system.exitProcess

class PlayerActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerBinding
    private lateinit var runnable: Runnable
    private var moreTime:Int = 0

    companion object {
        lateinit var player: ExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        private var repeat: Boolean = false
        private var isFullScreen: Boolean = false
        private var isLocked: Boolean = false
        @SuppressLint("StaticFieldLeak")
        lateinit var trackSelecter : DefaultTrackSelector
        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed: Float = 1.0f
        private var timer : Timer? = null
        var pipStatus:Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        initializeLayout()
        initializeBinding()
        supportActionBar?.hide()

        binding.fastForwardFl.setOnClickListener(DoubleClickListner(callback = object: DoubleClickListner.Callback{
            override fun doubleClicked() {
                binding.playerView.showController()
                binding.fastForwardBtn.visibility = View.VISIBLE
                moreTime = 0
                player.seekTo(player.currentPosition + 10000)

            }
        }))
        binding.fastRewinFl.setOnClickListener(DoubleClickListner(callback = object: DoubleClickListner.Callback{
            override fun doubleClicked() {
                binding.playerView.showController()
                binding.fastBackwardBtn.visibility = View.VISIBLE
                moreTime = 0
                player.seekTo(player.currentPosition - 10000)

            }
        }))

    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private fun initializeLayout() {
        when (intent.getStringExtra("class")) {
            "AllVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()
            }

            "FolderActivity" -> {
                playerList = ArrayList()
                playerList.addAll(FoldersActivity.currentFolderVideos)
                createPlayer()
            }
        }
        if (repeat) binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        else binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)

    }

    @SuppressLint("PrivateResource", "SetTextI18n")
    private fun initializeBinding() {

        binding.backBtn.setOnClickListener {
            finish()
        }
        binding.playPauseButton.setOnClickListener {
            if (player.isPlaying) pauseVideo()
            else playVideo()
        }
        binding.nextBtn.setOnClickListener {
            nextPrevVideo()
        }
        binding.prevBtn.setOnClickListener {
            nextPrevVideo(isNext = false)
        }
        binding.repeatBtn.setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                binding.repeatBtn.setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }
        binding.fullScreenBtn.setOnClickListener {
            if (isFullScreen){
                isFullScreen = false
                playInFullScreen(enable = false)
            }else{
                isFullScreen = true
                playInFullScreen(enable = true)
            }
        }
        binding.lockBtn.setOnClickListener {
            if (!isLocked){
                //for hidding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockBtn.setImageResource(R.drawable.lock_close_icon)
            }
            else{
                //for showing
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockBtn.setImageResource(R.drawable.lock_icon)

            }

        }
        binding.moreFeeaturesBtn.setOnClickListener {
            pauseVideo()
            val customDialoge = LayoutInflater.from(this).inflate(R.layout.more_features,binding.root,false)
            val bindingMf = MoreFeaturesBinding.bind(customDialoge)
            val dialoge = MaterialAlertDialogBuilder(this).setView(customDialoge)
                .setOnCancelListener { playVideo() }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialoge.show()
            bindingMf.audioTrack.setOnClickListener {
                dialoge.dismiss()
                playVideo()

                val audioTrack = ArrayList<String>()
                for (i in 0 until player.currentTrackGroups.length){
                    if (player.currentTrackGroups.get(i).getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT){
                        audioTrack.add(Locale(player.currentTrackGroups.get(i).getFormat(0).language.toString()).displayLanguage)
                    }

                }

                val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))
                MaterialAlertDialogBuilder(this,R.style.alertDialoge)
                    .setTitle("Select Language")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setItems(tempTracks){_, position ->
                        Toast.makeText(this, audioTrack[position]+"Selected", Toast.LENGTH_SHORT).show()
                        trackSelecter.setParameters(trackSelecter.buildUponParameters().setPreferredAudioLanguage(audioTrack[position]))
                    }
                    .create()
                    .show()
            }
            bindingMf.boosterIcon.setOnClickListener {
                dialoge.dismiss()
                val customDialogeB = LayoutInflater.from(this).inflate(R.layout.booster,binding.root,false)
                val bindingB = BoosterBinding.bind(customDialogeB)
                val dialogeB = MaterialAlertDialogBuilder(this).setView(customDialogeB)
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("Ok"){self,_->
                        loudnessEnhancer.setTargetGain(bindingB.verticalSeekBar.progress * 100)
                        playVideo()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogeB.show()
                bindingB.verticalSeekBar.progress  = loudnessEnhancer.targetGain.toInt() /100
                bindingB.progressText.text = "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt()/10} %"
                bindingB.verticalSeekBar.setOnProgressChangeListener {
                    bindingB.progressText.text  = "Audio Boost\n\n${it*10} %"
                }
                playVideo()
            }
            bindingMf.speedBtn.setOnClickListener {
                dialoge.dismiss()
                playVideo()
                val customDialogeS = LayoutInflater.from(this).inflate(R.layout.speed_layout,binding.root,false)
                val bindingS = SpeedLayoutBinding.bind(customDialogeS)
                val dialogeS = MaterialAlertDialogBuilder(this).setView(customDialogeS)
                    .setCancelable(false)
                    .setPositiveButton("Ok"){self,_->
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogeS.show()
                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(isIncrement = false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(isIncrement = true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
            }
            bindingMf.sleepTimerBtn.setOnClickListener {
                dialoge.dismiss()
                if (timer!= null) Toast.makeText(
                    this,
                    "Timer Already Running!\nClose App to Reset Timer!",
                    Toast.LENGTH_SHORT
                ).show()
                else{
                    var sleepTime = 15
                    val customDialogeS = LayoutInflater.from(this).inflate(R.layout.speed_layout,binding.root,false)
                    val bindingS = SpeedLayoutBinding.bind(customDialogeS)
                    val dialogeS = MaterialAlertDialogBuilder(this).setView(customDialogeS)
                        .setCancelable(false)
                        .setPositiveButton("Ok"){self,_->
                            timer = Timer()
                            val task = object :TimerTask(){
                                override fun run() {
                                    moveTaskToBack(true)
                                    exitProcess(1)
                                }
                            }
                            timer!!.schedule(task,sleepTime*60*1000.toLong())
                            self.dismiss()
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0x803700B3.toInt()))
                        .create()
                    dialogeS.show()
                    bindingS.speedText.text = "$sleepTime Min"
                    bindingS.minusBtn.setOnClickListener {
                        if (sleepTime>15) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                    bindingS.plusBtn.setOnClickListener {
                        if (sleepTime<120)sleepTime += 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                }

            }
            bindingMf.pipModeBtn.setOnClickListener {
                val appOps = getSystemService(Context.APP_OPS_SERVICE)as AppOpsManager
                val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE,android.os.Process.myUid(),packageName)== AppOpsManager.MODE_ALLOWED
                } else {
                    false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if (status) {
                        this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        dialoge.dismiss()
                        binding.playerView.hideController()
                        playVideo()
                        pipStatus = 0
                    }
                    else{
                        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS",
                            Uri.parse("package$packageName"))
                        startActivity(intent)
                    }
                }else{
                    Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
                    dialoge.dismiss()
                    playVideo()
                }
            }

        }


    }

    private fun playVideo() {
        binding.playPauseButton.setImageResource(R.drawable.pause_icon)
        player.play()
    }

    private fun pauseVideo() {
        binding.playPauseButton.setImageResource(R.drawable.play_icon)
        player.pause()
    }

    private fun createPlayer() {
        try {
            player.release()
        } catch (e: Exception) {
        }
        speed = 1.0f
        trackSelecter = DefaultTrackSelector(this)
        binding.videoTitle.text = playerList[position].title
        binding.videoTitle.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelecter).build()
        binding.playerView.player = player
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevVideo()
            }
        })
        playInFullScreen(enable = isFullScreen)
        setVisibility()
        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true

    }

    private fun nextPrevVideo(isNext: Boolean = true) {
        if (isNext) setPosition()
        else setPosition(isIncrement = false)
        createPlayer()

    }

    private fun setPosition(isIncrement: Boolean = true) {
        if (!repeat) {
            if (isIncrement) {
                if (playerList.size - 1 == position)
                    position = 0
                else ++position
            } else {
                if (position == 0)
                    position = playerList.size - 1
                else --position
            }
        }
    }

    private fun playInFullScreen(enable: Boolean) {
        if (enable){
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            binding.fullScreenBtn.setImageResource(R.drawable.full_screen_exit_icon)
        }else{
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            binding.fullScreenBtn.setImageResource(R.drawable.full_screen_btn)
        }
    }
    private fun setVisibility(){
        runnable = Runnable {
            if (binding.playerView.isControllerVisible) changeVisibility(View.VISIBLE)
            else changeVisibility(View.INVISIBLE)
            Handler(Looper.getMainLooper()).postDelayed(runnable,0)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable,0)
    }
    private fun changeVisibility(visibility: Int){
        binding.topController.visibility = visibility
        binding.bottomController.visibility = visibility
        binding.playPauseButton.visibility = visibility
        if (isLocked) binding.lockBtn.visibility = View.VISIBLE
        else binding.lockBtn.visibility = visibility

        if (moreTime == 2){
            binding.fastBackwardBtn.visibility = View.INVISIBLE
            binding.fastForwardBtn.visibility = View.INVISIBLE
        }else ++moreTime



    }
    private fun changeSpeed(isIncrement:Boolean){
        if (isIncrement){
            if (speed<= 2.9f){
                speed += 0.10f
            }
        }
        else{
            if (speed>0.20f){
                speed -= 0.10f
            }
        }
        player.setPlaybackSpeed(speed)

    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
        if (pipStatus!=0){
            finish()
            val intent = Intent(this,PlayerActivity::class.java)
            when(pipStatus){
                1-> intent.putExtra("class","FolderActivity")
                2-> intent.putExtra("class","AllVideos")
            }

            startActivity(intent)
        }
    }
}