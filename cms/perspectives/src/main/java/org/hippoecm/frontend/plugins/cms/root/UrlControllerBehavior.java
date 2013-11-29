/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.root;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONObject;
import org.apache.wicket.ajax.json.JSONTokener;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.StringValue;

public class UrlControllerBehavior extends AbstractDefaultAjaxBehavior {

    private static final JavaScriptResourceReference URLCONTROLLER_JS =
            new JavaScriptResourceReference(PathInUrlController.class, "UrlHistory.js");

    private static final String URL_PARAMETERS = "parameters";

    @Override
    public void renderHead(final Component component, final IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(URLCONTROLLER_JS));

        String attributesAsJson = renderAjaxAttributes(component).toString();
        response.render(OnLoadHeaderItem.forScript(
                "Hippo.UrlHistory.init(function(params) {\n"
                        + "    var call = new Wicket.Ajax.Call(),"
                        + "        attributes = jQuery.extend({}, " + attributesAsJson + ");\n"
                        + "    call.ajax(attributes);\n"
                        + "});"));
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        final List<CharSequence> dep = attributes.getDynamicExtraParameters();
        dep.add("return { " + URL_PARAMETERS + ": params };");
    }

    protected void setParameter(String name, String value) {
        final AjaxRequestTarget requestTarget = RequestCycle.get().find(AjaxRequestTarget.class);
        if (requestTarget != null) {
            final String javascript = String.format("Hippo.UrlHistory.setParameter('%s', '%s');", name, value);
            requestTarget.appendJavaScript(javascript);
        }
    }

    @Override
    protected final void respond(final AjaxRequestTarget target) {
        RequestCycle rc = RequestCycle.get();
        IRequestParameters requestParameters = rc.getRequest().getRequestParameters();
        process(requestParameters);
    }

    public void process(IRequestParameters requestParameters) {
        final StringValue paramsValue = requestParameters.getParameterValue(URL_PARAMETERS);

        Map<String, String> parameters = new HashMap<>();
        if (!paramsValue.isEmpty()) {
            final String value = paramsValue.toString();
            try {
                final JSONObject jsonObject = new JSONObject(new JSONTokener(value));
                final Iterator keys = jsonObject.keys();
                while (keys.hasNext()) {
                    final String next = (String) keys.next();
                    parameters.put(next, jsonObject.getString(next));
                }
            } catch (JSONException e) {
                throw new RuntimeException("Unable to parse parameters from '" + value + "'", e);
            }
        }
        onRequest(parameters);
    }

    protected void onRequest(Map<String, String> parameters) {
    }

}
