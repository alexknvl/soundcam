package com.alexknvl.soundcam

import java.io.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import scala.util.Try
import scala.util.control.NonFatal

import android.util.Log
import android.annotation.TargetApi
import android.hardware.Camera
import android.content.Context
import android.os.{ Build, Environment }
import android.content.pm.PackageManager

object CameraHelper {
  val MEDIA_TYPE_IMAGE: Int = 1
  val MEDIA_TYPE_VIDEO: Int = 2

  type Size = Camera#Size

  /**
   * @return Returns true if there is a camera on the device.
   */
  def isCameraAvailable(context: Context): Boolean =
    context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)

  /**
   * Iterate over supported camera preview sizes to see which one best fits the
   * dimensions of the given view while maintaining the aspect ratio. If none can,
   * be lenient with the aspect ratio.
   *
   * @param sizes Supported camera preview sizes.
   * @param targetWidth The width of the view.
   * @param targetHeight The height of the view.
   * @return Best match camera preview size to fit in the view.
   */
  def getOptimalPreviewSize(sizes: Iterable[Size], targetWidth: Int, targetHeight: Int): Size = {
    // Use a very small tolerance because we want an exact match.
    val ASPECT_TOLERANCE = 0.1
    val targetRatio = targetWidth.toDouble / targetHeight

    // Selects size with minimum height difference.
    def selectSize(sizes: Iterable[Size]): Size =
      sizes reduce { (a, b) =>
        if (Math.abs(a.height - targetHeight) < Math.abs(b.height - targetHeight)) a
        else b
      }

    // Try to find a preview size that matches aspect ratio and the target view size.
    // Iterate over all available sizes and pick the largest size that can fit in the view and
    // still maintain the aspect ratio.
    val similarSizes = sizes filter { size =>
      val ratio = size.width.toDouble / size.height
      Math.abs(ratio - targetRatio) < ASPECT_TOLERANCE
    }

    if (similarSizes.nonEmpty) selectSize(similarSizes)
    else selectSize(sizes)
  }

  /**
   * @return the default camera on the device. Returns null if there is no camera on the device.
   */
  def getDefaultCameraInstance: Try[Camera] = Try(Camera.open())

  /**
   * @return the default rear/back facing camera on the device. Returns None if camera is not
   * available.
   */
  def getBackFacingCameraInstance = getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK)

  /**
   * @return the default front facing camera on the device. Returns None if camera is not
   * available.
   */
  def getFrontFacingCameraInstance = getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)

  /**
   * @param position Physical position of the camera i.e Camera.CameraInfo.CAMERA_FACING_FRONT
   *                 or Camera.CameraInfo.CAMERA_FACING_BACK.
   * @return the default camera on the device. Returns None if camera is not available.
   */
  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
  private def getDefaultCamera(position: Int): Option[Camera] = {
    val cameraInfo = new Camera.CameraInfo

    def facing(i: Int): Int = {
      Camera.getCameraInfo(i, cameraInfo)
      cameraInfo.facing
    }

    val ids = for {
      i <- 0 until Camera.getNumberOfCameras
      if facing(i) == position
    } yield i

    try {
      ids.headOption.map { Camera.open(_) }
    } catch {
      case NonFatal(e) => None
    }
  }

  /**
   * Creates a media file in the {@code Environment.DIRECTORY_PICTURES} directory. The directory
   * is persistent and available to other applications like gallery.
   *
   * @param type Media type. Can be video or image.
   * @return A file object pointing to the newly created file.
   */
  def getOutputMediaFile(suffix: String, tpe: Int): Option[File] = {
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.
    if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
      return None
    }

    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val mediaStorageDir =
      if (suffix != "") new java.io.File(picturesDir, suffix)
      else picturesDir

    // Create the storage directory if it does not exist
    if (!mediaStorageDir.exists()) {
      if (!mediaStorageDir.mkdirs()) {
        Log.i(TAG, "failed to create directory");
        return None;
      }
    }

    // Create a media file name
    val timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    tpe match {
      case MEDIA_TYPE_IMAGE =>
        Some(new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg"))
      case MEDIA_TYPE_VIDEO =>
        Some(new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4"))
      case _ => None
    }
  }
}