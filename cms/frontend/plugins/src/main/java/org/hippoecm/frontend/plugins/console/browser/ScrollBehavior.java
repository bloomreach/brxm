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
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class ScrollBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private static final ResourceReference SCRIPT = new JavascriptResourceReference(ScrollBehavior.class, "scroll.js");
    private final PackagedTextTemplate INIT = new PackagedTextTemplate(ScrollBehavior.class, "init_scroll.js");
    private final ParameterModel model = new ParameterModel();

    public ScrollBehavior() {
        super();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderJavascriptReference(SCRIPT);
        TextTemplateHeaderContributor.forJavaScript(INIT, model).renderHead(response);
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        // NOP
    }

    class ParameterModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;

        final Map<String, Object> parameters = new HashMap<String, Object>();

        @Override
        public Object getObject() {
            parameters.put("id", getComponent().getMarkupId());
            return parameters;
        }

    }
}
