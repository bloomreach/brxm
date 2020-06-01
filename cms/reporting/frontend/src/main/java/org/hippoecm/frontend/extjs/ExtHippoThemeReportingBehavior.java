/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.extjs;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.resource.CssResourceReference;
import org.wicketstuff.js.ext.util.ExtCustomThemeBehavior;

/**
 * Adds the Hippo ExtJs theme CSS to the header.
 */
public class ExtHippoThemeReportingBehavior extends ExtCustomThemeBehavior {

    private static final long serialVersionUID = 1L;

    private static CssResourceReference REPORTING_CSS = new CssResourceReference(ExtHippoThemeReportingBehavior.class, "hippotheme/ExtHippoThemeReporting.css");

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        response.render(CssHeaderItem.forReference(REPORTING_CSS));
    }

}
