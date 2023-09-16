package com.invorel.blankchatpro.local.typeconverters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class BitmapTypeConverter {

  @TypeConverter
  fun fromBitMap(bitmap: Bitmap?): ByteArray? {
    if (bitmap == null) return null

    val outStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
    return outStream.toByteArray()
  }

  @TypeConverter
  fun toBitMap(byteArray: ByteArray?): Bitmap? {
    if (byteArray == null) return null

    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
  }
}