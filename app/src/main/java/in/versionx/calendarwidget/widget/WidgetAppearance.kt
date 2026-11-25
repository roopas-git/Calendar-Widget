package `in`.versionx.calendarwidget.widget

/**
 * Per-widget-instance visual customization — the old app's "Background Color
 * Setting" section (background/text/today/tomorrow color, opacity) plus corner radius, text
 * size and the extra styling options added on top of the old app (date block color,
 * gradient background, calendar color dots, hiding the date block). Kept separate
 * from [`in`.versionx.calendarwidget.data.LookaheadSettings], which stays app-global
 * since it also drives notifications, not just one widget.
 */
data class WidgetAppearance(
    val backgroundColor: Int,
    val textColor: Int,
    val todayColor: Int,
    val backgroundOpacityPercent: Int,
    val cornerRadiusDp: Int,
    val textSizeSp: Int,
    /** Color of the big MAY / 30 / FRI block on the left. */
    val dateColor: Int = DEFAULT_DATE_COLOR,
    /** When true, the background fades diagonally from [backgroundColor] to [gradientEndColor]. */
    val gradientEnabled: Boolean = false,
    val gradientEndColor: Int = DEFAULT_GRADIENT_END,
    /** Shows a dot in each event's own calendar color before its title. */
    val showCalendarColors: Boolean = true,
    val showDateBlock: Boolean = true,
    /** Color of tomorrow's events (the old app's "Tomorrow Color"). */
    val tomorrowColor: Int = DEFAULT_TOMORROW_COLOR,
) {
    companion object {
        const val MIN_OPACITY_PERCENT = 0
        const val MAX_OPACITY_PERCENT = 100

        const val MIN_CORNER_RADIUS_DP = 0
        const val MAX_CORNER_RADIUS_DP = 32

        const val MIN_TEXT_SIZE_SP = 10
        const val MAX_TEXT_SIZE_SP = 22

        // The old app's defaults (com.mondaz.widgettest, CalendarPreferenceValue):
        // background #000000 at opacity B2 (70%), text #CCFFFFFF, "calendar" color #FFAB00
        // for today's events and the date block, tomorrow #009ACD.
        const val OLD_BACKGROUND = 0xFF000000.toInt()
        const val OLD_TEXT = 0xCCFFFFFF.toInt()
        const val OLD_AMBER = 0xFFFFAB00.toInt()
        const val OLD_TOMORROW = 0xFF009ACD.toInt()

        private const val DEFAULT_DATE_COLOR = OLD_AMBER
        private const val DEFAULT_TOMORROW_COLOR = OLD_TOMORROW
        private const val DEFAULT_GRADIENT_END = 0xFF3A1C71.toInt()

        /** Matches the old app's colors: black 70% panel, amber date and "today", blue "tomorrow". */
        val DEFAULT = WidgetAppearance(
            backgroundColor = OLD_BACKGROUND,
            textColor = OLD_TEXT,
            todayColor = OLD_AMBER,
            backgroundOpacityPercent = 70,
            cornerRadiusDp = 16,
            textSizeSp = 12,
        )
    }
}

/**
 * One-tap color themes for the config screen. Applying a preset only changes colors
 * (and gradient on/off); size, opacity, radius and toggles stay as the user set them.
 */
enum class WidgetThemePreset(
    val label: String,
    val backgroundColor: Int,
    val gradientEndColor: Int?,
    val textColor: Int,
    val todayColor: Int,
    val dateColor: Int,
    val tomorrowColor: Int,
) {
    /** Exactly the old app's colors. */
    CLASSIC(
        "Classic", WidgetAppearance.OLD_BACKGROUND, null, WidgetAppearance.OLD_TEXT,
        WidgetAppearance.OLD_AMBER, WidgetAppearance.OLD_AMBER, WidgetAppearance.OLD_TOMORROW,
    ),
    SUNSET("Sunset", 0xFFFF5F6D.toInt(), 0xFFFFC371.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFF59D.toInt(), 0xFFFFFFFF.toInt(), 0xFF4A148C.toInt()),
    OCEAN("Ocean", 0xFF0F2027.toInt(), 0xFF2C7DA0.toInt(), 0xFFE0F7FA.toInt(), 0xFF64FFDA.toInt(), 0xFF64FFDA.toInt(), 0xFF81D4FA.toInt()),
    FOREST("Forest", 0xFF134E5E.toInt(), 0xFF71B280.toInt(), 0xFFF1F8E9.toInt(), 0xFFFFEB3B.toInt(), 0xFFC5E1A5.toInt(), 0xFF80DEEA.toInt()),
    BERRY("Berry", 0xFF3A1C71.toInt(), 0xFFD76D77.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFD54F.toInt(), 0xFFFFAB91.toInt(), 0xFF80D8FF.toInt()),
    CANDY("Candy", 0xFFFC466B.toInt(), 0xFF3F5EFB.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFF8D.toInt(), 0xFFFFFFFF.toInt(), 0xFFB9F6CA.toInt()),
    LIGHT("Light", 0xFFFFFFFF.toInt(), null, 0xFF212121.toInt(), 0xFFE53935.toInt(), 0xFFE53935.toInt(), WidgetAppearance.OLD_TOMORROW),
    ;

    fun applyTo(appearance: WidgetAppearance) = appearance.copy(
        backgroundColor = backgroundColor,
        gradientEnabled = gradientEndColor != null,
        gradientEndColor = gradientEndColor ?: appearance.gradientEndColor,
        textColor = textColor,
        todayColor = todayColor,
        dateColor = dateColor,
        tomorrowColor = tomorrowColor,
    )

    fun matches(appearance: WidgetAppearance): Boolean =
        appearance.backgroundColor == backgroundColor &&
            appearance.gradientEnabled == (gradientEndColor != null) &&
            (gradientEndColor == null || appearance.gradientEndColor == gradientEndColor) &&
            appearance.textColor == textColor &&
            appearance.todayColor == todayColor &&
            appearance.dateColor == dateColor &&
            appearance.tomorrowColor == tomorrowColor
}
