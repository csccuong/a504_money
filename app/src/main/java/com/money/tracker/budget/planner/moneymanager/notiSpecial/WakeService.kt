package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Collections

class WakeService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val notifiedFiles = Collections.synchronizedSet(HashSet<String>())
    private val notifiedScreenshots = Collections.synchronizedSet(HashSet<String>())

    private var downloadObserver: FileObserver? = null
    private var screenshotObserver: FileObserver? = null
    private var screenshotContentObserver: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        runCatching { createServiceChannel() }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            val notification = buildServiceNotification()
            if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q until Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                mainHandler.postDelayed({
                    Log.d(TAG, "Auto-stop after timeout on Android 15+")
                    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                    stopSelf()
                }, ANDROID_15_TIMEOUT_MS)
            }

            startDownloadObserver()
            startScreenshotObserver()
        }.onFailure {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun startDownloadObserver() {
        if (downloadObserver != null) return
        val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        downloadObserver = object : FileObserver(downloadPath, CREATE or CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (event != CLOSE_WRITE || path == null) return

                val realFileName = normalizePendingFileName(path)
                if (SUPPORTED_DOC_EXTENSIONS.none { realFileName.endsWith(it, ignoreCase = true) }) return

                val fullFilePath = "$downloadPath/$realFileName"
                if (notifiedFiles.add(fullFilePath)) {
                    NotificationUtil.showDownloadNotification(applicationContext, fullFilePath)
                    mainHandler.postDelayed({ notifiedFiles.remove(fullFilePath) }, DEDUPE_DELAY_MS)
                }
            }
        }.also { it.startWatching() }
    }

    private fun normalizePendingFileName(path: String): String {
        if (!path.startsWith(".pending-")) return path
        val dashIndex = path.indexOf('-', startIndex = 9)
        return if (dashIndex != -1 && dashIndex + 1 < path.length) {
            path.substring(dashIndex + 1)
        } else {
            path
        }
    }

    @Suppress("DEPRECATION")
    private fun startScreenshotObserver() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (screenshotObserver != null) return
            val screenshotPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path}/Screenshots"
            screenshotObserver = object : FileObserver(screenshotPath, CREATE or CLOSE_WRITE) {
                override fun onEvent(event: Int, path: String?) {
                    if (event != CLOSE_WRITE || path == null) return
                    if (!path.endsWith(".png", ignoreCase = true) && !path.endsWith(".jpg", ignoreCase = true)) return

                    val fullPath = "$screenshotPath/$path"
                    if (notifiedScreenshots.add(fullPath)) {
                        BitmapFactory.decodeFile(fullPath)?.let { bitmap ->
                            NotificationUtil.showScreenShotNotification(applicationContext, bitmap)
                        }
                        mainHandler.postDelayed({ notifiedScreenshots.remove(fullPath) }, DEDUPE_DELAY_MS)
                    }
                }
            }.also { it.startWatching() }
        } else {
            if (screenshotContentObserver != null) return
            val externalPrefix = "${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/"
            screenshotContentObserver = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    if (uri == null) return
                    if (!uri.toString().startsWith(externalPrefix)) {
                        showDefaultScreenshotNotification()
                        return
                    }
                    handleScreenshotUri(uri, retryCount = 0)
                }
            }.also { observer ->
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
            }
        }
    }

    private fun showDefaultScreenshotNotification() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.noti_day_2)
        NotificationUtil.showScreenShotNotification(this, bitmap)
    }

    private fun handleScreenshotUri(uri: Uri, retryCount: Int) {
        if (uri.scheme != "content" || uri.authority?.contains("media") != true) {
            showDefaultScreenshotNotification()
            return
        }

        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.IS_PENDING
        )

        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return

                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                val isPending = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_PENDING))

                if (isPending == 1 && retryCount < 3) {
                    mainHandler.postDelayed({ handleScreenshotUri(uri, retryCount + 1) }, 500L)
                    return
                }

                if (relativePath?.contains("Screenshots") == true && notifiedScreenshots.add(name)) {
                    decodeScreenshotBitmap(uri)?.let { bitmap ->
                        NotificationUtil.showScreenShotNotification(applicationContext, bitmap)
                    }
                    mainHandler.postDelayed({ notifiedScreenshots.remove(name) }, DEDUPE_DELAY_MS)
                }
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "No permission to read URI: $uri", se)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling screenshot URI: $uri", e)
        }
    }

    private fun decodeScreenshotBitmap(uri: Uri): Bitmap? = runCatching {
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching {
            downloadObserver?.stopWatching()
            downloadObserver = null
            screenshotObserver?.stopWatching()
            screenshotObserver = null
            screenshotContentObserver?.let { contentResolver.unregisterContentObserver(it) }
            screenshotContentObserver = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Money Manager Quick Bar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            NotificationManagerCompat.from(this).createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("RemoteViewLayout")
    private fun buildServiceNotification(): Notification = runCatching {
        val pendingIntent = NotificationUtil.createOpenAppPendingIntent(applicationContext)
        val compactLayout = RemoteViews(packageName, R.layout.custom_notification_service_no_ic)
        val expandedLayout = RemoteViews(packageName, R.layout.custom_notification_service)

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti_wallet)
            .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.brand_primary))
            .setContentIntent(pendingIntent)
            .setCustomContentView(compactLayout)
            .setCustomBigContentView(expandedLayout)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }.getOrElse {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noti_wallet)
            .setContentTitle("")
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(foregroundServiceType: Int, reason: Int) {
        runCatching {
            Log.e(TAG, "Foreground service timeout on Android 15. Stopping service.")
            downloadObserver?.stopWatching()
            downloadObserver = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.onFailure {
            Log.e(TAG, "onTimeout error", it)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val preferences = AppPreferences.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        if (now - preferences.getLastTaskRemovedNotificationTime() >= TASK_REMOVED_THROTTLE_MS) {
            runCatching {
                FirebaseAnalytics.getInstance(applicationContext)
                    .logEvent("event_show_noti_close_app_new", null)
                NotificationUtil.showClosedAppNotification(applicationContext)
                preferences.setLastTaskRemovedNotificationTime(now)
            }
        }
    }

    private companion object {
        const val TAG = "WakeService"
        const val CHANNEL_ID = "WakeServiceChannel"
        const val NOTIFICATION_ID = 1
        const val DEDUPE_DELAY_MS = 5_000L
        const val ANDROID_15_TIMEOUT_MS = 10 * 60 * 1000L
        const val TASK_REMOVED_THROTTLE_MS = 15 * 60 * 1000L

        val SUPPORTED_DOC_EXTENSIONS = listOf(
            ".doc", ".docx", ".ppt", ".pptx", ".pdf", ".xls", ".xlsx", ".xlsm", ".csv"
        )
    }
}
