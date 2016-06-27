/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

public class YuiDateField extends YuiDateTimeField {

    public YuiDateField(String id, IModel<Date> model) {
        this(id, model, null);
    }

    public YuiDateField(String id, IModel<Date> model, YuiDatePickerSettings settings) {
        super(id, model, settings);

        get(HOURS).setVisibilityAllowed(false);
        get(MINUTES).setVisibilityAllowed(false);
        get(AM_OR_PM_CHOICE).setVisibilityAllowed(false);
    }
}