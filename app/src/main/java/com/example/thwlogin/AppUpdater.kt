package com.example.thwlogin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val GITHUB_REPO = "TheGamePRO112/THWLogin"

    suspend fun checkUpdate(currentVersion: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "THWLogin-Android-App")
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.getString("tag_name").removePrefix("v").trim()

                if (isNewerVersion(currentVersion, tagName)) {
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name").lowercase()
                            if (name.endsWith(".apk")) {
                                return@withContext ReleaseInfo(
                                    version = tagName,
                                    downloadUrl = asset.getString("browser_download_url")
                                )
                            }
                        }
                    }
                    // Fallback: Falls kein .apk Asset angehängt ist, zur GitHub Release-Seite verlinken
                    val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases/latest")
                    return@withContext ReleaseInfo(
                        version = tagName,
                        downloadUrl = htmlUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AppUpdater", "Fehler beim Update-Check", e)
        }
        return@withContext null
    }

    private fun isNewerVersion(current: String, target: String): Boolean {
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = target.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(currParts.size, targetParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val t = targetParts.getOrElse(i) { 0 }
            if (t > c) return true
            if (t < c) return false
        }
        return false
    }

    fun startDownload(context: Context, downloadUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

data class ReleaseInfo(val version: String, val downloadUrl: String)