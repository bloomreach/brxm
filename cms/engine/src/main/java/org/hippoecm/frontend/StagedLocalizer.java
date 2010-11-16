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
package org.hippoecm.frontend;

import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.IModel;

/**
 * A Localizer implementation that interprets keys as criteria of descending
 * importance.  Different criteria are separated by a "," (comma).  Criteria
 * are dropped until a match is found.
 */
public class StagedLocalizer extends Localizer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    public StagedLocalizer() {
    }

    @Override
    public String getStringIgnoreSettings(String key, final Component component, final IModel<?> model,
            final String defaultValue) {
        while (key.contains(",")) {
            String value = super.getStringIgnoreSettings(key, component, model, null);
            if (value != null) {
                return value;
            }
            key = key.substring(0, key.lastIndexOf(','));
        }
        return super.getStringIgnoreSettings(key, component, model, defaultValue);
    }
}
