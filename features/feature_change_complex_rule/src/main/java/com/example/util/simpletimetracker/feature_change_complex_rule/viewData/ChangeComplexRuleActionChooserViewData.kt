package com.example.util.simpletimetracker.feature_change_complex_rule.viewData

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType

data class ChangeComplexRuleActionChooserViewData(
    val title: String,
    val selectedCount: Int,
    val viewData: List<ViewHolderType>,
)