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
package org.hippoecm.frontend.plugins.yui.datetime;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import java.util.Date;
import java.util.TimeZone;

/**
 * Semi-fork of YUI DateTimeField from Wicket extensions. Replaces Wicket extensions YUI behaviors with a {@link YuiDatePicker}
 * so it fit's in the Hippo ECM YUI framework.
 *
 * DatePicker can be configured using a frontend:pluginconfig node with name <code>datepicker</code>.
 *
 * @see YuiDatePickerSettings
 */

public class YuiDateTimeField extends DateTimeField {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: AjaxDateTimeField.java 19048 2009-07-28 16:08:33Z abogaart $";

    private static final long serialVersionUID = 1L;

    private boolean todayLinkVisible = true;

    public YuiDateTimeField(String id, IModel<Date> model) {
        this(id, model, new YuiDatePickerSettings());
    }

    public YuiDateTimeField(String id, IModel<Date> model, YuiDatePickerSettings settings) {
        super(id, model);

        setOutputMarkupId(true);

        //replace Wicket extensions YUI picker with our own
        DateTextField dateField = (DateTextField) get("date");
        for(IBehavior b : dateField.getBehaviors()) {
            dateField.remove(b);
        }
        dateField.add(new YuiDatePicker(settings));

        //add "Now" link
        AjaxLink<Date> today;
        add(today = new AjaxLink<Date>("today") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                MutableDateTime date = new MutableDateTime(new Date());
                boolean use12HourFormat = use12HourFormat();
                int hours = date.getHourOfDay() % (use12HourFormat ? 12 : 24);
                setHours(hours);
                setMinutes(date.getMinuteOfHour());
                setDate(date.toDate());
                if (target != null) {
                    target.addComponent(YuiDateTimeField.this);
                }
            }

            @Override
            public boolean isVisible() {
                return todayLinkVisible;
            }
        });

        today.add(new Image("current-date-img", new ResourceReference(YuiDateTimeField.class, "resources/set-now-16.png")));

        //Add change behavior to super fields
        for(String name : new String[] {"date", "hours", "minutes", "amOrPmChoice"}) {
            get(name).add(new AjaxFormComponentUpdatingBehavior("onChange") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    YuiDateTimeField.this.onUpdate(target);
                }
            });
        }
    }

    // callback that the ChangeBehaviour calls when one of the composing fields updates
    public void onUpdate(AjaxRequestTarget target) {
        updateDateTime(getDate(), getHours(), getMinutes());

        if (target != null) {
            target.addComponent(this);
        }
    }

    private void updateDateTime(Date date, Integer hours, Integer minutes) {
        MutableDateTime datetime = new MutableDateTime(date);
        try {
            TimeZone zone = getClientTimeZone();
            if (zone != null) {
                datetime.setZone(DateTimeZone.forTimeZone(zone));
            }

            if (hours != null) {
                datetime.set(DateTimeFieldType.hourOfDay(), hours % 24);
                datetime.setMinuteOfHour(minutes != null ? minutes : 0);
            }

            // the date will be in the server's timezone
            setDate(datetime.toDate());
            //setModelObject(datetime.toDate());
        } catch (RuntimeException e) {
            error(e.getMessage());
            invalid();
        }
    }

    @Override
    protected boolean use12HourFormat() {
        return false;
    }

    public void setTodayLinkVisible(boolean todayLinkVisible) {
        this.todayLinkVisible = todayLinkVisible;
    }

    @Override
    protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
        return new DateTextField(id, dateFieldModel, new StyleDateConverter(false));
    }

}
