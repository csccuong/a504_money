package com.money.tracker.budget.planner.moneymanager.utils.language

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.LayoutLanguageCustomBinding
import com.money.tracker.budget.planner.moneymanager.model.LanguageModel
import com.money.tracker.budget.planner.moneymanager.ui.common.gone

class UILanguageCustom @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    fun interface OnLanguageInteractionListener {
        fun onLanguageInteraction(position: Int, isLanguageSelected: Boolean, languageCode: String)
    }

    private val binding: LayoutLanguageCustomBinding =
        LayoutLanguageCustomBinding.inflate(LayoutInflater.from(context), this, true)

    private val adapterEng = LanguageCustomAdapter(showHandHint = true, onItemSelected = ::onAdapterItemSelected)
    private val adapterHindi = LanguageCustomAdapter(showHandHint = false, onItemSelected = ::onAdapterItemSelected)
    private val adapterPor = LanguageCustomAdapter(showHandHint = false, onItemSelected = ::onAdapterItemSelected)
    private val adapterOther = LanguageCustomAdapter(showHandHint = false, onItemSelected = ::onAdapterItemSelected)
    private val allAdapters = listOf(adapterEng, adapterHindi, adapterPor, adapterOther)

    private var isVisibleEng = false
    private var isVisibleHindi = false
    private var isVisiblePor = false
    private var isItemLanguageSelected = false
    private var listener: OnLanguageInteractionListener? = null

    init {
        setupStaticSectionViews()
        setupRecyclerViews()
        setupListeners()
    }

    private fun setupStaticSectionViews() {
        binding.languageES.ivAvatar.setImageResource(R.drawable.flag_es)
        binding.languageFR.ivAvatar.setImageResource(R.drawable.flag_fr)
        binding.languageHindi.ivAvatar.setImageResource(R.drawable.flag_hi)
        binding.languagePor.ivAvatar.setImageResource(R.drawable.flag_collap_pt)
        binding.languageEnglishCollapse.ivAvatar.setImageResource(R.drawable.flag_collap_en)

        binding.languageHindi.imgCountries.gone()
        binding.languageHindi.animHand.gone()
        binding.languagePor.animHand.gone()
        binding.languagePor.imgCountries.setImageResource(R.drawable.img_por)

        refreshSectionTitles()
    }

    private fun setupRecyclerViews() {
        binding.rcvLanguageCollap1.adapter = adapterEng
        binding.rcvLanguageCollap2.adapter = adapterHindi
        binding.rcvLanguageCollap3.adapter = adapterPor
        binding.rcvLanguage4.adapter = adapterOther
    }

    private fun setupListeners() {
        binding.languageES.llNotColap.setOnClickListener {
            selectDirectLanguage(selectedCode = "es", isSpanish = true)
        }

        binding.languageFR.llNotColap.setOnClickListener {
            selectDirectLanguage(selectedCode = "fr", isSpanish = false)
        }

        binding.languageHindi.itemCollap.setOnClickListener {
            isVisibleHindi = !isVisibleHindi
            binding.rcvLanguageCollap2.visibility = if (isVisibleHindi) View.VISIBLE else View.GONE
            onSectionToggled()
        }

        binding.languageEnglishCollapse.itemCollap.setOnClickListener {
            isVisibleEng = !isVisibleEng
            binding.rcvLanguageCollap1.visibility = if (isVisibleEng) View.VISIBLE else View.GONE
            if (isVisibleEng) {
                binding.languageEnglishCollapse.itemCollap.postDelayed({
                    binding.nestedScrollView.smoothScrollTo(0, binding.languageEnglishCollapse.itemCollap.bottom)
                }, 100L)
            }
            onSectionToggled()
        }

        binding.languagePor.itemCollap.setOnClickListener {
            isVisiblePor = !isVisiblePor
            binding.rcvLanguageCollap3.visibility = if (isVisiblePor) View.VISIBLE else View.GONE
            onSectionToggled()
        }
    }

    private fun selectDirectLanguage(selectedCode: String, isSpanish: Boolean) {
        binding.languageES.imgSelected.setImageResource(
            if (isSpanish) R.drawable.ic_checked_language else R.drawable.ic_unchecked_language
        )
        binding.languageFR.imgSelected.setImageResource(
            if (isSpanish) R.drawable.ic_unchecked_language else R.drawable.ic_checked_language
        )
        allAdapters.forEach { it.unselectAll() }
        adapterEng.hideAnimHand()
        binding.languageEnglishCollapse.animHand.gone()

        isItemLanguageSelected = true
        listener?.onLanguageInteraction(0, true, selectedCode)
    }

    private fun onSectionToggled() {
        listener?.onLanguageInteraction(0, isItemLanguageSelected, "")
        binding.languageEnglishCollapse.animHand.gone()
    }

    private fun onAdapterItemSelected(
        position: Int,
        language: LanguageModel,
        selectedAdapter: LanguageCustomAdapter
    ) {
        isItemLanguageSelected = true
        listener?.onLanguageInteraction(position, true, language.isoLanguage)

        binding.languageFR.imgSelected.setImageResource(R.drawable.ic_unchecked_language)
        binding.languageES.imgSelected.setImageResource(R.drawable.ic_unchecked_language)
        allAdapters.filter { it !== selectedAdapter }.forEach { it.unselectAll() }
        adapterEng.hideAnimHand()
    }

    private fun refreshSectionTitles() {
        binding.languageES.tvTitle.text = context.getString(R.string.spanish)
        binding.languageHindi.tvTitle.text = context.getString(R.string.hindi)
        binding.languageFR.tvTitle.text = context.getString(R.string.french)
        binding.languagePor.tvTitle.text = context.getString(R.string.portuguese)
        binding.languageEnglishCollapse.tvTitle.text = context.getString(R.string.english)
    }

    fun updateData(
        englishList: List<LanguageModel>,
        indianList: List<LanguageModel>,
        portugueseList: List<LanguageModel>,
        otherList: List<LanguageModel>
    ) {
        refreshSectionTitles()
        adapterEng.submitList(englishList)
        adapterHindi.submitList(indianList)
        adapterPor.submitList(portugueseList)
        adapterOther.submitList(otherList)
    }

    fun selectLanguageCode(langCode: String) {
        val normalized = langCode.trim().lowercase()
        val base = normalized.substringBefore("-").substringBefore("_")

        binding.languageES.imgSelected.setImageResource(R.drawable.ic_unchecked_language)
        binding.languageFR.imgSelected.setImageResource(R.drawable.ic_unchecked_language)
        allAdapters.forEach { it.unselectAll() }
        binding.languageEnglishCollapse.animHand.gone()

        when (base) {
            "es" -> {
                binding.languageES.imgSelected.setImageResource(R.drawable.ic_checked_language)
                isItemLanguageSelected = true
            }
            "fr" -> {
                binding.languageFR.imgSelected.setImageResource(R.drawable.ic_checked_language)
                isItemLanguageSelected = true
            }
            else -> {
                val matched = adapterOther.selectByCode(normalized) ||
                    adapterEng.selectByCode(normalized) ||
                    adapterHindi.selectByCode(normalized) ||
                    adapterPor.selectByCode(normalized)
                isItemLanguageSelected = matched
            }
        }
    }

    fun setOnLanguageInteractionListener(listener: OnLanguageInteractionListener?) {
        this.listener = listener
    }
}
