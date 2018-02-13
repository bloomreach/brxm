/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
 * timezone. Prints the date but excludes the time.
 */
public interface DatePrinter extends IClusterable {

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
     * for all possible styles.
     *
     * @param style the formatter style to obtain, not null
     * @return the date as a string based on the style
     */
    String print(final FormatStyle style);

    /**
     * Append an explanatory string to the printed date if it is in Daylight Saving Time.
     * Java shifts the time zone +1 if a date is in DST (e.g. CET becomes CEST), so to avoid confusion we add
     * a description after the time zone (e.g. " (DST)" in English).
     *
     * @return the DatePrinter instance
     */
    DatePrinter appendDST();

    static DatePrinter of(final Date date) {
        return date != null ? of(date.toInstant()) : EmptyDatePrinter.INSTANCE;
    }

    static DatePrinter of(final Calendar calendar) {
        return calendar != null ? of(calendar.toInstant()) : EmptyDatePrinter.INSTANCE;
    }

    static DatePrinter of(final Instant instant) {
        return instant != null ? new JavaDatePrinter(instant) : EmptyDatePrinter.INSTANCE;
    }

    class JavaDatePrinter implements DatePrinter {

        private final Instant instant;
        private final Locale locale;
        private final ZoneId zoneId;
        private boolean appendDST;

        JavaDatePrinter(final Instant instant) {
            this.instant = instant;

            final UserSession session = UserSession.get();
            if (session == null) {
                throw new NullPointerException("Unable to retrieve user session");
            }

            locale = session.getLocale();
            zoneId = toZoneId(session.getTimeZone());
        }

        private static ZoneId toZoneId(final TimeZone timeZone) {
            return ZoneId.of(timeZone.getID(), ZoneId.SHORT_IDS);
        }

        @Override
        public DatePrinter appendDST() {
            appendDST = true;
            return this;
        }

        @Override
        public String print() {
            return print(FormatStyle.MEDIUM);
        }

        @Override
        public String print(final FormatStyle style) {
            return print(DateTimeFormatter.ofLocalizedDate(style));
        }

        @Override
        public String print(final String pattern) {
            return print(DateTimeFormatter.ofPattern(pattern));
        }

        String print(DateTimeFormatter formatter) {
            final ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, zoneId);
            formatter = formatter.withLocale(locale);

            return dateTime.format(formatter) + getSuffix();
        }

        private String getSuffix() {
            final String dst = new ClassResourceModel("dst", JavaDatePrinter.class, locale, null).getObject();
            return appendDST && isDST() ? " (" + dst + ")" : StringUtils.EMPTY;
        }

        private boolean isDST() {
            return zoneId.getRules().isDaylightSavings(instant);
        }
    }

    class EmptyDatePrinter implements DatePrinter {

        private final static DatePrinter INSTANCE = new EmptyDatePrinter();

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
        public DatePrinter appendDST() {
            return this;
        }
    }
}
