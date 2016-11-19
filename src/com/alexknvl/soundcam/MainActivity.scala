package com.alexknvl.soundcam

import spire.syntax.cfor._

import android.hardware.Camera
import android.app.Activity
import android.widget.FrameLayout
import android.os.Bundle
import android.media.{ AudioTrack, AudioFormat, AudioManager }

final class AudioStreamer {
  import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D

  final val sampleRate = 10000
  final val channel = AudioFormat.CHANNEL_OUT_MONO
  final val format = AudioFormat.ENCODING_PCM_16BIT
  final val minBufferSize = 10000//AudioTrack.getMinBufferSize(sampleRate, channel, format)

  final val audio = new AudioTrack(AudioManager.STREAM_MUSIC,
    sampleRate, channel, format, sampleRate, AudioTrack.MODE_STREAM)
  audio.play()

  final val spectrum = new Array[Float](minBufferSize * 2)
  final val fft = new FloatFFT_1D(minBufferSize)
  final val buffer = new Array[Float](minBufferSize * 2)
  final val outputBuffer = new Array[Short](minBufferSize)

  def update(): Unit = {
    generateSignal()
    audio.write(outputBuffer, 0, outputBuffer.length)
  }

  def generateSignal(): Unit = {
    val spectrum = this.spectrum
    val fft = this.fft
    val buffer = this.buffer
    val outputBuffer = this.outputBuffer

    Array.copy(spectrum, 0, buffer, 0, minBufferSize * 2)
    fft.complexForward(buffer)

    var max = 0f
    cfor(0)(_ < minBufferSize, _ + 1) { i =>
      if (scala.math.abs(buffer(2 * i + 0)) > max)
        max = scala.math.abs(buffer(2 * i + 0))
    }

    cfor(0)(_ < minBufferSize, _ + 1) { i =>
      outputBuffer(i) = (buffer(2 * i) / max * 4000).toShort
    }
  }

  def setAmplitude(k: Int, amplitude: Float): Unit = {
    val spectrum = this.spectrum
    val N = minBufferSize
    spectrum(2 * k + 0) = amplitude
    spectrum(2 * (N - k) + 0) = amplitude
  }
}

class FastCameraCallback(camera: Camera) extends CameraCallback {
  val streamer = new AudioStreamer

  val hueData = new Array[Float](360)
  def updateHueData(frame: Array[Byte], width: Int, height: Int) {
    val hueData = this.hueData
    val size = width * height

    for (i <- 0 until hueData.length) {
      hueData(i) = 0
    }

    cfor(0)(_ < height, _ + 20) { j =>
      cfor(0)(_ < width, _ + 20) { i =>
        val rgb = YUVDecoder.decode(frame, width, height, i, j)
        val (h, s, v) = ColorConverter.rgb2hsv(rgb)

        val k = (h / 360 * hueData.length).toInt
        hueData(k) += s * v
      }
    }
  }

  def onFrameData(frame: Array[Byte], width: Int, height: Int) {
    updateHueData(frame, width, height)

    var max = 0f
    cfor(0)(_ < hueData.length, _ + 1) { j =>
      if (hueData(j) > max) max = hueData(j)
    }

    cfor(0)(_ < hueData.length, _ + 1) { j =>
      streamer.setAmplitude(j * 3 + 500, (hueData(j) / max.toFloat).toInt)
    }

    streamer.update()
  }
}

class MainActivity extends Activity {
  private[this] implicit final val ctx: android.content.Context = this

  private var camera: Camera = null
  private var callback: FastCameraCallback = null
  private var cameraPreview: CameraPreview = null

  private def getCameraInstance(): Option[Camera] = {
    if (!CameraHelper.isCameraAvailable(this)) {
      reportError("No camera on this device.")
      None
    } else {
      val camera = CameraHelper.getBackFacingCameraInstance
      if (camera.isEmpty) {
        reportError("No back facing camera found.")
        None
      } else camera
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    //android.os.Debug.startMethodTracing("soundcam")

    setContentView(R.layout.activity_home)

    getCameraInstance() match {
      case Some(camera) =>
        this.callback = new FastCameraCallback(camera)
        cameraPreview = new CameraPreview(this, camera, this.callback)
        val preview = findViewById(R.id.camera_preview).asInstanceOf[FrameLayout]
        preview.addView(cameraPreview)

        this.camera = camera
      case None => ()
    }
  }

  override def onPause() {
    if (camera != null) {
      camera.stopPreview()
      camera.setPreviewCallback(null)
      camera.release()
      camera = null
    }
    //android.os.Debug.stopMethodTracing()
    super.onPause()
  }
}
