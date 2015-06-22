/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.editor.editor;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;

/**
 * Prevent form submit from enter keypress at input fields. The component container used in the constructor
 * {@link PreventSubmitOnEnterBehavior#PreventSubmitOnEnterBehavior(String)} must set its markup-id with the call
 * {@link Component#setOutputMarkupId(boolean)} before creating this behavior.
 */
class PreventSubmitOnEnterBehavior extends Behavior {
    public static final String PREVENT_SUBMIT_JS_TEMPLATE = "$('${selector}').keypress(function (e) {if (e.which == 13) {e.preventDefault();}});";

    private String selector;

    /**
     * Construct the behavior to prevent form submit when pressing the enter key at input fields
     *
     * @param containerId the Id of the container's input fields
     */
    public PreventSubmitOnEnterBehavior(final String containerId) {
        this.selector = "#" + containerId + " input";
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        final Map<String, Object> params = new HashMap<>();
        params.put("selector", selector);

        final String script = MapVariableInterpolator.interpolate(PREVENT_SUBMIT_JS_TEMPLATE, params);
        response.render(OnDomReadyHeaderItem.forScript(script));
    }

}
