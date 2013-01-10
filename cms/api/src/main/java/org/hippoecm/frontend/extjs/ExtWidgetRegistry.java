/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.extjs;

import java.util.List;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.wicketstuff.js.ext.ExtObservable;

/**
 * Central registry for the configuration of Ext widgets. The configuration of these widgets
 * can be retrieved in Javascript via the xtype of the widget:
 * <pre>
 * var config = Hippo.ExtWidgets.getConfig('somextype')
 * </pre>
 * @see ExtWidget
 */
public class ExtWidgetRegistry extends ExtObservable {

    public static final String EXT_WIDGET_SERVICE_ID = ExtWidgetRegistry.class.getName();

    private final IPluginContext context;
    private boolean loadedWidgets;

    public ExtWidgetRegistry(IPluginContext context) {
        this.context = context;
        add(JavascriptPackageResource.getHeaderContribution(ExtWidgetRegistry.class, "ExtWidgetRegistry.js"));
    }

    /**
     * Prevent that we render the head more than once, because it is explicitly rendered before other head contributions.
     * This way we force the initialization of the component registry before the initialization of child
     * components, so Ext widget configurations can always be accessed in constructors of normal Ext components.
     *
     * @param response
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        if (!loadedWidgets) {
            List<IHeaderContributor> contributors = context.getServices(EXT_WIDGET_SERVICE_ID, IHeaderContributor.class);
            for (IHeaderContributor contributor : contributors) {
                if (!(contributor instanceof ExtWidget)) {
                    add(contributor);
                }
            }
            List<ExtWidget> widgets = context.getServices(EXT_WIDGET_SERVICE_ID, ExtWidget.class);
            for (ExtWidget widget : widgets) {
                add(widget);
            }
            loadedWidgets = true;
        }
        super.renderHead(response);
    }

}
