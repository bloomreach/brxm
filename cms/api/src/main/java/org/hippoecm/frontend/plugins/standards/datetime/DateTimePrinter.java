/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.datetime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.session.UserSession;

/**
 * Utility for printing java date objects using the java.time classes. Takes care of setting the correct locale and
 * timezone.
 */
public interface DateTimePrinter extends IClusterable {

    /**
     * Print with default style (medium-short).
     *
     * @return the date as a string formatted in default style
     */
    String print();

    /**
     * Print with specified pattern. See <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#patterns">patterns</a>
     * for all pattern options.
     *
     * @param pattern the pattern to use, not null
     * @return the date as a string based on the pattern
     */
    String print(final String pattern);

    /**
     * Print with specified FormatStyle. Check <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/FormatStyle.html">here</a>
     * for all possible styles. The specified style will be used for both the date and the time part.
     *
     * @param style the formatter style to obtain, not null
     * @return the date as a string based on the style
     */
    String print(final FormatStyle style);
    /**
     * Print with specified FormatStyle. Check <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/FormatStyle.html">here</a>
     * for all possible styles. The specified style will be used for date only
     *
     * @param style the formatter style to obtain, not null
     * @return the date as a string based on the style
     */
    String printDate(final FormatStyle style);

    /**
     * Print with specified FormatStyles. Check <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/FormatStyle.html">here</a>
     * for all possible styles. The dateStyle will be used for the date part, the timeStyle for the time part.
     *
     * @param dateStyle the formatter style to use for the date part, not null
     * @param timeStyle the formatter style to use for the time part, not null
     * @return the date as a string based the both styles
     */
    String print(final FormatStyle dateStyle, final FormatStyle timeStyle);

    /**
     * Append an explanatory string to the printed date if it is in Daylight Saving Time.
     * Java shifts the time zone +1 if a date is in DST (e.g. CET becomes CEST), so to avoid confusion we add
     * a description after the time zone (e.g. " (DST)" in English).
     *
     * @return the DateTimePrinter instance
     */
    DateTimePrinter appendDST();

    static DateTimePrinter of(final Date date) {
        return date != null ? of(date.toInstant()) : EmptyDateTimePrinter.INSTANCE;
    }

    static DateTimePrinter of(final Calendar calendar) {
        return calendar != null ? of(calendar.toInstant()) : EmptyDateTimePrinter.INSTANCE;
    }

    static DateTimePrinter of(final Instant instant) {
        if (instant == null) {
            return EmptyDateTimePrinter.INSTANCE;
        }

        final UserSession session = UserSession.get();
        if (session == null) {
            throw new NullPointerException("Unable to retrieve user session");
        }

        final Locale locale = session.getLocale();
        final ZoneId zoneId = toZoneId(session.getTimeZone());

        return new JavaDateTimePrinter(instant, locale, zoneId);
    }

    static ZoneId toZoneId(final TimeZone timeZone) {
        return ZoneId.of(timeZone.getID(), ZoneId.SHORT_IDS);
    }

    class JavaDateTimePrinter implements DateTimePrinter {

        private final Instant instant;
        private final Locale locale;
        private final ZoneId zoneId;
        private boolean appendDST;

        private JavaDateTimePrinter(final Instant instant, final Locale locale, final ZoneId zoneId) {
            this.instant = instant;
            this.locale = locale;
            this.zoneId = zoneId;
        }

        @Override
        public DateTimePrinter appendDST() {
            appendDST = true;
            return this;
        }

        @Override
        public String print() {
            return print(FormatStyle.MEDIUM, FormatStyle.SHORT);
        }

        @Override
        public String print(final FormatStyle style) {
            return print(style, style);
        }

        @Override
        public String printDate(final FormatStyle style) {
            return print(DateTimeFormatter.ofLocalizedDate(style));
        }

        @Override
        public String print(final FormatStyle dateStyle, final FormatStyle timeStyle) {
            return print(DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle));
        }

        @Override
        public String print(final String pattern) {
            return print(DateTimeFormatter.ofPattern(pattern));
        }

        private String print(DateTimeFormatter formatter) {
            final ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, zoneId);
            formatter = formatter.withLocale(locale);
            final String dst = new ClassResourceModel("dst", JavaDateTimePrinter.class, locale, null).getObject();
            final String suffix = appendDST && isDST() ? " (" + dst + ")" : StringUtils.EMPTY;
            return dateTime.format(formatter) + suffix;
        }

        private boolean isDST() {
            return zoneId.getRules().isDaylightSavings(instant);
        }
    }

    class EmptyDateTimePrinter implements DateTimePrinter {

        private final static DateTimePrinter INSTANCE = new EmptyDateTimePrinter();

        @Override
        public String print() {
            return StringUtils.EMPTY;
        }

        @Override
        public String print(final String pattern) {
            return print();
        }

        @Override
        public String print(final FormatStyle style) {
            return print();
        }

        @Override
        public String printDate(final FormatStyle style) {
            return print();
        }

        @Override
        public String print(final FormatStyle dateStyle, final FormatStyle timeStyle) {
            return print();
        }

        @Override
        public DateTimePrinter appendDST() {
            return this;
        }
    }
}
