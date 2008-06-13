/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.widgets;

import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

public class AjaxDateTimeField extends DateTimeField {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String amPm = "AM";

    public AjaxDateTimeField(String id, IModel model) {
        super(id, model);

        get("date").add(new ChangeBehaviour());
        get("hours").add(new ChangeBehaviour());
        get("minutes").add(new ChangeBehaviour());
        get("amOrPmChoice").add(new ChangeBehaviour());

        Date date = (Date) getModelObject();
        if (date.getHours() >= 12) {
            amPm = "PM";
        }
        String[] ampm = new String[] { "AM", "PM" };
        replace(new DropDownChoice("amOrPmChoice", new PropertyModel(this, "amPm"), Arrays.asList(ampm))
                .add(new ChangeBehaviour()));

        setOutputMarkupId(true);
    }

    // override Wicket methods; we don't use the convertedinput field
    @Override
    protected void convertInput() {
    }

    @Override
    public void updateModel() {
    }

    // callback that the ChangeBehaviour calls when one of the composing fields updates
    public void onUpdate(AjaxRequestTarget target) {
        Object dateFieldInput = getField("date").getModelObject();
        MutableDateTime date = new MutableDateTime(dateFieldInput);
        Integer hours = (Integer) getField("hours").getModelObject();
        Integer minutes = (Integer) getField("minutes").getModelObject();

        try {
            TimeZone zone = getClientTimeZone();
            if (zone != null) {
                date.setZone(DateTimeZone.forTimeZone(zone));
            }

            boolean use12HourFormat = use12HourFormat();
            if (hours != null) {
                date.set(DateTimeFieldType.hourOfDay(), hours.intValue() % (use12HourFormat ? 12 : 24));
                date.setMinuteOfHour((minutes != null) ? minutes.intValue() : 0);
            }
            if (use12HourFormat) {
                date.set(DateTimeFieldType.halfdayOfDay(), amPm.equals("PM") ? 1 : 0);
            }

            // the date will be in the server's timezone
            setModelObject(date.toDate());

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RuntimeException e) {
            error(e.getMessage());
            invalid();
        }
    }

    private FormComponent getField(String id) {
        return (FormComponent) get(id);
    }

    class ChangeBehaviour extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        ChangeBehaviour() {
            super("onChange");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            AjaxDateTimeField.this.onUpdate(target);
        }
    }
}
