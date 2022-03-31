package com.example.romancemaker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.gauravk.audiovisualizer.visualizer.BarVisualizer
import java.lang.StringBuilder
import java.lang.Thread.sleep
import java.text.FieldPosition
import kotlin.properties.Delegates

class PlayerActivity : AppCompatActivity() {
    lateinit var playbtn: Button ;lateinit var nextbtn: Button ;lateinit var prevbtn: Button
    lateinit var ffbtn: Button ;lateinit var frbtn: Button; lateinit var txtname: TextView
    lateinit var txtstart: TextView; lateinit var txtstop: TextView; lateinit var seekmusic: SeekBar
    lateinit var visualizer: BarVisualizer; lateinit var imageView: ImageView;
    lateinit var updateSeekBar: Thread

    lateinit var sname: String
    var mediaPlayer: MediaPlayer? = null
    lateinit var musicList : ArrayList<String>
    lateinit var idList : ArrayList<Long>
    var position = 0

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        visualizer?.release()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        supportActionBar?.title = "Now Playing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        playbtn = findViewById(R.id.playbtn); nextbtn = findViewById(R.id.nextbtn)
        prevbtn = findViewById(R.id.prevbtn); ffbtn = findViewById(R.id.ffbtn)
        frbtn = findViewById(R.id.frbtn); txtname = findViewById(R.id.txtsn)
        txtstart = findViewById(R.id.txtstart); txtstop = findViewById(R.id.txtstop)
        seekmusic = findViewById(R.id.seekbar); visualizer = findViewById(R.id.blast)

        seekmusic = findViewById(R.id.seekbar); visualizer = findViewById(R.id.blast)
        imageView = findViewById(R.id.imageview);

        mediaPlayer?.stop()
        mediaPlayer?.release()

        musicList = intent.extras?.get("songs") as ArrayList<String>
        idList = intent.extras?.get("ids") as ArrayList<Long>
        position = intent.getIntExtra("pos", 0)

        txtname.isSelected = true
        val uri = Uri.withAppendedPath(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            idList[position].toString()
        )
        sname = musicList[position]
        txtname.text = sname

        Log.i(ContentValues.TAG, "uri: $uri")
        mediaPlayer = MediaPlayer.create(applicationContext, uri)
        mediaPlayer?.start()

        updateSeekBar = Thread(){
            val totalDuration = mediaPlayer!!.duration
            var position_cur: Int = 0

            while (position_cur < totalDuration){
                try {
                    sleep(500)
                    position_cur = mediaPlayer!!.currentPosition
                    seekmusic.setProgress(position_cur)
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        seekmusic.max = mediaPlayer!!.duration
        updateSeekBar.start()
        seekmusic.progressDrawable.setColorFilter(resources.getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY)
        seekmusic.thumb.setColorFilter(resources.getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN)

        seekmusic.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                mediaPlayer?.seekTo(p0!!.progress)
            }
        })

        val endTime = createTime(mediaPlayer!!.duration)
        txtstop.text = endTime

        val delay: Int = 1000

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(object: Runnable {
            override fun run() {
                txtstart.text = createTime(mediaPlayer!!.currentPosition)
                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())

        playbtn.setOnClickListener {
            if(mediaPlayer!!.isPlaying){
                playbtn.setBackgroundResource(R.drawable.ic_play)
                mediaPlayer?.pause()
            }else{
                playbtn.setBackgroundResource(R.drawable.ic_pause)
                mediaPlayer?.start()
            }
        }

        mediaPlayer?.setOnCompletionListener {
            nextbtn.performClick()
        }

        val audioSessionId = mediaPlayer!!.audioSessionId
        if(audioSessionId != -1){
            visualizer.setAudioSessionId(audioSessionId)
        }

        nextbtn.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()

            position = ((position+1) % musicList.size)
            val u = Uri.withAppendedPath(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                idList[position].toString()
            )

            sname = musicList[position]
            txtname.text = sname
            mediaPlayer = MediaPlayer.create(applicationContext, u)

            mediaPlayer?.start()
            playbtn.setBackgroundResource(R.drawable.ic_pause)

            val endTime1 = createTime(mediaPlayer!!.duration)
            txtstop.text = endTime1

            startAnimation(imageView)

            val audioSessionId = mediaPlayer!!.audioSessionId
            if(audioSessionId != -1){
                visualizer.setAudioSessionId(audioSessionId)
            }
        }

        prevbtn.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()

            position = ((position+musicList.size-1) % musicList.size)
            val u = Uri.withAppendedPath(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                idList[position].toString()
            )

            sname = musicList[position]
            txtname.text = sname
            mediaPlayer = MediaPlayer.create(applicationContext, u)

            mediaPlayer?.start()
            playbtn.setBackgroundResource(R.drawable.ic_pause)

            val endTime2 = createTime(mediaPlayer!!.duration)
            txtstop.text = endTime2

            startAnimation(imageView)

            val audioSessionId = mediaPlayer!!.audioSessionId
            if(audioSessionId != -1){
                visualizer.setAudioSessionId(audioSessionId)
            }
        }

        ffbtn.setOnClickListener {
            if(mediaPlayer!!.isPlaying){
                mediaPlayer?.seekTo(mediaPlayer!!.currentPosition+10000)
            }
        }

        frbtn.setOnClickListener {
            if(mediaPlayer!!.isPlaying){
                mediaPlayer?.seekTo(mediaPlayer!!.currentPosition-10000)
            }
        }
    }

    fun startAnimation(view: View){
        val animator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        animator.duration = 1000
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator)
        animatorSet.start()
    }

    fun createTime(duration: Int): String{
        var time = StringBuilder()
        val min = duration/1000/60
        val sec = duration/1000%60

        time.append(min).append(":")

        if(sec < 10){
            time.append("0")
        }
        time.append(sec)

        return time.toString()
    }
}