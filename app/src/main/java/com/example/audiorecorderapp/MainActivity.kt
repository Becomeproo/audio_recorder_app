package com.example.audiorecorderapp

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audiorecorderapp.databinding.ActivityMainBinding
import java.io.IOException

// 5. 파형 그리기 - 인터페이스 상속
class MainActivity : AppCompatActivity(), OnTimerTickListener {
    private lateinit var binding: ActivityMainBinding

    // 5. 파형 그리기
    private lateinit var timer: Timer

    // 3. 녹음 기능 추가
    private var recorder: MediaRecorder? = null
    private var fileName: String = ""
    private var state: State = State.RELEASE

    // 4. 재생 기능 추가
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. 녹음 기능 추가
        fileName = "${externalCacheDir?.absoluteFile}/audiorecordtest.3gp"
        timer = Timer(this)

        // 3. 녹음 기능 구현 - onRecord() 메서드 추가
        binding.recordButton.setOnClickListener {
            when (state) {
                State.RELEASE -> record()
                State.RECORDING -> onRecord(false)
                State.PLAYING -> {}
            }
        }

        // 4. 재생 기능 구현
        binding.playButton.setOnClickListener {
            when (state) {
                State.RELEASE -> onPlay(true)
                else -> {}
            }
        }

        binding.stopButton.setOnClickListener {
            when (state) {
                State.PLAYING -> onPlay(false)
                else -> {}
            }
        }
    }

    // 2. 오디오 권한 요청
    private fun record() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                AUDIO_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
                onRecord(true)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, AUDIO_PERMISSION) -> {
                showPermissionRationalDialog()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(AUDIO_PERMISSION),
                    AUDIO_PERMISSION_CODE
                )
            }
        }
    }

    // 3. 녹음 기능 구현
    private fun onRecord(start: Boolean) = if (start) startRecording() else stopRecording()

    // 4. 재생 기능 구현
    private fun onPlay(start: Boolean) = if (start) startPlaying() else stopPlaying()

    // 3. 녹음 기능 구현
    private fun startRecording() {
        state = State.RECORDING

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("APP", "prepare() failed $e")
            }

            start()
        }

        // 5. 파형 그리기
        // 6. 파형 그리기2 - 녹음 기능이 시작될 때 이전 녹음 기록 삭제
        binding.waveformView.clearData()
        timer.start()

        binding.recordButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_baseline_stop_24
            )
        )
        binding.recordButton.imageTintList = ColorStateList.valueOf(Color.BLACK)
        binding.playButton.isEnabled = false
        binding.playButton.alpha = 0.3f
    }

    // 3. 녹음 기능 구현
    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }

        recorder = null

        // 5. 파형 그리기
        timer.stop()

        state = State.RELEASE

        binding.recordButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_baseline_fiber_manual_record_24
            )
        )
        binding.recordButton.imageTintList = ColorStateList.valueOf(getColor(R.color.red))
        binding.playButton.isEnabled = true
        binding.playButton.alpha = 1.0f
    }

    // 4. 재생 기능 구현
    // 6. 파형 그리기2 - 녹음된 파형 재생을 위한 타이머 추가
    private fun startPlaying() {
        state = State.PLAYING

        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
            } catch (e: IOException) {
                Log.e("APP", "mediaPlayer prepare() failed $e")
            }
            start()
        }

        binding.waveformView.clearWave()
        timer.start()

        player?.setOnCompletionListener {
            stopPlaying()
        }

        binding.recordButton.apply {
            isEnabled = false
            alpha = 0.3f
        }
    }

    // 4. 재생 기능 구현
    // 6. 파형 그리기2 - 녹음된 파형 정지를 위한 타이머 추가
    private fun stopPlaying() {
        state = State.RELEASE

        player?.release()
        player = null

        timer.stop()

        binding.recordButton.apply {
            isEnabled = true
            alpha = 1.0f
        }
    }


    // 2. 오디오 권한 요청
    private fun showPermissionRationalDialog() {
        AlertDialog.Builder(this).apply {
            setMessage("녹음 기능을 위한 권한이 필요합니다.")
            setPositiveButton("권한 허용") { _, _ ->
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(AUDIO_PERMISSION),
                    AUDIO_PERMISSION_CODE
                )
            }
            setNegativeButton("거부") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
        }.show()
    }

    // 2. 오디오 권한 요청
    private fun showPermissionSettingDialog() {
        AlertDialog.Builder(this).apply {
            setMessage("녹음 권한을 허용하지 않을 경우, 앱을 사용할 수 없습니다. 앱 설정에서 녹음 권한을 허용해 주셔야 합니다.")
            setPositiveButton("권한 허용") { _, _ ->
                navigateToAppSetting()
            }
            setNegativeButton("거부") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
        }.show()
    }

    // 2. 오디오 권한 요청
    private fun navigateToAppSetting() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    // 2. 오디오 권한 요청
    // 3. 녹음 기능 - onRecord() 메서드 추가
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val audioRecordPermissionGranted =
            requestCode == AUDIO_PERMISSION_CODE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (audioRecordPermissionGranted) {
            onRecord(true)
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, AUDIO_PERMISSION)) {
                showPermissionRationalDialog()
            } else {
                showPermissionSettingDialog()
            }
        }
    }

    // 5. 파형 그리기
    // 6. 파형 그리기2 - timerTextView 추가
    override fun onTick(duration: Long) {
        val milliSecond = duration % 1000
        val second = (duration / 1000) % 60
        val minute = (duration / 1000) / 60

        binding.timerTextView.text = String.format("%02d:%02d.%02d", minute, second, milliSecond / 10)

        if (state == State.PLAYING) {
            binding.waveformView.replayAmplitude(duration.toInt())
        } else if (state == State.RECORDING) {
            binding.waveformView.addAmplitude(recorder?.maxAmplitude?.toFloat() ?: 0f)
        }
    }


    // 3. 녹음 기능 - 3가지 상태로 분류(릴리즈, 녹음중, 재생중)
    private enum class State {
        RELEASE, RECORDING, PLAYING
    }

    // 2. 오디오 권한 요청
    companion object {
        const val AUDIO_PERMISSION_CODE = 100
        const val AUDIO_PERMISSION = android.Manifest.permission.RECORD_AUDIO
    }
}