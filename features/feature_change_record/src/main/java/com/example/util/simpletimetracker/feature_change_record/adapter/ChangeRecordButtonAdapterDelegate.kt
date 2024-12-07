package com.example.util.simpletimetracker.feature_change_record.adapter

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.createRecyclerBindingAdapterDelegate
import com.example.util.simpletimetracker.feature_change_record.model.ChangeRecordActionsBlock
import com.example.util.simpletimetracker.feature_views.extension.setOnClickWith
import com.example.util.simpletimetracker.feature_change_record.adapter.ChangeRecordButtonViewData as ViewData
import com.example.util.simpletimetracker.feature_change_record.databinding.ChangeRecordButtonItemBinding as Binding

fun createChangeRecordButtonAdapterDelegate(
    onClick: (ViewData) -> Unit,
) = createRecyclerBindingAdapterDelegate<ViewData, Binding>(
    Binding::inflate,
) { binding, item, _ ->

    with(binding) {
        item as ViewData

        tvRecordQuickActionsButton.text = item.text
        ivRecordQuickActionsButton.setImageResource(item.icon)
        cardChangeRecordActionsButton.setCardBackgroundColor(item.iconColor)
        itemRecordQuickActionsButton.isEnabled = item.isEnabled
        itemRecordQuickActionsButton.setOnClickWith(item, onClick)
    }
}

data class ChangeRecordButtonViewData(
    val block: ChangeRecordActionsBlock,
    val text: String,
    @DrawableRes val icon: Int,
    @ColorInt val iconColor: Int,
    val isEnabled: Boolean,
) : ViewHolderType {

    override fun getUniqueId(): Long = block.ordinal.toLong()

    override fun isValidType(other: ViewHolderType): Boolean =
        other is ViewData
}