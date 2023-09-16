package com.invorel.blankchatpro.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Contacts.Photo
import com.invorel.blankchatpro.state.Contact
import java.io.ByteArrayInputStream
import java.io.IOException

object ContentResolverUtils {

  fun getContactsWithNumber(
    contentResolver: ContentResolver,
    onContactsFetched: (List<Contact>) -> Unit,
  ) {

    val contactsList = mutableListOf<Contact>()

    contentResolver.query(
      /* uri = */ Contacts.CONTENT_URI,
      /* projection = */ arrayOf(
        Contacts._ID,
        Contacts.DISPLAY_NAME
      ),
      /* selection = */ Contacts.HAS_PHONE_NUMBER + " = ?",
      /* selectionArgs = */ arrayOf("1"),
      /* sortOrder = */ Phone.DISPLAY_NAME_PRIMARY.plus(" ASC")
    )?.use { contactCursor ->
      while (contactCursor.moveToNext()) {
        val contactIdColumnIndex = contactCursor.getColumnIndex(Contacts._ID)
        val contactNameColumnIndex =
          contactCursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)

        if (contactIdColumnIndex != -1 && contactNameColumnIndex != -1) {

          val contactId = contactCursor.getString(contactIdColumnIndex)
          val contactName = contactCursor.getString(contactNameColumnIndex)

          val contact = Contact(
            id = contactId.toLong(),
            name = contactName,
            number = retrievePhoneNumber(contentResolver, contactId.toLong()),
            photo = retrieveThumbNailPhoto(contactId.toLong(), contentResolver)
          )
          contactsList.add(contact)
        }
      }
    }

    onContactsFetched.invoke(contactsList)
  }

  private fun retrievePhoneNumber(contentResolver: ContentResolver, contactId: Long): String {
    contentResolver.query(
      Phone.CONTENT_URI,
      null,
      Phone.CONTACT_ID.plus(" =?"),
      arrayOf(contactId.toString()),
      null
    )?.use { phoneCursor ->
      return if (phoneCursor.moveToFirst()) {
        val contactNumberColumnIndex = phoneCursor.getColumnIndex(Phone.NUMBER)
        phoneCursor.getString(contactNumberColumnIndex)
      } else {
        return ""
      }
    }
    return ""
  }

  fun retrieveThumbNailPhoto(contactId: Long, contentResolver: ContentResolver): Bitmap? {
    val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
    val photoUri = Uri.withAppendedPath(contactUri, Photo.CONTENT_DIRECTORY)
    contentResolver.query(photoUri, arrayOf(Photo.PHOTO), null, null, null)
      ?.use { imageCursor ->
        if (imageCursor.moveToFirst()) {
          val data = imageCursor.getBlob(0)
          if (data != null) {
            return getBitmapFromInputStream(ByteArrayInputStream(data))
          }
        }
      }
    return null
  }

  private fun getBitmapFromInputStream(inputStream: ByteArrayInputStream): Bitmap? {
    return try {
      BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
      e.printStackTrace()
      null
    }
  }
}


