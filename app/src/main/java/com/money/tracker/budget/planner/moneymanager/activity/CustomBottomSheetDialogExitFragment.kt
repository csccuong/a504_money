package com.money.tracker.budget.planner.moneymanager.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.ads.AdsManager
import com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs
import com.money.tracker.budget.planner.moneymanager.databinding.FragmentBottomSheetExitBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomBottomSheetDialogExitFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBottomSheetExitBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomSheetExitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as? BottomSheetDialog ?: return@setOnShowListener
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { sheet ->
                    BottomSheetBehavior.from(sheet).apply {
                        isDraggable = false
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
        }

        binding.close.setOnClickListener {
            dismiss()
            activity?.finishAffinity()
        }

        loadExitNativeAd()
    }

    private fun loadExitNativeAd() {
        val hostActivity = activity ?: return
        AdsManager.loadAndShowNative(
            activity = hostActivity,
            container = binding.frAds,
            placementKey = "native_all",
            adUnitIdRes = R.string.native_all,
            layoutRes = R.layout.layout_native_small,
            isEnabled = RemoteConfigs.native_all,
            requireFullAds = false
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): CustomBottomSheetDialogExitFragment = CustomBottomSheetDialogExitFragment()
    }
}
