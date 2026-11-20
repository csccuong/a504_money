package com.money.tracker.budget.planner.moneymanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.viewpager.widget.PagerAdapter
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.ItemSlideLayoutBinding

class SlideAdapter(private val context: Context) : PagerAdapter() {

    private data class SlidePage(
        @DrawableRes val imageRes: Int,
        @StringRes val titleRes: Int
    )

    private val slides = listOf(
        SlidePage(R.drawable.intro1, R.string.note_continue1),
        SlidePage(R.drawable.intro2, R.string.note_continue2),
        SlidePage(R.drawable.intro3, R.string.note_continue3),
        SlidePage(R.drawable.intro4, R.string.note_continue4)
    )

    override fun getCount(): Int = slides.size

    override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val binding = ItemSlideLayoutBinding.inflate(LayoutInflater.from(context), container, false)
        val slide = slides[position]
        binding.imLogoSlide.setImageResource(slide.imageRes)
        binding.tvTitle.setText(slide.titleRes)
        container.addView(binding.root)
        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }
}
