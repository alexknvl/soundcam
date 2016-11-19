package com.alexknvl

import android.util.Log
import android.widget.Toast
import android.content.Context

package object soundcam {
  val TAG = "SoundCam"

  def info(x: Any): Unit = {
    val message = x.toString
    Log.i(TAG, message)
  }

  def reportWarning(x: Any)(implicit ctx: Context): Unit = {
    val message = x.toString
    Log.w(TAG, message)
    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
  }

  def reportError(x: Any)(implicit ctx: Context): Unit = {
    val message = x.toString
    Log.e(TAG, message)
    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
  }
}
