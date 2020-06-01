/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import java.util.Calendar;
import java.util.Date;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.FutureDateValidator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.yui.datetime.YuiDateTimeField;

public class DatePickerComponent extends Panel {
    public DatePickerComponent(String id, IModel<Date> dateModel, IModel<String> label) {
        super(id);

        add(new Label("question", label));
        add(CssClass.append("date-picker-container"));

        Calendar minimum = Calendar.getInstance();
        minimum.setTime(dateModel.getObject());
        minimum.set(Calendar.SECOND, 0);
        minimum.set(Calendar.MILLISECOND, 0);

        // if you want to round upwards, the following ought to be executed: minimum.add(Calendar.MINUTE, 1);
        dateModel.setObject(minimum.getTime());
        YuiDateTimeField ydtf = new YuiDateTimeField("value", dateModel);
        ydtf.add(new FutureDateValidator());
        add(ydtf);
    }

    public FormComponent<Date> getDateTimeField() {
        return (FormComponent<Date>) get("value");
    }
}
