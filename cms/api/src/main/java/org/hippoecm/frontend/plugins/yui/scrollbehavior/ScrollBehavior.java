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
package org.hippoecm.frontend.plugins.yui.scrollbehavior;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class ScrollBehavior extends AbstractBehavior {
    private static final long serialVersionUID = 1L;

    private static final ResourceReference SCRIPT = new JavascriptResourceReference(ScrollBehavior.class, "scroll.js");
    private final PackagedTextTemplate INIT = new PackagedTextTemplate(ScrollBehavior.class, "init_scroll.js");
    private final ParameterModel model = new ParameterModel();
    private String componentMarkupId;

    public ScrollBehavior() {
        super();
    }

    public void bind(Component component) {
        this.componentMarkupId = component.getMarkupId();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderJavascriptReference(SCRIPT);
        TextTemplateHeaderContributor.forJavaScript(INIT, model).renderHead(response);
    }

    class ParameterModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;

        final Map<String, Object> parameters = new HashMap<String, Object>();

        @Override
        public Object getObject() {
            parameters.put("id", componentMarkupId);
            parameters.put("filterName", WebApplication.get().getWicketFilter().getFilterConfig().getFilterName());
            return parameters;
        }
    }
}
