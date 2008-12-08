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
package org.hippoecm.frontend.plugins.console.browser;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class ScrollBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private static PackagedTextTemplate INIT_SCRIPT = new PackagedTextTemplate(ScrollBehavior.class, "init_scroll.js");
    private static ResourceReference SCRIPT = new JavascriptResourceReference(ScrollBehavior.class, "scroll.js");

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", getComponent().getMarkupId());

        IModel parametersModel = new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;
            @Override
            public Object getObject() {
                return parameters;
            }
        };

        TextTemplateHeaderContributor.forJavaScript(INIT_SCRIPT, parametersModel).renderHead(response);
        response.renderJavascriptReference(SCRIPT);
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        // NOP
    }
}
