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

import java.io.Serializable;
import java.time.format.FormatStyle;
import java.util.Date;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

public class DateTimeLabel extends Label {

    public DateTimeLabel(final String id, final IModel<Date> model) {
        super(id, new DateTimePrinterModel(model, DateTimePrinter::print));
    }

    public DateTimeLabel(final String id, final IModel<Date> model, final FormatStyle style) {
        super(id, new DateTimePrinterModel(model, printer -> printer.print(style)));
    }
    
    public DateTimeLabel(final String id, final IModel<Date> model, final FormatStyle dateStyle, final FormatStyle timeStyle, final boolean dateOnly) {
        super(id, new DateTimePrinterModel(model, printer -> {
            if (dateOnly) {
                return printer.printDate(dateStyle);
            } else {
                return printer.print(dateStyle, timeStyle);
            }
        }));
    }
    public DateTimeLabel(final String id, final IModel<Date> model, final FormatStyle dateStyle, final FormatStyle timeStyle) {
        super(id, new DateTimePrinterModel(model, printer -> printer.print(dateStyle, timeStyle)));
    }

    public DateTimeLabel(final String id, final IModel<Date> model, final String pattern) {
        super(id, new DateTimePrinterModel(model, printer -> printer.print(pattern)));
    }

    private interface Printer extends Serializable {
        String print(DateTimePrinter printer);
    }

    private static class DateTimePrinterModel extends AbstractReadOnlyModel<String> {
        private final IModel<Date> dateModel;
        private final Printer printer;

        private DateTimePrinterModel(final IModel<Date> dateModel, final Printer printer) {
            this.dateModel = dateModel;
            this.printer = printer;
        }

        @Override
        public String getObject() {
            final Date date = dateModel.getObject();
            return printer.print(DateTimePrinter.of(date));
        }
    }
}
