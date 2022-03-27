package com.example.util.simpletimetracker.feature_statistics_detail.interactor

import com.example.util.simpletimetracker.core.extension.isNotFiltered
import com.example.util.simpletimetracker.core.interactor.TypesFilterInteractor
import com.example.util.simpletimetracker.core.mapper.RangeMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.model.Range
import com.example.util.simpletimetracker.domain.model.RangeLength
import com.example.util.simpletimetracker.domain.model.Record
import com.example.util.simpletimetracker.feature_statistics_detail.mapper.StatisticsDetailViewDataMapper
import com.example.util.simpletimetracker.feature_statistics_detail.model.SplitChartGrouping
import com.example.util.simpletimetracker.feature_statistics_detail.viewData.StatisticsDetailChartViewData
import com.example.util.simpletimetracker.navigation.params.screen.TypesFilterParams
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class StatisticsDetailSplitChartInteractor @Inject constructor(
    private val statisticsDetailViewDataMapper: StatisticsDetailViewDataMapper,
    private val typesFilterInteractor: TypesFilterInteractor,
    private val recordInteractor: RecordInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val timeMapper: TimeMapper,
    private val rangeMapper: RangeMapper,
) {

    suspend fun getSplitChartViewData(
        filter: TypesFilterParams,
        rangeLength: RangeLength,
        rangePosition: Int,
        splitChartGrouping: SplitChartGrouping,
    ): StatisticsDetailChartViewData {
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val range = timeMapper.getRangeStartAndEnd(
            rangeLength = rangeLength,
            shift = rangePosition,
            firstDayOfWeek = firstDayOfWeek,
            startOfDayShift = startOfDayShift
        )
        val data = getDurations(filter, range, splitChartGrouping, startOfDayShift)

        return when (splitChartGrouping) {
            SplitChartGrouping.HOURLY ->
                statisticsDetailViewDataMapper.mapToHourlyChartViewData(data)
            SplitChartGrouping.DAILY ->
                statisticsDetailViewDataMapper.mapToDailyChartViewData(data, firstDayOfWeek)
        }
    }

    suspend fun getDurationSplitViewData(
        filter: TypesFilterParams,
        rangeLength: RangeLength,
        rangePosition: Int,
    ): StatisticsDetailChartViewData {
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val range = timeMapper.getRangeStartAndEnd(
            rangeLength = rangeLength,
            shift = rangePosition,
            firstDayOfWeek = firstDayOfWeek,
            startOfDayShift = startOfDayShift
        )

        val typeIds = typesFilterInteractor.getTypeIds(filter)
        // TODO get from range and by type from db
        val records = recordInteractor.getByType(typeIds).filter { it.isNotFiltered(filter) }
        val ranges = if (range.first != 0L && range.second != 0L) {
            rangeMapper.getRecordsFromRange(records, range.first, range.second)
        } else {
            records
        }.map { Range(it.timeStarted, it.timeEnded) }

        val step: Long = TimeUnit.MINUTES.toMillis(5) // TODO calculate
        val total = ranges.size
        val buckets: MutableMap<Range, Long> = mutableMapOf()
        val minTimeStarted = ranges
            .minByOrNull { it.duration }?.duration.orZero()
            .let { nearestLower(divider = step, value = it) }
        val maxTimeEnded = ranges
            .maxByOrNull { it.duration }?.duration.orZero()
            .let { nearestUpper(divider = step, value = it) }
        (minTimeStarted..maxTimeEnded step step).forEach {
            buckets[Range(it, it + step)] = 0
        }

        var bucketTimeStarted: Long
        var bucketTimeEnded: Long
        var bucketRange: Range
        var nearestLower: Long
        var nearestUpper: Long
        var duration: Long
        ranges.forEach { it ->
            duration = it.duration
            nearestLower = nearestLower(divider = step, value = duration)
            nearestUpper = nearestUpper(divider = step, value = duration)
            bucketTimeStarted = nearestLower
            bucketTimeEnded = nearestUpper
                .takeUnless { it == duration }
                ?: (nearestUpper + step)
            bucketRange = Range(bucketTimeStarted, bucketTimeEnded)
            buckets[bucketRange] = buckets[bucketRange].orZero() + 1
        }
        val data = buckets.mapValues { (_, count) ->
            count * 100f / total
        }

        return statisticsDetailViewDataMapper.mapToDurationsSlipChartViewData(data)
    }

    /**
     * Finds next multiple of divider bigger than value.
     * Ex. value = 31, divider = 5, result 35.
     */
    private fun nearestUpper(divider: Long, value: Long): Long {
        return divider * (ceil(abs(value / divider.toFloat()))).toLong()
    }

    /**
     * Finds next multiple of divider bigger than value.
     * Ex. value = 31, divider = 5, result 30.
     */
    private fun nearestLower(divider: Long, value: Long): Long {
        return divider * (floor(abs(value / divider.toFloat()))).toLong()
    }

    private suspend fun getDurations(
        filter: TypesFilterParams,
        range: Pair<Long, Long>,
        splitChartGrouping: SplitChartGrouping,
        startOfDayShift: Long,
    ): Map<Int, Float> {
        val calendar = Calendar.getInstance()
        val dataDurations: MutableMap<Int, Long> = mutableMapOf()
        val dataTimesTracked: MutableMap<Int, Long> = mutableMapOf()

        val typeIds = typesFilterInteractor.getTypeIds(filter)
        val records = recordInteractor.getByType(typeIds).filter { it.isNotFiltered(filter) }
        val ranges = mapToRanges(records, range)
        val totalTracked = ranges.let(rangeMapper::mapToDuration)

        processRecords(calendar, ranges, splitChartGrouping, startOfDayShift).forEach {
            val grouping = mapToGrouping(calendar, it, splitChartGrouping, startOfDayShift)
            val duration = it.timeEnded - it.timeStarted
            dataDurations[grouping] = dataDurations[grouping].orZero() + duration
            dataTimesTracked[grouping] = dataTimesTracked[grouping].orZero() + 1
        }

        val daysTracked = dataTimesTracked.values.filter { it != 0L }.size

        return dataDurations.mapValues { (_, duration) ->
            when {
                totalTracked != 0L -> duration * 100f / totalTracked
                daysTracked != 0 -> 100f / daysTracked
                else -> 100f
            }
        }
    }

    private fun mapToRanges(records: List<Record>, range: Pair<Long, Long>): List<Range> {
        return if (range.first != 0L && range.second != 0L) {
            rangeMapper.getRecordsFromRange(records, range.first, range.second)
                .map { rangeMapper.clampToRange(it, range.first, range.second) }
        } else {
            records.map { Range(it.timeStarted, it.timeEnded) }
        }
    }

    private fun processRecords(
        calendar: Calendar,
        records: List<Range>,
        splitChartGrouping: SplitChartGrouping,
        startOfDayShift: Long,
    ): List<Range> {
        val processedRecords: MutableList<Range> = mutableListOf()

        records.forEach { record ->
            splitIntoRecords(calendar, record, splitChartGrouping, startOfDayShift).forEach { processedRecords.add(it) }
        }

        return processedRecords
    }

    private fun mapToGrouping(
        calendar: Calendar,
        record: Range,
        splitChartGrouping: SplitChartGrouping,
        startOfDayShift: Long,
    ): Int {
        return calendar
            .apply { timeInMillis = record.timeStarted }
            .let {
                when (splitChartGrouping) {
                    SplitChartGrouping.HOURLY -> {
                        it.get(Calendar.HOUR_OF_DAY)
                    }
                    SplitChartGrouping.DAILY -> {
                        it.timeInMillis -= startOfDayShift
                        it.get(Calendar.DAY_OF_WEEK)
                    }
                }
            }
    }

    // TODO splitting all records hourly probably is super expensive memory wise, find a better way?
    /**
     * If a record is on several days or hours - split into several records each within separate range.
     */
    private tailrec fun splitIntoRecords(
        calendar: Calendar,
        record: Range,
        splitChartGrouping: SplitChartGrouping,
        startOfDayShift: Long,
        splitRecords: MutableList<Range> = mutableListOf(),
    ): List<Range> {
        val rangeCheck = when (splitChartGrouping) {
            SplitChartGrouping.HOURLY -> timeMapper.sameHour(
                date1 = record.timeStarted,
                date2 = record.timeEnded,
                calendar = calendar
            )
            SplitChartGrouping.DAILY -> timeMapper.sameDay(
                date1 = record.timeStarted - startOfDayShift,
                date2 = record.timeEnded - startOfDayShift,
                calendar = calendar
            )
        }
        val rangeStep = when (splitChartGrouping) {
            SplitChartGrouping.HOURLY -> Calendar.HOUR_OF_DAY
            SplitChartGrouping.DAILY -> Calendar.DATE
        }

        if (rangeCheck) {
            return splitRecords.also { it.add(record) }
        }

        val adjustedCalendar = calendar.apply {
            timeInMillis = record.timeStarted
            if (splitChartGrouping == SplitChartGrouping.DAILY) timeInMillis -= startOfDayShift
            if (splitChartGrouping == SplitChartGrouping.DAILY) set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (splitChartGrouping == SplitChartGrouping.DAILY) timeInMillis += startOfDayShift
        }
        val rangeEnd = adjustedCalendar.apply { add(rangeStep, 1) }.timeInMillis

        val firstRecord = record.copy(
            timeStarted = record.timeStarted,
            timeEnded = rangeEnd
        )
        val secondRecord = record.copy(
            timeStarted = rangeEnd,
            timeEnded = record.timeEnded
        )
        splitRecords.add(firstRecord)

        return splitIntoRecords(
            calendar = calendar,
            record = secondRecord,
            splitChartGrouping = splitChartGrouping,
            startOfDayShift = startOfDayShift,
            splitRecords = splitRecords
        )
    }
}