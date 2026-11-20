package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.ArrayRes
import androidx.annotation.LayoutRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.data.AppPreferences
import com.money.tracker.budget.planner.moneymanager.utils.LocaleHelper
import java.util.Calendar
import kotlin.random.Random

object UnlockNotifier {

    private const val THROTTLE_WINDOW_MS = 30 * 60 * 1000L

    private val DAY_IMAGES = intArrayOf(
        R.drawable.noti_day_1,
        R.drawable.noti_day_2,
        R.drawable.noti_day_3,
        R.drawable.noti_day_4,
        R.drawable.noti_day_5,
        R.drawable.noti_day_6
    )

    private val DAY_SMALL_ICONS = intArrayOf(
        R.drawable.ic_noti_wallet,
        R.drawable.ic_noti_receipt,
        R.drawable.ic_noti_chart,
        R.drawable.ic_noti_budget,
        R.drawable.ic_noti_wallet,
        R.drawable.ic_noti_coin
    )

    private val NIGHT_IMAGES = intArrayOf(
        R.drawable.noti_night_1,
        R.drawable.noti_night_2,
        R.drawable.noti_night_3,
        R.drawable.noti_night_4,
        R.drawable.noti_night_5,
        R.drawable.noti_night_6
    )

    private val NIGHT_SMALL_ICONS = intArrayOf(
        R.drawable.ic_noti_moon,
        R.drawable.ic_noti_coin,
        R.drawable.ic_noti_chart,
        R.drawable.ic_noti_receipt,
        R.drawable.ic_noti_wallet,
        R.drawable.ic_noti_budget
    )

    fun handleUserPresent(context: Context) {
        val preferences = AppPreferences.getInstance(context)
        val now = System.currentTimeMillis()
        if (now - preferences.getLastUnlockNotificationTime() < THROTTLE_WINDOW_MS) {
            return
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val analytics = FirebaseAnalytics.getInstance(context)

        if (hour in 7 until 19) {
            analytics.logEvent("event_show_noti_day_new", null)
            showScheduledNotification(
                context = context,
                images = DAY_IMAGES,
                smallIcons = DAY_SMALL_ICONS,
                titlesArrayRes = R.array.special_titles_news,
                messagesArrayRes = R.array.special_messages_news,
                expandedLayoutRes = R.layout.custom_notification_expand_news
            )
        } else {
            analytics.logEvent("event_show_noti_night_new", null)
            showScheduledNotification(
                context = context,
                images = NIGHT_IMAGES,
                smallIcons = NIGHT_SMALL_ICONS,
                titlesArrayRes = R.array.special_titles_v2,
                messagesArrayRes = R.array.special_messages_v2,
                expandedLayoutRes = R.layout.noti_custom_expand
            )
        }

        preferences.setLastUnlockNotificationTime(now)
    }

    private fun showScheduledNotification(
        context: Context,
        images: IntArray,
        smallIcons: IntArray,
        @ArrayRes titlesArrayRes: Int,
        @ArrayRes messagesArrayRes: Int,
        @LayoutRes expandedLayoutRes: Int
    ) {
        NotificationUtil.ensureChannel(
            context = context,
            channelId = NotificationUtil.CHANNEL_DOWNLOAD_OBSERVER,
            name = "Money Manager Reminders",
            description = "Daily spending, budget, and savings reminders"
        )

        val localized = LocaleHelper.localizedContext(context)
        val titles = localized.resources.getStringArray(titlesArrayRes)
        val messages = localized.resources.getStringArray(messagesArrayRes)
        val maxIndex = minOf(titles.size, messages.size, images.size, smallIcons.size)
        if (maxIndex <= 0) return

        val index = Random.nextInt(maxIndex)
        val title = titles[index]
        val message = messages[index]
        val openLabel = localized.getString(R.string.open_now)
        val imageBitmap = BitmapFactory.decodeResource(context.resources, images[index])

        val pendingIntent = NotificationUtil.createOpenAppPendingIntent(context) {
            putExtra("noti_type_special_v2", true)
        }

        val compactView = NotificationUtil.buildRemoteView(
            context, R.layout.noti_custom_default, R.id.notification_root,
            pendingIntent, title, message, openLabel, imageBitmap, applyMiddleImage = false
        )
        val headsUpView = NotificationUtil.buildRemoteView(
            context, R.layout.noti_custom_header, R.id.notification_root,
            pendingIntent, title, message, openLabel, imageBitmap, applyMiddleImage = false
        )
        val expandedView = NotificationUtil.buildRemoteView(
            context, expandedLayoutRes, R.id.notification_root_expanded,
            pendingIntent, title, message, openLabel, imageBitmap, applyMiddleImage = true
        )

        val builder = NotificationCompat.Builder(context, NotificationUtil.CHANNEL_DOWNLOAD_OBSERVER)
            .setSmallIcon(smallIcons[index])
            .setColor(ContextCompat.getColor(context, R.color.brand_primary))
            .setContentIntent(pendingIntent)
            .setCustomContentView(compactView)
            .setCustomHeadsUpContentView(headsUpView)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        NotificationUtil.notifyIfPermitted(context, builder)
    }
}
