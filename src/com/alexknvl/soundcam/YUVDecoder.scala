package com.alexknvl.soundcam

object YUVDecoder {
  @inline final def decode(src: Array[Byte], width: Int, height: Int, i: Int, j: Int): Int = {
    val size = width * height
    val y = src(j * width + i) & 0xff
    val i2 = i / 2
    val j2 = j / 2
    val u = (src(size + 2 * i2 + j2 * width) & 0xff)
    val v = (src(size + 2 * i2 + j2 * width + 1) & 0xff)

    return ColorConverter.yuv2rgb(y, u, v)
  }

  final def decode(src: Array[Byte], width: Int, height: Int, dst: Array[Int]): Unit = {
    var j = 0
    while (j < height) {
      var i = 0
      while (i < width) {
        dst(j * width + i) = decode(src, width, height, i, j)
        i += 1
      }
      j += 1
    }
  }
}