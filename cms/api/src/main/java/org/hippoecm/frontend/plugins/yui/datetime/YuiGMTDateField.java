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
package org.hippoecm.frontend.plugins.yui.datetime;

import java.util.Date;
import java.util.TimeZone;

import org.apache.wicket.model.IModel;

/**
 * The component to represent only the date section of the {@link Date} object value in GMT timezone.
 *
 * @deprecated no longer used because it uses a hardcoded GMT timezone, while user session time zone is to be used.
 *              Use YuiDateTimeField instead.
 */
@Deprecated
public class YuiGMTDateField extends YuiDateTimeField {

    public YuiGMTDateField(String id, IModel<Date> model, YuiDatePickerSettings settings) {
        super(id, model, settings);

        // hiding the "hours" component hides the entire "hours" wicket:enclosure
        get(HOURS).setVisibilityAllowed(false);
        // hide the minutes field to prevent wicket.ajax javascript errors
        get(MINUTES).setVisibilityAllowed(false);
    }

    @Override
    protected TimeZone getClientTimeZone() {
        return TimeZone.getTimeZone("GMT");
    }
}
