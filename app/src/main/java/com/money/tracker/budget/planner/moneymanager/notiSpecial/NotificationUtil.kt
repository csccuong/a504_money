package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import java.io.File

object NotificationUtil {

    const val CHANNEL_DOWNLOAD_OBSERVER = "DownloadFileObserverChannel"
    private const val CHANNEL_APP_INSTALL = "AppInstallChannel"

    fun showDownloadNotification(context: Context, filePath: String) {
        val localized = LocaleHelper.localizedContext(context)
        val fileName = File(filePath).name
        val pendingIntent = createOpenAppPendingIntent(context) {
            putExtra("path", filePath)
            putExtra("name", fileName)
            putExtra("isFromNotification", true)
        }

        val title = localized.getString(R.string.title_dowload_done)
        val body = localized.getString(R.string.body_dowload_done) + fileName
        val button = localized.getString(R.string.open_now)

        notifyCustom(
            context = context,
            channelId = CHANNEL_DOWNLOAD_OBSERVER,
            channelName = "Money Manager Reminders",
            channelDesc = "Daily spending, budget, and savings reminders",
            smallIconRes = R.drawable.ic_noti_receipt,
            pendingIntent = pendingIntent,
            compactLayout = R.layout.custom_notification_dowload,
            headsUpLayout = R.layout.custom_noti_header_dowload,
            expandedLayout = R.layout.custom_notification_expanded_dowload,
            title = title,
            body = body,
            buttonText = button
        )
    }

    fun showScreenShotNotification(context: Context, screenshotBitmap: Bitmap?) {
        val localized = LocaleHelper.localizedContext(context)
        val pendingIntent = createOpenAppPendingIntent(context)

        notifyCustom(
            context = context,
            channelId = CHANNEL_DOWNLOAD_OBSERVER,
            channelName = "Money Manager Reminders",
            channelDesc = "Daily spending, budget, and savings reminders",
            smallIconRes = R.drawable.ic_noti_receipt,
            pendingIntent = pendingIntent,
            compactLayout = R.layout.custom_notification_dowload,
            headsUpLayout = R.layout.custom_noti_header_dowload,
            expandedLayout = R.layout.custom_notification_expanded_dowload,
            title = localized.getString(R.string.title_screen_shoot_done),
            body = localized.getString(R.string.body_screen_shoot_done),
            buttonText = localized.getString(R.string.open_now),
            imageBitmap = screenshotBitmap
        )
    }

    fun showClosedAppNotification(context: Context) {
        val localized = LocaleHelper.localizedContext(context)
        val pendingIntent = createOpenAppPendingIntent(context) {
            putExtra("noti_type_special_v2", true)
        }

        notifyCustom(
            context = context,
            channelId = CHANNEL_DOWNLOAD_OBSERVER,
            channelName = "Money Manager Reminders",
            channelDesc = "Daily spending, budget, and savings reminders",
            smallIconRes = R.drawable.ic_noti_wallet,
            pendingIntent = pendingIntent,
            compactLayout = R.layout.custom_notification,
            headsUpLayout = R.layout.custom_noti_header,
            expandedLayout = R.layout.custom_notification_expanded,
            title = localized.getString(R.string.title_out_done),
            body = localized.getString(R.string.body_out_done),
            buttonText = localized.getString(R.string.continued)
        )
    }

    fun showAppInstallNotification(context: Context, appInfo: AppInstallReceiver.AppInfo) {
        val localized = LocaleHelper.localizedContext(context)
        showAppEventNotification(
            context = context,
            compactLayout = R.layout.noti_custom_default,
            title = localized.getString(R.string.noti_app_installed_title),
            body = localized.getString(R.string.noti_app_installed_body, appInfo.appName),
            buttonText = localized.getString(R.string.open_now),
            appIcon = appInfo.appIcon
        )
    }

    fun showAppRemovedNotification(context: Context, appInfo: AppInstallReceiver.AppInfo) {
        val localized = LocaleHelper.localizedContext(context)
        showAppEventNotification(
            context = context,
            compactLayout = R.layout.custom_notification_dowload,
            title = localized.getString(R.string.noti_app_removed_title),
            body = localized.getString(R.string.noti_app_removed_body),
            buttonText = localized.getString(R.string.noti_app_removed_btn),
            appIcon = appInfo.appIcon
        )
    }

