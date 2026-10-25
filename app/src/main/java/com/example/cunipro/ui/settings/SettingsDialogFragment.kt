package com.example.cunipro.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.cunipro.R
import com.example.cunipro.databinding.DialogSettingsBinding
import com.example.cunipro.util.LanguageManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isFr = LanguageManager.isFrench(requireContext())

        updateLanguageUI(isFr)

        binding.btnSettingFr.setOnClickListener {
            LanguageManager.setLanguage(requireContext(), true)
            updateLanguageUI(true)
            dismiss()
        }

        binding.btnSettingEn.setOnClickListener {
            LanguageManager.setLanguage(requireContext(), false)
            updateLanguageUI(false)
            dismiss()
        }

        binding.btnCloseSettings.setOnClickListener {
            dismiss()
        }
    }

    private fun updateLanguageUI(isFr: Boolean) {
        if (isFr) {
            binding.btnSettingFr.setBackgroundColor(requireContext().getColor(R.color.cuni_green_primary))
            binding.btnSettingFr.setTextColor(requireContext().getColor(R.color.white))
            binding.btnSettingEn.setBackgroundColor(requireContext().getColor(R.color.cuni_green_light))
            binding.btnSettingEn.setTextColor(requireContext().getColor(R.color.cuni_text_primary))
        } else {
            binding.btnSettingEn.setBackgroundColor(requireContext().getColor(R.color.cuni_green_primary))
            binding.btnSettingEn.setTextColor(requireContext().getColor(R.color.white))
            binding.btnSettingFr.setBackgroundColor(requireContext().getColor(R.color.cuni_green_light))
            binding.btnSettingFr.setTextColor(requireContext().getColor(R.color.cuni_text_primary))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
