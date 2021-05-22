package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.adapter.ViewHolderType
import com.example.util.simpletimetracker.core.adapter.category.CategoryViewData
import com.example.util.simpletimetracker.core.adapter.empty.EmptyViewData
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.viewData.RecordTypeIcon
import com.example.util.simpletimetracker.domain.model.Category
import com.example.util.simpletimetracker.domain.model.RecordTag
import com.example.util.simpletimetracker.domain.model.RecordType
import javax.inject.Inject

class CategoryViewDataMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    private val iconMapper: IconMapper,
    private val resourceRepo: ResourceRepo
) {

    fun mapActivityTag(
        category: Category,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false
    ): CategoryViewData.Activity {
        return CategoryViewData.Activity(
            id = category.id,
            name = category.name,
            iconColor = getTextColor(isDarkTheme, isFiltered),
            color = getColor(category.color, isDarkTheme, isFiltered)
        )
    }

    fun mapRecordTag(
        tag: RecordTag,
        type: RecordType,
        isDarkTheme: Boolean,
        isFiltered: Boolean = false,
        showIcon: Boolean = true
    ): CategoryViewData.Record {
        val icon = type.icon.let(iconMapper::mapIcon)

        return CategoryViewData.Record(
            id = tag.id,
            name = tag.name,
            iconColor = getTextColor(isDarkTheme, isFiltered),
            iconAlpha = getIconAlpha(icon, isFiltered),
            color = getColor(type.color, isDarkTheme, isFiltered),
            icon = if (showIcon) icon else null
        )
    }

    fun mapRecordTagUntagged(
        isDarkTheme: Boolean,
        showIcon: Boolean
    ): CategoryViewData.Record {
        return CategoryViewData.Record(
            id = 0L,
            name = R.string.change_record_untagged.let(resourceRepo::getString),
            iconColor = getTextColor(isDarkTheme, false),
            color = colorMapper.toUntrackedColor(isDarkTheme),
            icon = if (showIcon) RecordTypeIcon.Image(R.drawable.unknown) else null
        )
    }

    fun mapRecordTagUntyped(
        tag: RecordTag,
        isDarkTheme: Boolean
    ): CategoryViewData.Record {
        return CategoryViewData.Record(
            id = 0L,
            name = tag.name,
            iconColor = getTextColor(isDarkTheme, false),
            color = colorMapper.toUntrackedColor(isDarkTheme),
            icon = RecordTypeIcon.Image(R.drawable.unknown)
        )
    }

    fun mapToRecordTagsEmpty(): List<ViewHolderType> {
        return EmptyViewData(
            message = resourceRepo.getString(R.string.change_record_categories_empty)
        ).let(::listOf)
    }

    fun mapToTypeNotSelected(): List<ViewHolderType> {
        return EmptyViewData(
            message = resourceRepo.getString(R.string.change_record_activity_not_selected)
        ).let(::listOf)
    }

    private fun getTextColor(
        isDarkTheme: Boolean,
        isFiltered: Boolean
    ): Int {
        return if (isFiltered) {
            colorMapper.toFilteredIconColor(isDarkTheme)
        } else {
            colorMapper.toIconColor(isDarkTheme)
        }
    }

    private fun getColor(
        colorId: Int,
        isDarkTheme: Boolean,
        isFiltered: Boolean
    ): Int {
        return if (isFiltered) {
            colorMapper.toFilteredColor(isDarkTheme)
        } else {
            colorId
                .let { colorMapper.mapToColorResId(it, isDarkTheme) }
                .let(resourceRepo::getColor)
        }
    }

    private fun getIconAlpha(icon: RecordTypeIcon, isFiltered: Boolean): Float {
        return if (icon is RecordTypeIcon.Emoji && isFiltered) {
            FILTERED_ICON_EMOJI_ALPHA
        } else {
            DEFAULT_ICON_EMOJI_ALPHA
        }
    }

    companion object {
        private const val DEFAULT_ICON_EMOJI_ALPHA = 1.0f
        private const val FILTERED_ICON_EMOJI_ALPHA = 0.3f
    }
}