    fun showAppUpdatedNotification(context: Context, appInfo: AppInstallReceiver.AppInfo) {
        val localized = LocaleHelper.localizedContext(context)
        showAppEventNotification(
            context = context,
            compactLayout = R.layout.custom_notification_dowload,
            title = localized.getString(R.string.noti_app_updated_title),
            body = localized.getString(R.string.noti_app_updated_body),
            buttonText = localized.getString(R.string.noti_app_updated_btn),
            appIcon = appInfo.appIcon
        )
    }

    private fun showAppEventNotification(
        context: Context,
        @LayoutRes compactLayout: Int,
        title: String,
        body: String,
        buttonText: String,
        appIcon: Drawable?
    ) {
        val pendingIntent = createOpenAppPendingIntent(context)
        notifyCustom(
            context = context,
            channelId = CHANNEL_APP_INSTALL,
            channelName = "Smart Activity Reminders",
            channelDesc = "Helpful reminders to track receipts, bills, and subscriptions",
            smallIconRes = R.drawable.ic_noti_budget,
            pendingIntent = pendingIntent,
            compactLayout = compactLayout,
            headsUpLayout = R.layout.custom_noti_header_dowload,
            expandedLayout = R.layout.custom_notification_expanded_dowload,
            title = title,
            body = body,
            buttonText = buttonText,
            imageBitmap = appIcon?.let(::drawableToBitmap)
        )
    }

    fun ensureChannel(
        context: Context,
        channelId: String,
        name: String,
        description: String,
        importance: Int = NotificationManager.IMPORTANCE_HIGH
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    fun createOpenAppPendingIntent(
        context: Context,
        configureIntent: Intent.() -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context, ProDocsSplashNotiActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            configureIntent()
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildRemoteView(
        context: Context,
        @LayoutRes layoutId: Int,
        @IdRes rootViewId: Int,
        pendingIntent: PendingIntent,
        title: String,
        body: String,
        buttonText: String,
        imageBitmap: Bitmap? = null,
        applyMiddleImage: Boolean = false
    ): RemoteViews = RemoteViews(context.packageName, layoutId).apply {
        val padding = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            (20 * context.resources.displayMetrics.density).toInt()
        } else {
            0
        }
        setViewPadding(rootViewId, padding, padding, padding, padding)
        setOnClickPendingIntent(R.id.notification_button, pendingIntent)
        setTextViewText(R.id.notification_title_bold, title)
        setTextViewText(R.id.notification_title, body)
        setTextViewText(R.id.notification_button, buttonText)
        if (imageBitmap != null) {
            setImageViewBitmap(R.id.notification_image, imageBitmap)
            if (applyMiddleImage) {
                setImageViewBitmap(R.id.notification_middle_image, imageBitmap)
            }
        }
    }

    fun notifyIfPermitted(context: Context, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun notifyCustom(
        context: Context,
        channelId: String,
        channelName: String,
        channelDesc: String,
        @DrawableRes smallIconRes: Int,
        pendingIntent: PendingIntent,
        @LayoutRes compactLayout: Int,
        @LayoutRes headsUpLayout: Int,
        @LayoutRes expandedLayout: Int,
        title: String,
        body: String,
        buttonText: String,
        imageBitmap: Bitmap? = null
    ) {
        ensureChannel(context, channelId, channelName, channelDesc)

        val compactView = buildRemoteView(
            context, compactLayout, R.id.notification_root, pendingIntent, title, body, buttonText, imageBitmap
        )
        val headsUpView = buildRemoteView(
            context, headsUpLayout, R.id.notification_root, pendingIntent, title, body, buttonText, imageBitmap
        )
        val expandedView = buildRemoteView(
            context, expandedLayout, R.id.notification_root_expanded, pendingIntent, title, body, buttonText, imageBitmap
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_primary))
            .setContentIntent(pendingIntent)
            .setCustomContentView(compactView)
            .setCustomHeadsUpContentView(headsUpView)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notifyIfPermitted(context, builder)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
