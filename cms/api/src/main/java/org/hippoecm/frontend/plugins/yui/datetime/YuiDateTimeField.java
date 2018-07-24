/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Locale;
import java.util.TimeZone;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.UpdateFeedbackInfo;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Semi-fork of YUI DateTimeField from Wicket extensions. Replaces Wicket extensions YUI behaviors with a {@link YuiDatePicker}
 * so it fit's in the Hippo ECM YUI framework.
 * <p>
 * DatePicker can be configured using a frontend:pluginconfig node with name <code>datepicker</code>.
 *
 * @see YuiDatePickerSettings for all configuration options
 */
public class YuiDateTimeField extends DateTimeField {

    private static final String CURRENT_DATE_TIME_TOOLTIP = "set-to-current-date-tooltip";
    private static final String CURRENT_DATE_TIME_LABEL = "set-to-current-date";
    public static final String DATE_LABEL = "date-label";
    public static final String HOURS_LABEL = "hours-label";
    public static final String MINUTES_LABEL = "minutes-label";

    private final YuiDatePickerSettings settings;
    private boolean currentDateLinkVisible = true;

    public YuiDateTimeField(final String id, final IModel<Date> model) {
        this(id, model, null);
    }

    public YuiDateTimeField(final String id, final IModel<Date> model, final YuiDatePickerSettings settings) {
        super(id, model);

        if (settings != null) {
            this.settings = settings;
        }
        else {
            this.settings = new YuiDatePickerSettings();
            this.settings.setLanguage(getLocale().getLanguage());
        }

        setOutputMarkupId(true);

        final DateTextField dateField = (DateTextField) get("date");
        dateField.setLabel(Model.of(getString(DATE_LABEL)));

        // Remove existing behaviors from dateField
        dateField.getBehaviors().forEach(dateField::remove);
        // And add our own YuiDatePicker instead
        dateField.add(new YuiDatePicker(this.settings));

        // Restrict the size of the input field to match the date pattern
        final int dateLength = calculateDateLength();

        dateField.add(AttributeModifier.replace("size", dateLength));
        dateField.add(AttributeModifier.replace("maxlength", dateLength));

        final TextField hoursField = (TextField) get("hours");
        hoursField.setLabel(Model.of(getString(HOURS_LABEL)));

        final TextField minutesField = (TextField) get("minutes");
        minutesField.setLabel(Model.of(getString(MINUTES_LABEL)));

        //add "current-date" link
        final AjaxLink<Date> currentDateLink;
        add(currentDateLink = new AjaxLink<Date>("current-date") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                YuiDateTimeField.this.setDefaultModelObject(new Date());
                dateField.clearInput();
                hoursField.clearInput();
                minutesField.clearInput();
                if (target != null) {
                    target.add(YuiDateTimeField.this);
                }
            }

            @Override
            public boolean isVisible() {
                return currentDateLinkVisible;
            }
        });

        currentDateLink.add(HippoIcon.fromSprite("current-date-icon", Icon.RESTORE));
        currentDateLink.add(new Label("current-date-label", getTodayLinkLabel()));
        currentDateLink.add(TitleAttribute.set(getTodayLinkTooltip()));

        //Add change behavior to super fields
        for (final String name : new String[]{"date", "hours", "minutes", "amOrPmChoice"}) {
            get(name).add(new AjaxFormComponentUpdatingBehavior("change") {

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    updateDateTime();
                    send(YuiDateTimeField.this, Broadcast.BUBBLE, new UpdateFeedbackInfo(target));
                }

                @Override
                protected void onError(final AjaxRequestTarget target, final RuntimeException e) {
                    super.onError(target, e);
                    send(YuiDateTimeField.this, Broadcast.BUBBLE, new UpdateFeedbackInfo(target));
                }
            });
        }
    }

    String getTodayLinkLabel() {
        return getString(CURRENT_DATE_TIME_LABEL);
    }

    String getTodayLinkTooltip() {
        return getString(CURRENT_DATE_TIME_TOOLTIP);
    }

    private int calculateDateLength() {
        return settings.getDatePattern().length() + 2;
    }

    private void updateDateTime() {
        final Date date = getDate();
        if (date != null) {
            final MutableDateTime dateTime = new MutableDateTime(date);
            try {
                final TimeZone zone = getClientTimeZone();
                if (zone != null) {
                    dateTime.setZone(DateTimeZone.forTimeZone(zone));
                }

                setHourAndMinutes(dateTime);

                // the date will be in the server's timezone
                setDate(dateTime.toDate());
                setModelObject(dateTime.toDate());
            } catch (final RuntimeException e) {
                error(e.getMessage());
                invalid();
            }
        }
    }

    void setHourAndMinutes(final MutableDateTime dateTime) {
        final Integer hours = getHours();
        if (hours != null) {
            dateTime.set(DateTimeFieldType.hourOfDay(), hours % 24);
            final Integer minutes = getMinutes();
            dateTime.setMinuteOfHour(minutes != null ? minutes : 0);
        }
    }

    @Override
    protected boolean use12HourFormat() {
        return false;
    }

    /**
     * @deprecated As of 5.2.0 replaced by {@link #setCurrentDateLinkVisible}
     */
    @Deprecated
    public void setTodayLinkVisible(final boolean todayLinkVisible) {
        setCurrentDateLinkVisible(todayLinkVisible);
    }

    public void setCurrentDateLinkVisible(final boolean currentDateLinkVisible) {
        this.currentDateLinkVisible = currentDateLinkVisible;
    }

    protected String getDatePattern() {
        return settings.getDatePattern();
    }

    @Override
    protected DatePicker newDatePicker() {
        return new DatePicker() {
            @Override
            protected String getDatePattern() {
                return "This string is non-null to please the base class.  Please ignore it."
                        + "  The date-picker behavior is overridden later in the construction phase by the YuiDatePicker.";
            }
        };
    }

    @Override
    protected DateTextField newDateTextField(final String id, final PropertyModel<Date> dateFieldModel) {
        return new DateTextField(id, dateFieldModel, new DateConverter(false) {
            @Override
            public String getDatePattern(final Locale locale) {
                return YuiDateTimeField.this.getDatePattern();
            }

            @Override
            protected DateTimeFormatter getFormat(final Locale locale) {
                return DateTimeFormat.forPattern(YuiDateTimeField.this.getDatePattern()).withLocale(getLocale());
            }
        });
    }
}
