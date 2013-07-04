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
package org.hippoecm.frontend.plugins.yui.ajax;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;

public class AjaxIndicatorBehavior extends AbstractYuiBehavior {

    private static final long serialVersionUID = 1L;

    private final static ResourceReference AJAX_LOADER_GIF = new PackageResourceReference(AjaxIndicatorBehavior.class,
            "ajax-loader.gif");

    final private AjaxIndicatorAppender ajaxIndicator;

    public AjaxIndicatorBehavior() {
        ajaxIndicator = new AjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getIndicatorUrl() {
                return RequestCycle.get().urlFor(AJAX_LOADER_GIF, null);
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addModule(HippoNamespace.NS, "ajaxindicator");

        Map<String, Object> parameters = new MiniMap(1);
        parameters.put("id", ajaxIndicator.getMarkupId());
        helper.addTemplate(AjaxIndicatorBehavior.class, "init_ajax_indicator.js", parameters);
    }

    @Override
    public void bind(Component component) {
        component.add(ajaxIndicator);
        super.bind(component);
    }

}
