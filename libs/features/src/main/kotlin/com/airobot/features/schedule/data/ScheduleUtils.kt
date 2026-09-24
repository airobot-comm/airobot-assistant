package com.airobot.features.schedule.data

import java.util.Calendar

data class TodayInfo(
    val festival: String?,
    val solarTerm: String?,
    val weatherCondition: String,
    val weatherTemp: String,
    val weatherAdvice: String,
    val lunarDate: String,
    val aiAdvice: String
)

object ScheduleUtils {
    private val FESTIVALS = mapOf(
        "1-1" to "元旦",
        "2-14" to "情人节",
        "3-8" to "妇女节",
        "3-12" to "植树节",
        "5-1" to "劳动节",
        "5-4" to "青年节",
        "6-1" to "儿童节",
        "8-1" to "建军节",
        "9-10" to "教师节",
        "10-1" to "国庆节",
        "12-24" to "平安夜",
        "12-25" to "圣诞节"
    )

    private val SOLAR_TERMS = mapOf(
        "2-3" to "立春",
        "2-18" to "雨水",
        "3-5" to "惊蛰",
        "3-20" to "春分",
        "4-4" to "清明",
        "4-19" to "谷雨",
        "5-5" to "立夏",
        "5-20" to "小满",
        "6-5" to "芒种",
        "6-21" to "夏至",
        "7-6" to "小暑",
        "7-22" to "大暑",
        "8-7" to "立秋",
        "8-22" to "处暑",
        "9-7" to "白露",
        "9-22" to "秋分",
        "10-8" to "寒露",
        "10-23" to "霜降",
        "11-7" to "立冬",
        "11-22" to "小雪",
        "12-6" to "大雪",
        "12-21" to "冬至",
        "1-5" to "小寒",
        "1-20" to "大寒"
    )

    private val WEATHER_MOCKS = listOf(
        WeatherMock("晴朗", "12°C", "阳光明媚，紫外线较强。建议穿单层长袖或薄外套，出门记得涂抹防晒霜并佩戴墨镜。"),
        WeatherMock("多云", "8°C", "云层较厚，气温适中。建议穿针织衫加防风外套，适合户外散步或慢跑。"),
        WeatherMock("小雨", "5°C", "有降水概率，体感湿冷。建议穿防水外套或带伞，注意保暖，路面湿滑请小心慢行。"),
        WeatherMock("微风", "15°C", "春风拂面，非常舒适。建议穿轻薄透气的春装，适合开窗通风或进行户外踏青。")
    )

    private val LUNAR_MOCKS = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val LUNAR_MONTHS = listOf(
        "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    )

    private val ADVICE_LIST = listOf(
        "今天是高效工作的好时机，保持专注！",
        "记得多喝水，保持身体水分充足。",
        "适合学习新技能，哪怕只是10分钟。",
        "给家人或朋友打个电话吧，分享你的快乐。",
        "晚上早点休息，保证充足的睡眠。",
        "保持微笑，好运会伴随你一整天！"
    )

    private data class WeatherMock(
        val condition: String,
        val temp: String,
        val advice: String
    )

    fun getTodayInfo(calendar: Calendar): TodayInfo {
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val key = "$month-$day"

        val festival = FESTIVALS[key]
        val solarTerm = SOLAR_TERMS[key]
        val weather = WEATHER_MOCKS[day % WEATHER_MOCKS.size]

        val hash = day + month * 31
        val lunarMonth = LUNAR_MONTHS[month % LUNAR_MONTHS.size]
        val lunarDay = LUNAR_MOCKS[hash % LUNAR_MOCKS.size]
        val lunarDate = "农历${lunarMonth}月${lunarDay}"

        val aiAdvice = ADVICE_LIST[day % ADVICE_LIST.size]

        return TodayInfo(
            festival = festival,
            solarTerm = solarTerm,
            weatherCondition = weather.condition,
            weatherTemp = weather.temp,
            weatherAdvice = weather.advice,
            lunarDate = lunarDate,
            aiAdvice = aiAdvice
        )
    }

    fun getLocalDateString(calendar: Calendar): String {
        return String.format(java.util.Locale.US, "%d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
