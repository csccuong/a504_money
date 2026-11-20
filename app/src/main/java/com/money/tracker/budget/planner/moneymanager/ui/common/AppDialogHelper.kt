package com.money.tracker.budget.planner.moneymanager.ui.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.DialogConfirmBinding

object AppDialogHelper {

    fun createStyledDialog(
        context: Context,
        contentView: View,
        widthRatio: Float = 0.90f,
        cancelable: Boolean = true
    ): Dialog {
        val dialog = Dialog(context, R.style.AppDialogTheme)
        dialog.setContentView(contentView)
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)
        dialog.window?.let { window ->
            val maxWidthPx = (420 * context.resources.displayMetrics.density).toInt()
            val targetWidth = ((context.resources.displayMetrics.widthPixels * widthRatio).toInt())
                .coerceAtMost(maxWidthPx)
            window.setLayout(targetWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            SystemBarsHelper.applyImmersiveSticky(window)
        }
        return dialog
    }

    fun showConfirmDialog(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        @StringRes positiveTextRes: Int = R.string.delete,
        @StringRes negativeTextRes: Int = R.string.cancel,
        isDestructive: Boolean = true,
        @DrawableRes iconRes: Int = R.drawable.ic_delete_clean,
        onConfirm: () -> Unit
    ) {
        val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
        val dialog = createStyledDialog(context, binding.root, widthRatio = 0.88f)

        binding.tvDialogTitle.text = context.getString(titleRes)
        binding.tvDialogMessage.text = context.getString(messageRes)
        binding.btnNegative.text = context.getString(negativeTextRes)
        binding.btnPositive.text = context.getString(positiveTextRes)
        binding.ivDialogIcon.setImageResource(iconRes)

        if (isDestructive) {
            binding.iconBadgeContainer.setBackgroundResource(R.drawable.bg_dialog_icon_danger)
            binding.ivDialogIcon.setColorFilter(context.compatColor(R.color.semantic_expense))
            binding.btnPositive.setBackgroundResource(R.drawable.bg_btn_danger_solid)
        } else {
            binding.iconBadgeContainer.setBackgroundResource(R.drawable.bg_dialog_icon_primary)
            binding.ivDialogIcon.setColorFilter(context.compatColor(R.color.brand_primary))
            binding.btnPositive.setBackgroundResource(R.drawable.bg_save_button)
        }

        binding.btnNegative.setOnClickListener { dialog.dismiss() }
        binding.btnPositive.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }
}
