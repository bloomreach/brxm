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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility for printing java date objects using the java.time classes. Takes care of setting the correct locale and
 * timezone and prints the date and the time.
 */
public interface DateTimePrinter extends DatePrinter {

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
     * Override appendDST to return a more specific implementation DateTimePrinter and allow proper method chaining.
     *
     * @see DatePrinter#appendDST()
     * @return the DateTimePrinter instance
     */
    @Override
    DateTimePrinter appendDST();

    static DateTimePrinter of(final Date date) {
        return date != null ? of(date.toInstant()) : EmptyDateTimePrinter.INSTANCE;
    }

    static DateTimePrinter of(final Calendar calendar) {
        return calendar != null ? of(calendar.toInstant()) : EmptyDateTimePrinter.INSTANCE;
    }

    static DateTimePrinter of(final Instant instant) {
        return instant != null ? new JavaDateTimePrinter(instant) : EmptyDateTimePrinter.INSTANCE;
    }

    class JavaDateTimePrinter extends JavaDatePrinter implements DateTimePrinter {

        JavaDateTimePrinter(final Instant instant) {
            super(instant);
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
        public String print(final FormatStyle dateStyle, final FormatStyle timeStyle) {
            return print(DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle));
        }

        @Override
        public DateTimePrinter appendDST() {
            super.appendDST();
            return this;
        }
    }

    class EmptyDateTimePrinter extends EmptyDatePrinter implements DateTimePrinter {

        private final static DateTimePrinter INSTANCE = new EmptyDateTimePrinter();

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
