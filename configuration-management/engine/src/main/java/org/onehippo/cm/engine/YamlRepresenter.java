/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

class YamlRepresenter extends Representer {

    /**
     * Copied from Hippo provided fix in https://bitbucket.org/asomov/snakeyaml/issues/364/
     */
    protected class RepresentDate implements Represent {
        public Node representData(Object data) {
            // because SimpleDateFormat ignores timezone we have to use Calendar
            Calendar calendar;
            if (data instanceof Calendar) {
                calendar = (Calendar)data;
            } else {
                calendar = Calendar.getInstance(getTimeZone() == null ? TimeZone.getTimeZone("UTC")
                        : timeZone);
                calendar.setTime((Date)data);
            }
            int years = calendar.get(Calendar.YEAR);
            int months = calendar.get(Calendar.MONTH) + 1; // 0..12
            int days = calendar.get(Calendar.DAY_OF_MONTH); // 1..31
            int hour24 = calendar.get(Calendar.HOUR_OF_DAY); // 0..24
            int minutes = calendar.get(Calendar.MINUTE); // 0..59
            int seconds = calendar.get(Calendar.SECOND); // 0..59
            int millis = calendar.get(Calendar.MILLISECOND);
            StringBuilder buffer = new StringBuilder(String.valueOf(years));
            while (buffer.length() < 4) {
                // ancient years
                buffer.insert(0, "0");
            }
            buffer.append("-");
            if (months < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(months));
            buffer.append("-");
            if (days < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(days));
            buffer.append("T");
            if (hour24 < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(hour24));
            buffer.append(":");
            if (minutes < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(minutes));
            buffer.append(":");
            if (seconds < 10) {
                buffer.append("0");
            }
            buffer.append(String.valueOf(seconds));
            if (millis > 0) {
                if (millis < 10) {
                    buffer.append(".00");
                } else if (millis < 100) {
                    buffer.append(".0");
                } else {
                    buffer.append(".");
                }
                buffer.append(String.valueOf(millis));
            }

            // Get the offset from GMT taking DST into account
            int gmtOffset = calendar.getTimeZone().getOffset(calendar.get(Calendar.ERA),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.DAY_OF_WEEK),
                    calendar.get(Calendar.MILLISECOND));
            if (gmtOffset == 0) {
                buffer.append('Z');
            } else {
                if (gmtOffset < 0) {
                    buffer.append('-');
                    gmtOffset *= -1;
                } else {
                    buffer.append('+');
                }
                int minutesOffset = gmtOffset / (60 * 1000);
                int hoursOffset = minutesOffset / 60;
                int partOfHour = minutesOffset % 60;

                if (hoursOffset < 10) {
                    buffer.append('0');
                }
                buffer.append(hoursOffset);
                buffer.append(':');
                if (partOfHour < 10) {
                    buffer.append('0');
                }
                buffer.append(partOfHour);
            }

            return representScalar(getTag(data.getClass(), Tag.TIMESTAMP), buffer.toString(), null);
        }
    }

    public YamlRepresenter() {
        super();
        this.multiRepresenters.put(Calendar.class, new RepresentDate());
    }

}
