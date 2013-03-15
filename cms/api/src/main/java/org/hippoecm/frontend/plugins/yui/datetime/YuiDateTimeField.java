/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Date;
import java.util.TimeZone;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Semi-fork of YUI DateTimeField from Wicket extensions. Replaces Wicket extensions YUI behaviors with a {@link YuiDatePicker}
 * so it fit's in the Hippo ECM YUI framework.
 *
 * DatePicker can be configured using a frontend:pluginconfig node with name <code>datepicker</code>.
 *
 * @see YuiDatePickerSettings
 */

public class YuiDateTimeField extends DateTimeField {

    private static final long serialVersionUID = 1L;

    public static final String MINUTES_LABEL = "minutes-label";
    public static final String HOURS_LABEL = "hours-label";
    public static final String DATE_LABEL = "date-label";

    private boolean todayLinkVisible = true;

    private YuiDatePickerSettings settings;

    public YuiDateTimeField(String id, IModel<Date> model) {
        this(id, model, null);
    }

    public YuiDateTimeField(String id, IModel<Date> model, YuiDatePickerSettings settings) {
        super(id, model);

        if (settings == null) {
            settings = new YuiDatePickerSettings();
        }

        this.settings = settings;

        setOutputMarkupId(true);

        //replace Wicket extensions YUI picker with our own
        DateTextField dateField = (DateTextField) get("date");
        for (IBehavior b : dateField.getBehaviors()) {
            dateField.remove(b);
        }
        dateField.add(new YuiDatePicker(settings));
        IModel<String> sizeModel = new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return Integer.valueOf(YuiDateTimeField.this.settings.getDatePattern().length() + 2).toString();
            }

        };
        dateField.add(new AttributeModifier("size", sizeModel));
        dateField.add(new AttributeModifier("maxlength", sizeModel));
        dateField.add(new AttributeModifier("style", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                int size = YuiDateTimeField.this.settings.getDatePattern().length() + 2;
                return "width: " + (size * 6 + 7) + "px;";
            }

        }));

        dateField.setLabel(new StringResourceModel(DATE_LABEL, this, null));

        final TextField hoursField = (TextField) get("hours");
        hoursField.setLabel(new StringResourceModel(HOURS_LABEL, this, null));

        final TextField minutesField = (TextField) get("minutes");
        minutesField.setLabel(new StringResourceModel(MINUTES_LABEL, this, null));

        //add "Now" link
        AjaxLink<Date> today;
        add(today = new AjaxLink<Date>("today") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((FormComponent<Date>) YuiDateTimeField.this.get("date")).clearInput();
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

        today.add(new Image("current-date-img", new ResourceReference(YuiDateTimeField.class,
                "resources/set-now-16.png")));

        //Add change behavior to super fields
        for (String name : new String[] { "date", "hours", "minutes", "amOrPmChoice" }) {
            get(name).add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    YuiDateTimeField.this.onUpdate(target);
                }
            });
        }

        add(CSSPackageResource.getHeaderContribution(YuiDateTimeField.class, "yuidatetime.css"));
    }

    // callback that the ChangeBehaviour calls when one of the composing fields updates
    public void onUpdate(AjaxRequestTarget target) {
        updateDateTime(getDate(), getHours(), getMinutes());

        if (target != null) {
            target.addComponent(this);
        }
    }

    private void updateDateTime(Date date, Integer hours, Integer minutes) {
        if(date!=null) {
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
    }

    @Override
    protected boolean use12HourFormat() {
        return false;
    }

    public void setTodayLinkVisible(boolean todayLinkVisible) {
        this.todayLinkVisible = todayLinkVisible;
    }

    protected String getDatePattern() {
        return settings.getDatePattern();
    }

    @Override
    protected DatePicker newDatePicker() {
        return new DatePicker() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getDatePattern() {
                return "This string is non-null to please the base class.  Please ignore it."
                        + "  The date-picker behavior is overridden later in the construction phase by the YuiDatePicker.";
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
        return new DateTextField(id, dateFieldModel, new DateConverter(false) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getDatePattern() {
                return YuiDateTimeField.this.getDatePattern();
            }

            @Override
            protected DateTimeFormatter getFormat() {
                return DateTimeFormat.forPattern(getDatePattern()).withLocale(getLocale());
            }
        });
    }
}
