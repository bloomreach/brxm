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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * Label component that represents only the date (year-month-day) of the
 * {@link ZonedDateTime} model object in current session locale format.
 *
 * @deprecated no longer used because it was only used by deprecated GMTDateLabel.
 */
@Deprecated
public class ZonedDateLabel extends Label {

    public ZonedDateLabel(final String id, final IModel<ZonedDateTime> model, final FormatStyle dateStyle) {
        super(id, new ZonedDateTimePrinterModel(model, dateStyle));
    }

    private static class ZonedDateTimePrinterModel extends AbstractReadOnlyModel<String> {
        private final IModel<ZonedDateTime> model;
        private final FormatStyle dateStyle;

        ZonedDateTimePrinterModel(final IModel<ZonedDateTime> model, final FormatStyle dateStyle) {
            this.model = model;
            this.dateStyle = dateStyle;
        }

        @Override
        public void detach() {
            if (model != null) {
                model.detach();
            }
        }

        @Override
        public String getObject() {
            final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(dateStyle);
            final DateTimeFormatter withLocale = formatter.withLocale(getLocale());

            final ZonedDateTime zonedDateTime = model.getObject();
            return zonedDateTime == null ? StringUtils.EMPTY : zonedDateTime.format(withLocale);
        }

        private static Locale getLocale() {
            return Session.get().getLocale();
        }
    }
}