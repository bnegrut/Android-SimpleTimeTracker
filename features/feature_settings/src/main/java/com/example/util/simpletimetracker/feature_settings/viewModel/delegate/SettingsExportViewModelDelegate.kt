package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.domain.extension.flip
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsExportViewDataInteractor
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsExportViewModelDelegate @Inject constructor(
    private val settingsExportViewDataInteractor: SettingsExportViewDataInteractor,
) : ViewModelDelegate() {

    private var parent: SettingsParent? = null
    private var isCollapsed: Boolean = true

    fun init(parent: SettingsParent) {
        this.parent = parent
    }

    suspend fun getViewData(): List<ViewHolderType> {
        return settingsExportViewDataInteractor.execute(
            isCollapsed = isCollapsed,
        )
    }

    fun onCollapseClick() = delegateScope.launch {
        isCollapsed = isCollapsed.flip()
        parent?.updateContent()
    }
}