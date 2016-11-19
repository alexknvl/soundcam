package com.alexknvl.soundcam

object ColorConverter {
  @inline private[this] final def clip(v: Int) =
    if (v < 0) 0
    else if (v > 255) 255
    else v

  @inline final def yuv2rgb(y: Int, u: Int, v: Int): Int = {
    val c = y - 16
    val d = u - 128
    val e = v - 128

    val r = clip((298 * c           + 409 * e + 128) >> 8)
    val g = clip((298 * c - 100 * d - 208 * e + 128) >> 8)
    val b = clip((298 * c + 516 * d           + 128) >> 8)

    return (r << 16) + (g << 8) + b
  }

  @inline final def rgb2hsv(rgb: Int): (Float, Float, Float) =
    rgb2hsv(rgb >> 16, (rgb >> 8) & 0xff, rgb & 0xff)
  @inline final def rgb2hsv(r: Int, g: Int, b: Int): (Float, Float, Float) = {
    import scala.math.{ min, max }
    val minValue = min(min(r, g), b)
    val maxValue = max(max(r, g), b)
    val delta = maxValue - minValue

    val v = maxValue.toFloat

    if (v > 0) {
      val s = delta.toFloat / maxValue
      val h0 =
        if (r == maxValue) (g - b).toFloat / delta
        else if (g == maxValue) 2 + (b - r).toFloat / delta
        else 4 + (r -g) / delta
      val h =
        if (h0 < 0) h0 * 60 + 360
        else h0 * 60
      (h, s, v)
    } else (0, 0, 0)
  }

  @inline final def rgb2h(rgb: Int): Float =
    rgb2h(rgb >> 16, (rgb >> 8) & 0xff, rgb & 0xff)
  @inline final def rgb2h(r: Int, g: Int, b: Int): Float = {
    import scala.math.{ min, max }
    val minValue = min(min(r, g), b)
    val maxValue = max(max(r, g), b)
    val delta = maxValue - minValue

    if (maxValue > 0) {
      val h0 =
        if (r == maxValue) (g - b).toFloat / delta
        else if (g == maxValue) 2 + (b - r).toFloat / delta
        else 4 + (r - g) / delta
      val h =
        if (h0 < 0) h0 * 60 + 360
        else h0 * 60
      h
    } else 0
  }
}