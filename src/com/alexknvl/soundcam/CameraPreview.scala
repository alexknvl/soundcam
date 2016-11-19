package com.alexknvl.soundcam

import android.util.Log
import android.hardware.Camera
import android.hardware.Camera.{ CameraInfo, PictureCallback, PreviewCallback }
import android.view.{ View, ViewGroup, SurfaceView, SurfaceHolder, Menu }
import android.graphics.ImageFormat
import android.content.Context

import scala.collection.JavaConverters._

trait CameraCallback {
  def onFrameData(frame: Array[Byte], width: Int, height: Int)
}

final class CameraPreview(
  private[this] val context: Context,
  private[this] val camera: Camera,
  private[this] val callback: CameraCallback)
extends SurfaceView(context) with SurfaceHolder.Callback with Camera.PreviewCallback {
  // Install a SurfaceHolder.Callback so we get notified when the
  // underlying surface is created and destroyed.
  private[this] val holder = {
    val holder = getHolder()
    holder.addCallback(this)
    // deprecated setting, but required on Android versions prior to 3.0
    holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    holder
  }

  private[this] var width: Int = 0
  private[this] var height: Int = 0

  override def surfaceCreated(holder: SurfaceHolder): Unit = { }

  override def surfaceDestroyed(holder: SurfaceHolder): Unit = { }

  override def surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int): Unit = {
    if (holder.getSurface == null) {
      // Preview surface does not exist.
      return;
    }

    // Stop preview before making changes.
    try {
      camera.stopPreview()
    } catch {
      // Ignore: tried to stop a non-existent preview.
      case e: Exception => Log.e(TAG, "Error stopping camera preview: " + e.getMessage)
    }

    info(s"w=$w, h=$h")

    // Query camera to find all the sizes and choose the optimal size given the
    // dimensions of our preview surface.
    val parameters = camera.getParameters
    val supportedPreviewSizes = parameters.getSupportedPreviewSizes.asScala
    val size = CameraHelper.getOptimalPreviewSize(supportedPreviewSizes, w, h)

    parameters.setPreviewSize(size.width, size.height)
    width = size.width
    height = size.height

    info(s"width=$width, height=$height")

    parameters.setRecordingHint(true)
    parameters.setPreviewFrameRate(30)
    //parameters.setPreviewFpsRange(15000,30000)
    parameters.setAutoWhiteBalanceLock(true)

    camera.setParameters(parameters)

    val format = parameters.getPreviewFormat
    val bufSize = width * height * ImageFormat.getBitsPerPixel(format) / 8
    for (_ <- 0 until 5) {
      camera.addCallbackBuffer(new Array[Byte](bufSize))
    }

    camera.setOneShotPreviewCallback(this)
    camera.setPreviewDisplay(holder)

    // start preview with new settings
    try {
      camera.startPreview()
    } catch {
      case e: Exception => Log.e(TAG, "Error starting camera preview: " + e.getMessage)
    }
  }
  override def onPreviewFrame(data: Array[Byte], camera: Camera): Unit = {
    callback.onFrameData(data, width, height)
    camera.addCallbackBuffer(data)
    camera.setPreviewCallback(this)
  }
}
