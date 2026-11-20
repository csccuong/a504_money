package com.money.tracker.budget.planner.moneymanager.notiSpecial

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.money.tracker.budget.planner.moneymanager.R
import com.money.tracker.budget.planner.moneymanager.databinding.FragmentPerNotiBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetPerNotiFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPerNotiBinding? = null
    private val binding get() = requireNotNull(_binding)

    var onSetClick: (() -> Unit)? = null
    var onDontSetClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerNotiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureBottomSheet()
        setupClickListeners()
    }

    private fun configureBottomSheet() {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as? BottomSheetDialog ?: return@setOnShowListener

            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.touch_outside)
                ?.setOnClickListener { dismissAsDeclined() }

            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { sheet ->
                    BottomSheetBehavior.from(sheet).apply {
                        isDraggable = false
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                    sheet.background = ContextCompat.getDrawable(requireContext(), android.R.color.transparent)
                }
        }
    }

    private fun setupClickListeners() {
        binding.set.setOnClickListener {
            onSetClick?.invoke()
            dismiss()
        }
        binding.notNow.setOnClickListener { dismissAsDeclined() }
        binding.later.setOnClickListener { dismissAsDeclined() }
    }

    private fun dismissAsDeclined() {
        onDontSetClick?.invoke()
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnCancelListener { onDontSetClick?.invoke() }
        return bottomSheetDialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): BottomSheetPerNotiFragment = BottomSheetPerNotiFragment()
    }
}
