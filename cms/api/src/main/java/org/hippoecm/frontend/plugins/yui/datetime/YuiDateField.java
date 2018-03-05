/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.joda.time.DateTimeFieldType;
import org.joda.time.MutableDateTime;

public class YuiDateField extends YuiDateTimeField {

    private static final String CURRENT_DATE_LABEL = "set-to-current-date-only";
    private static final String CURRENT_DATE_TOOLTIP = "set-to-current-date-only-tooltip";

    public YuiDateField(final String id, final IModel<Date> model) {
        this(id, model, null);
    }

    public YuiDateField(final String id, final IModel<Date> model, final YuiDatePickerSettings settings) {
        super(id, model, settings);

        // hiding the "hours" component hides the entire "hours" wicket:enclosure
        get(HOURS).setVisibilityAllowed(false);
        // hide the minutes field to prevent wicket.ajax javascript errors
        get(MINUTES).setVisibilityAllowed(false);
    }

    @Override
    String getTodayLinkLabel() {
        return getString(CURRENT_DATE_LABEL);
    }

    @Override
    String getTodayLinkTooltip() {
        return getString(CURRENT_DATE_TOOLTIP);
    }

    /**
     * Set the hour and minutes to 00:00. This 'removes' the time factor from affecting sort order when handling
     * multiple documents with a date-only field.
     *
     * @param dateTime
     */
    @Override
    void setHourAndMinutes(final MutableDateTime dateTime) {
        dateTime.set(DateTimeFieldType.hourOfDay(), 0);
        dateTime.setMinuteOfHour(0);
    }
}
