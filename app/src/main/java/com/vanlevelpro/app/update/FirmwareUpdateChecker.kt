package com.vanlevelpro.app.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FirmwareUpdateInfo(
    val versionTag: String,
    val binDownloadUrl: String,
    val releaseNotes: String
)

object FirmwareUpdateChecker {

    private const val TAG = "TEST"

    // Separate repo from the Android app - this is the ESP32 firmware
    // itself (stretchstretchy/VanLevelPro).
    private const val API_URL =
        "https://api.github.com/repos/stretchstretchy/VanLevelPro/releases/latest"

    sealed class CheckResult {
        data class UpdateAvailable(val info: FirmwareUpdateInfo) : CheckResult()
        object UpToDate : CheckResult()
        object CheckFailed : CheckResult()
    }

    /**
     * currentVersion is whatever the connected ESP32 itself reported
     * (via the "version" command) - not a compile-time constant like
     * the Android app's own version, since it depends on whatever
     * firmware happens to already be flashed on this particular board.
     */
    suspend fun checkForUpdate(currentVersion: String): CheckResult =
        withContext(Dispatchers.IO) {

            try {

                val connection = URL(API_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "FirmwareUpdateChecker: GitHub API returned $responseCode")
                    return@withContext CheckResult.CheckFailed
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }

                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "")
                val releaseNotes = json.optString("body", "")

                if (tagName.isEmpty()) {
                    Log.e(TAG, "FirmwareUpdateChecker: no tag_name in response")
                    return@withContext CheckResult.CheckFailed
                }

                val assets = json.optJSONArray("assets")
                var binUrl = ""

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".bin")) {
                            binUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (binUrl.isEmpty()) {
                    Log.e(TAG, "FirmwareUpdateChecker: release '$tagName' has no .bin asset attached")
                    return@withContext CheckResult.CheckFailed
                }

                Log.e(TAG, "FirmwareUpdateChecker: latest release is $tagName, device is $currentVersion")

                if (!UpdateChecker.isNewerVersion(tagName, currentVersion)) {
                    return@withContext CheckResult.UpToDate
                }

                CheckResult.UpdateAvailable(
                    FirmwareUpdateInfo(
                        versionTag = tagName,
                        binDownloadUrl = binUrl,
                        releaseNotes = releaseNotes
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "FirmwareUpdateChecker: check failed", e)
                CheckResult.CheckFailed
            }
        }

    /**
     * Downloads the firmware binary directly into memory rather than
     * to disk - firmware images are small enough (typically well under
     * 2MB) that this is simpler than the APK updater's DownloadManager
     * approach, and BluetoothManager.performOtaUpdate() needs the raw
     * bytes in memory anyway to stream over BLE.
     */
    suspend fun downloadFirmware(url: String): ByteArray? =
        withContext(Dispatchers.IO) {

            try {

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000

                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "downloadFirmware: HTTP $responseCode")
                    return@withContext null
                }

                connection.inputStream.use { it.readBytes() }

            } catch (e: Exception) {
                Log.e(TAG, "downloadFirmware: failed", e)
                null
            }
        }
}