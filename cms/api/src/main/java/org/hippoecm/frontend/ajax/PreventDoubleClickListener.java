/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.ajax;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class PreventDoubleClickListener extends AjaxCallListener {

    static final JavaScriptResourceReference AJAX_UTILS_JS = new JavaScriptResourceReference(NoDoubleClickAjaxLink.class,
            "ajax-utils.js");

    private static final String disableJavascriptReplaceTag = "Hippo.Ajax.disable('%s');";
    private static final String enableJavascriptReplaceTag = "Hippo.Ajax.enable('%s');";

    @Override
    public CharSequence getBeforeHandler(final Component component) {
        return getDisableJavascript(component);
    }

    @Override
    public CharSequence getCompleteHandler(final Component component) {
        return getEnableJavascript(component);
    }

    @Override
    public CharSequence getFailureHandler(final Component component) {
        return getEnableJavascript(component);
    }

    public static String getDisableJavascript(final Component component) {
        return String.format(disableJavascriptReplaceTag, component.getMarkupId());
    }

    public static String getEnableJavascript(final Component component) {
        return String.format(enableJavascriptReplaceTag, component.getMarkupId());
    }
}
