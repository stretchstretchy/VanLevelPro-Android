package com.vanlevelpro.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.vanlevelpro.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionTag: String,
    val apkDownloadUrl: String,
    val releaseNotes: String
)

object UpdateChecker {

    private const val TAG = "TEST"

    // GitHub repo this app checks for releases in: stretchstretchy/VanLevelPro-Android
    private const val API_URL =
        "https://api.github.com/repos/stretchstretchy/VanLevelPro-Android/releases/latest"

    private const val APK_FILENAME = "vanlevelpro_update.apk"

    /**
     * Hits GitHub's Releases API for the latest published release.
     * Returns null if there's no update available, or if the check
     * fails for any reason (network error, no APK attached, etc.) -
     * callers should treat null as "nothing to report", not an error
     * the user needs to see.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {

        try {

            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "UpdateChecker: GitHub API returned $responseCode")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }

            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")

            if (tagName.isEmpty()) {
                Log.e(TAG, "UpdateChecker: no tag_name in response")
                return@withContext null
            }

            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            if (apkUrl == null) {
                Log.e(TAG, "UpdateChecker: release '$tagName' has no .apk asset attached")
                return@withContext null
            }

            if (!isNewerVersion(tagName, BuildConfig.VERSION_NAME)) {
                Log.e(TAG, "UpdateChecker: already up to date ($tagName vs current ${BuildConfig.VERSION_NAME})")
                return@withContext null
            }

            Log.e(TAG, "UpdateChecker: update available -> $tagName")

            UpdateInfo(
                versionTag = tagName,
                apkDownloadUrl = apkUrl,
                releaseNotes = releaseNotes
            )

        } catch (e: Exception) {
            Log.e(TAG, "UpdateChecker: check failed", e)
            null
        }
    }

    /**
     * Compares two version strings numerically (e.g. "1.9" vs "1.10" -
     * a plain string comparison would wrongly say 1.9 is newer). Tags
     * are expected as "v1.1" or "1.1"; a leading "v" is stripped.
     */
    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {

        val remoteParts = remoteTag.removePrefix("v").removePrefix("V")
            .split(".").mapNotNull { it.toIntOrNull() }

        val currentParts = currentVersion.removePrefix("v").removePrefix("V")
            .split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)

        for (i in 0 until length) {
            val remote = remoteParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }

            if (remote != current) {
                return remote > current
            }
        }

        return false
    }

    /**
     * Starts downloading the APK via Android's DownloadManager. Returns
     * the download ID, which the caller should track via
     * DownloadManager.ACTION_DOWNLOAD_COMPLETE to know when to call
     * installApk().
     */
    fun downloadUpdate(context: Context, apkUrl: String): Long {

        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Clear out any previous partial/stale download of the same name.
        getDownloadedApkFile(context).delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("VanLevel Pro Update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                APK_FILENAME
            )

        return downloadManager.enqueue(request)
    }

    fun getDownloadedApkFile(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(dir, APK_FILENAME)
    }

    /**
     * Launches the system package installer for the already-downloaded
     * APK. Requires the user to have allowed "install unknown apps" for
     * this app (same prompt as any sideloaded APK) - Android shows that
     * prompt automatically if it hasn't been granted yet.
     */
    fun installApk(context: Context) {

        val apkFile = getDownloadedApkFile(context)

        if (!apkFile.exists()) {
            Log.e(TAG, "installApk: downloaded file not found at ${apkFile.path}")
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
