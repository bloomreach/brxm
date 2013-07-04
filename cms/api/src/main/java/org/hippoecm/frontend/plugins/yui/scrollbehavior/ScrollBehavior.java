/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;

public class ScrollBehavior extends Behavior {
    private static final long serialVersionUID = 1L;

    private static final ResourceReference SCRIPT = new JavaScriptResourceReference(ScrollBehavior.class, "scroll.js");
    private final PackageTextTemplate INIT = new PackageTextTemplate(ScrollBehavior.class, "init_scroll.js");

    final Map<String, Object> parameters = new HashMap<String, Object>();

    public ScrollBehavior() {
        super();
    }

    public void bind(Component component) {
        parameters.put("id", component.getMarkupId());
        parameters.put("filterName", WebApplication.get().getWicketFilter().getFilterConfig().getFilterName());
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(SCRIPT));
        response.render(OnDomReadyHeaderItem.forScript(INIT.interpolate(parameters).getString()));
    }

}
