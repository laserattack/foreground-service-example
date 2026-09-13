package com.example.foregroundservice

import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Uploader {

    private const val UPLOAD_URL = "https://10.0.2.2:8443/upload"

    fun uploadZip(file: File): Boolean {
        return try {
            val url = URL(UPLOAD_URL)
            val conn = url.openConnection() as HttpsURLConnection

            // Доверять любому сертификату (для самоподписанного)
            conn.sslSocketFactory = createTrustAllSslContext().socketFactory
            conn.hostnameVerifier = HostnameVerifier { _, _ -> true }

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

    private fun createTrustAllSslContext(): SSLContext {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                                                 override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                                                 override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                                                 override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                                             })
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, trustAll, SecureRandom())
        return ctx
    }
}
