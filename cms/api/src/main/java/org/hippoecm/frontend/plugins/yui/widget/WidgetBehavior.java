/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.widget;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;

import net.sf.json.JsonConfig;

public class WidgetBehavior extends AbstractYuiBehavior {
    private static final long serialVersionUID = 1L;


    Component component;
    WidgetTemplate template;

    public WidgetBehavior() {
        this(new WidgetSettings());
    }

    public WidgetBehavior(WidgetSettings settings) {
        template = new WidgetTemplate() {

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            protected void decorateJsonConfig(JsonConfig jsonConfig) {
                jsonConfig.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
            }
        };
        template.setConfiguration(settings);
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        this.component = component;
    }


    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "hippowidget");
        context.addTemplate(template);
    }

    @Override
    protected void onRenderHead(final IHeaderResponse response) {
        super.onRenderHead(response);
        response.render(OnDomReadyHeaderItem.forScript("YAHOO.hippo.WidgetManager.render();"));
    }

    public String getMarkupId() {
        return component.getMarkupId();
    }

    protected WidgetTemplate getTemplate() {
        return template;
    }

    public String getUpdateScript() {
        return "YAHOO.hippo.WidgetManager.update('" + getMarkupId() + "');";
    }

}
