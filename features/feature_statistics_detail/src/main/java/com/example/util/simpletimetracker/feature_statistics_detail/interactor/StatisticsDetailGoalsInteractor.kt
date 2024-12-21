package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import com.example.util.simpletimetracker.domain.extension.getDaily
import com.example.util.simpletimetracker.domain.extension.getMonthly
import com.example.util.simpletimetracker.domain.extension.getWeekly
import com.example.util.simpletimetracker.domain.extension.value
import com.example.util.simpletimetracker.domain.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.model.DayOfWeek
import com.example.util.simpletimetracker.domain.model.RangeLength
import com.example.util.simpletimetracker.domain.model.RecordBase
import com.example.util.simpletimetracker.domain.model.RecordType
import com.example.util.simpletimetracker.domain.model.RecordTypeGoal
import com.example.util.simpletimetracker.domain.model.RecordsFilter
import com.example.util.simpletimetracker.feature_statistics_detail.interactor.StatisticsDetailChartInteractor.CompositeChartData
import com.example.util.simpletimetracker.feature_statistics_detail.mapper.StatisticsDetailViewDataMapper
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartGrouping
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartLength
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartMode
import com.example.util.simpletimetracker.feature_statistics_detail.model.ChartSplitSortMode
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailGoalsCompositeViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StatisticsDetailGoalsInteractor @Inject constructor(
    private val chartInteractor: StatisticsDetailChartInteractor,
    private val statisticsDetailViewDataMapper: StatisticsDetailViewDataMapper,
    private val prefsInteractor: PrefsInteractor,
    private val statisticsDetailGetGoalFromFilterInteractor: StatisticsDetailGetGoalFromFilterInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
) {

    // TODO compare?
    suspend fun getChartViewData(
        records: List<RecordBase>,
        filter: List<RecordsFilter>,
        currentChartGrouping: ChartGrouping,
        currentChartLength: ChartLength,
        rangeLength: RangeLength,
        rangePosition: Int,
    ): StatisticsDetailGoalsCompositeViewData = withContext(Dispatchers.Default) {
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val useProportionalMinutes = prefsInteractor.getUseProportionalMinutes()
        val useMonthDayTimeFormat = prefsInteractor.getUseMonthDayTimeFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val types = recordTypeInteractor.getAll()
        val typesMap = types.associateBy(RecordType::id)
        val typesOrder = types.map(RecordType::id)
        val goals = statisticsDetailGetGoalFromFilterInteractor.execute(filter)

        val compositeData = getChartRangeSelectionData(
            currentChartGrouping = currentChartGrouping,
            currentChartLength = currentChartLength,
            rangeLength = rangeLength,
            firstDayOfWeek = firstDayOfWeek,
            goals = goals,
        )
        val goal = getGoal(
            goals = goals,
            appliedChartGrouping = compositeData.appliedChartGrouping,
        )
        val chartMode = when (goal?.type) {
            is RecordTypeGoal.Type.Duration -> ChartMode.DURATIONS
            is RecordTypeGoal.Type.Count -> ChartMode.COUNTS
            null -> ChartMode.DURATIONS
        }
        val ranges = chartInteractor.getRanges(
            compositeData = compositeData,
            rangeLength = rangeLength,
            rangePosition = rangePosition,
            firstDayOfWeek = firstDayOfWeek,
            startOfDayShift = startOfDayShift,
            useMonthDayTimeFormat = useMonthDayTimeFormat,
        )
        val data = chartInteractor.getChartData(
            allRecords = records,
            ranges = ranges,
            typesOrder = typesOrder,
            typesMap = typesMap,
            isDarkTheme = isDarkTheme,
            chartMode = chartMode,
            splitByActivity = false,
            splitSortMode = ChartSplitSortMode.ACTIVITY_ORDER,
        )
        val prevData = chartInteractor.getPrevData(
            rangeLength = rangeLength,
            compositeData = compositeData,
            rangePosition = rangePosition,
            firstDayOfWeek = firstDayOfWeek,
            startOfDayShift = startOfDayShift,
            useMonthDayTimeFormat = useMonthDayTimeFormat,
            records = records,
            typesOrder = typesOrder,
            typesMap = typesMap,
            isDarkTheme = isDarkTheme,
            chartMode = chartMode,
            splitSortMode = ChartSplitSortMode.ACTIVITY_ORDER,
        )
        val goalValue = getGoalValue(goal)
        val goalSubtype = goal?.subtype ?: RecordTypeGoal.Subtype.Goal

        return@withContext StatisticsDetailGoalsCompositeViewData(
            viewData = statisticsDetailViewDataMapper.mapGoalChartViewData(
                goalData = statisticsDetailViewDataMapper.mapGoalData(
                    data = data,
                    goalValue = goalValue,
                    goalSubtype = goalSubtype,
                    isDarkTheme = isDarkTheme,
                ),
                goalChartPrevData = statisticsDetailViewDataMapper.mapGoalData(
                    data = prevData,
                    goalValue = goalValue,
                    goalSubtype = goalSubtype,
                    isDarkTheme = isDarkTheme,
                ),
                goalValue = goalValue,
                rangeLength = rangeLength,
                availableChartGroupings = compositeData.availableChartGroupings,
                appliedChartGrouping = compositeData.appliedChartGrouping,
                availableChartLengths = compositeData.availableChartLengths,
                appliedChartLength = compositeData.appliedChartLength,
                chartMode = chartMode,
                useProportionalMinutes = useProportionalMinutes,
                showSeconds = showSeconds,
                isDarkTheme = isDarkTheme,
            ),
            appliedChartGrouping = compositeData.appliedChartGrouping,
            appliedChartLength = compositeData.appliedChartLength,
        )
    }

    private fun getChartRangeSelectionData(
        currentChartGrouping: ChartGrouping,
        currentChartLength: ChartLength,
        rangeLength: RangeLength,
        firstDayOfWeek: DayOfWeek,
        goals: List<RecordTypeGoal>,
    ): CompositeChartData {
        val mainData = chartInteractor.getChartRangeSelectionData(
            currentChartGrouping = currentChartGrouping,
            currentChartLength = currentChartLength,
            rangeLength = rangeLength,
            firstDayOfWeek = firstDayOfWeek,
        )

        val availableChartGroupings = mainData.availableChartGroupings
            .filter { getGoal(goals, it).value != 0L }
            .takeUnless { it.isEmpty() }
            ?: listOf(ChartGrouping.DAILY)

        return mainData.copy(
            availableChartGroupings = availableChartGroupings,
            appliedChartGrouping = mainData.appliedChartGrouping
                .takeIf { it in availableChartGroupings }
                ?: availableChartGroupings.firstOrNull()
                ?: ChartGrouping.DAILY,
        )
    }

    private fun getGoal(
        goals: List<RecordTypeGoal>,
        appliedChartGrouping: ChartGrouping,
    ): RecordTypeGoal? {
        return when (appliedChartGrouping) {
            ChartGrouping.DAILY -> goals.getDaily()
            ChartGrouping.WEEKLY -> goals.getWeekly()
            ChartGrouping.MONTHLY -> goals.getMonthly()
            ChartGrouping.YEARLY -> null
        }
    }

    private fun getGoalValue(
        goal: RecordTypeGoal?,
    ): Long {
        return when (goal?.type) {
            is RecordTypeGoal.Type.Duration -> goal.value * 1000
            is RecordTypeGoal.Type.Count -> goal.value
            null -> 0L
        }
    }
}