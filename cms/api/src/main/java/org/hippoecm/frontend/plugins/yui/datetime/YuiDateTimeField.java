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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.editor.resources.CmsEditorHeaderItem;
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
    private static final CssResourceReference YUIDATETIME_STYLESHEET = new CssResourceReference(YuiDateTimeField.class, "yuidatetime.css") {
        @Override
        public Iterable<? extends HeaderItem> getDependencies() {
            return Collections.singleton(CmsEditorHeaderItem.get());
        }
    };

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
        for (Behavior b : dateField.getBehaviors()) {
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
                YuiDateTimeField.this.setDefaultModelObject(new Date());
                ((FormComponent<Date>) YuiDateTimeField.this.get("date")).clearInput();
                ((FormComponent<Date>) YuiDateTimeField.this.get("hours")).clearInput();
                ((FormComponent<Date>) YuiDateTimeField.this.get("minutes")).clearInput();
                if (target != null) {
                    target.add(YuiDateTimeField.this);
                }
            }

            @Override
            public boolean isVisible() {
                return todayLinkVisible;
            }
        });

        today.add(new Image("current-date-img", new PackageResourceReference(YuiDateTimeField.class,
                "resources/set-now-16.png")));

        //Add change behavior to super fields
        for (String name : new String[] { "date", "hours", "minutes", "amOrPmChoice" }) {
            get(name).add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    updateDateTime();
                }
            });
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(YUIDATETIME_STYLESHEET));
    }

    private void updateDateTime() {
        Date date = getDate();
        if (date != null) {
            MutableDateTime datetime = new MutableDateTime(date);
            try {
                TimeZone zone = getClientTimeZone();
                if (zone != null) {
                    datetime.setZone(DateTimeZone.forTimeZone(zone));
                }

                Integer hours = getHours();
                if (hours != null) {
                    datetime.set(DateTimeFieldType.hourOfDay(), hours % 24);

                    Integer minutes = getMinutes();
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
            public String getDatePattern(Locale locale) {
                return YuiDateTimeField.this.getDatePattern();
            }

            @Override
            protected DateTimeFormatter getFormat(Locale locale) {
                return DateTimeFormat.forPattern(YuiDateTimeField.this.getDatePattern()).withLocale(getLocale());
            }
        });
    }
}
