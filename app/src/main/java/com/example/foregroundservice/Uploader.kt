package com.example.foregroundservice

import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object Uploader {

    private const val UPLOAD_URL = "http://10.0.2.2:8000/upload"

    fun uploadZip(file: File): Boolean {
        return try {
            val url = URL(UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/zip")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            conn.setFixedLengthStreamingMode(file.length())

            FileInputStream(file).use { input ->
                val out: OutputStream = conn.outputStream
                input.copyTo(out)
                out.flush()
                out.close()
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            android.util.Log.e("Uploader", "upload failed", e)
            false
        }
    }
}
