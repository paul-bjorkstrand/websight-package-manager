package pl.ds.websight.packagemanager.rest.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.DateUtil;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public abstract class Schedule implements Comparable<Schedule> {

    private static final Logger LOG = LoggerFactory.getLogger(Schedule.class);

    @JsonCreator
    public static Schedule createSchedule(@JsonProperty("at") String at, @JsonProperty("cron") String cron) {
        if (StringUtils.isAllBlank(at, cron) || (StringUtils.isNotBlank(at) && StringUtils.isNotBlank(cron))) {
            return new InvalidSchedule(ImmutablePair.of(Arrays.asList(at, cron).toString(), "Only one not blank value of \"at\" or " +
                    "\"cron\" should be provided"));
        }
        if (StringUtils.isNotBlank(cron)) {
            return new PeriodicSchedule(cron);
        }
        try {
            Date date = DateUtil.parseDateTime(at);
            return date.before(Calendar.getInstance().getTime()) ?
                    new InvalidSchedule(ImmutablePair.of(DateUtil.format(date), "The date should be set to future")) :
                    new DateSchedule(date);
        } catch (ParseException e) {
            String pattern = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.getPattern();
            String errorMessage = "Could not parse schedule date to format " + pattern;
            LOG.debug(errorMessage, e);
            return new InvalidSchedule(ImmutablePair.of(at, errorMessage));
        }
    }

    public abstract boolean matches(ScheduleInfo scheduleInfo);

    public abstract void addToBuilder(JobBuilder.ScheduleBuilder scheduleBuilder);

    public abstract Pair<String, String> getError();

    @Override
    public abstract int compareTo(@NotNull Schedule o);

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private static class DateSchedule extends Schedule {

        private final Date date;

        private DateSchedule(Date date) {
            this.date = date;
        }

        @Override
        public boolean matches(ScheduleInfo scheduleInfo) {
            return scheduleInfo.getAt() != null && scheduleInfo.getAt().equals(date);
        }

        @Override
        public void addToBuilder(JobBuilder.ScheduleBuilder scheduleBuilder) {
            scheduleBuilder.at(date);
        }

        @Override
        public Pair<String, String> getError() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DateSchedule that = (DateSchedule) o;
            return date.equals(that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }

        @Override
        public String toString() {
            return DateUtil.format(date);
        }

        @Override
        public int compareTo(@NotNull Schedule objectToCompare) {
            if (objectToCompare instanceof DateSchedule) {
                DateSchedule dateScheduleToCompare = (DateSchedule) objectToCompare;
                return dateScheduleToCompare.date.compareTo(date); // Date schedules should be sorted from the furthest future date to cover
                // NoSuchElementException in JobScheduler
            }
            return -1;
        }
    }

    private static class PeriodicSchedule extends Schedule {

        private final String expression;

        private PeriodicSchedule(String expression) {
            this.expression = expression;
        }

        @Override
        public boolean matches(ScheduleInfo scheduleInfo) {
            return StringUtils.isNotBlank(scheduleInfo.getExpression()) && scheduleInfo.getExpression().equals(expression);
        }

        @Override
        public void addToBuilder(JobBuilder.ScheduleBuilder scheduleBuilder) {
            scheduleBuilder.cron(expression);
        }

        @Override
        public Pair<String, String> getError() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PeriodicSchedule that = (PeriodicSchedule) o;
            return expression.equals(that.expression);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expression);
        }

        @Override
        public String toString() {
            return expression;
        }

        @Override
        public int compareTo(@NotNull Schedule o) {
            return -1; // cron expressions should be always at the beginning of schedules list
        }
    }

    private static class InvalidSchedule extends Schedule {

        private final Pair<String, String> error;

        private InvalidSchedule(Pair<String, String> error) {
            this.error = error;
        }

        @Override
        public boolean matches(ScheduleInfo scheduleInfo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addToBuilder(JobBuilder.ScheduleBuilder scheduleBuilder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pair<String, String> getError() {
            return error;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InvalidSchedule that = (InvalidSchedule) o;
            return error.equals(that.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(error);
        }

        @Override
        public int compareTo(@NotNull Schedule o) {
            return 1;
        }
    }
}
