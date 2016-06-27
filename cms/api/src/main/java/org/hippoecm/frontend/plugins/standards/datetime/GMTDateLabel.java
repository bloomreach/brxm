/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standards.datetime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.util.Date;

import org.apache.wicket.model.Model;

/**
 * Label component to render a date (year-month-day) of the {@link Date} object in GMT timezone.
 */
public class GMTDateLabel extends ZonedDateLabel {
    public GMTDateLabel(final String id, Date date, final FormatStyle dateStyle) {
        super(id, toGMTDate(date), dateStyle);
    }

    public static Model<ZonedDateTime> toGMTDate(Date date) {
        final ZonedDateTime gmtDate = date == null ? null : date.toInstant().atZone(ZoneId.of("GMT"));
        return Model.of(gmtDate);
    }
}
