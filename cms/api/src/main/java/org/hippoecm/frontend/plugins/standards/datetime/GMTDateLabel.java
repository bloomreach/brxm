/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Label component to render a date (year-month-day) of the {@link Date} object in GMT timezone.
 */
@Deprecated
public class GMTDateLabel extends ZonedDateLabel {

    private static class GMTZonedDateTimeModel extends AbstractReadOnlyModel<ZonedDateTime> {
        private final IModel<Date> dateModel;

        GMTZonedDateTimeModel(IModel<Date> dateModel) {
            this.dateModel = dateModel;
        }

        @Override
        public ZonedDateTime getObject() {
            if (dateModel == null) {
                return null;
            }
            final Date date = dateModel.getObject();
            return (date != null) ? date.toInstant().atZone(ZoneId.of("GMT")) : null;
        }

        @Override
        public void detach() {
            if (this.dateModel != null) {
                this.dateModel.detach();
            }
        }
    }

    public GMTDateLabel(final String id, IModel<Date> dateModel, final FormatStyle dateStyle) {
        super(id, new GMTZonedDateTimeModel(dateModel), dateStyle);
    }
}
