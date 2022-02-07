/*
 *  Copyright 2016-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.time.format.FormatStyle;
import java.util.Date;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

public class DateTimeLabel extends Label {

    public DateTimeLabel(final String id, final IModel<Date> model) {
        super(id, () -> DateTimePrinter.of(model.getObject()).print());
    }

    public DateTimeLabel(final String id, final IModel<Date> model, final FormatStyle style) {
        super(id, () -> DateTimePrinter.of(model.getObject()).print(style));
    }

    public DateTimeLabel(final String id, final IModel<Date> model, final FormatStyle dateStyle, final FormatStyle timeStyle) {
        super(id, () -> DateTimePrinter.of(model.getObject()).print(dateStyle, timeStyle));
    }

    public DateTimeLabel(final String id, final IModel<Date> model, final String pattern) {
        super(id, () -> DateTimePrinter.of(model.getObject()).print(pattern));
    }
}
