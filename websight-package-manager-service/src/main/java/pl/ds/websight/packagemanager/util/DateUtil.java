package pl.ds.websight.packagemanager.util;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class DateUtil {

    public static final String DATE_TIME_FORMAT_PATTERN = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.getPattern();
    public static final String DATE_TIME_ZONE_FORMAT_PATTERN = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.getPattern();

    private DateUtil() {
        // no instances
    }

    /**
     * Parses local date. Supported format is {@code yyyy-MM-dd'T'HH:mm:ss}.
     *
     * @param dateString date to parse in
     * @return parsed date or {@code null} if it cannot be parsed
     */
    public static Date parseDateTime(String dateString) throws ParseException {
        return DateUtils.parseDate(dateString, DATE_TIME_FORMAT_PATTERN);
    }

    /**
     * Formats date to ISO 8601 extended date time format ({@code yyyy-MM-dd'T'HH:mm:ssZZ}).
     * This format is supported by JavaScript date parser, so can be used for returning dates
     * consumed by frontend apps. If {@code date} is {@code null}, then {@code null} is returned.
     *
     * @param date date to format
     * @return formatted date, eg. {@code 2011-10-10T14:48:00.000+09:00}
     */
    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return DateFormatUtils.format(date, DATE_TIME_ZONE_FORMAT_PATTERN, getTimeZone());
    }

    /**
     * Formats calendar to ISO 8601 extended date time format ({@code yyyy-MM-dd'T'HH:mm:ssZZ}).
     * This format is supported by JavaScript date parser, so can be used for returning dates
     * consumed by frontend apps. If {@code calendar} is {@code null}, then {@code null} is returned.
     *
     * @param calendar calendar to format
     * @return formatted calendar, eg. {@code 2011-10-10T14:48:00.000+09:00}
     */
    public static String format(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return DateFormatUtils.format(calendar, DATE_TIME_ZONE_FORMAT_PATTERN, getTimeZone());
    }

    private static TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

}
