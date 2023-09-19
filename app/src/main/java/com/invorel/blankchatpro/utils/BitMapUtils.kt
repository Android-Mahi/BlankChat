package com.invorel.blankchatpro.utils

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object BitMapUtils {

  fun convertBitMapToByteArray(bitmap: Bitmap): ByteArray {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    return baos.toByteArray()
  }
}
