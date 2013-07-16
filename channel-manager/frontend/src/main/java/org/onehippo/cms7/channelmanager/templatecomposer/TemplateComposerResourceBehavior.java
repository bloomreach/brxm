/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.extjs.ExtWidgetRegistry;
import org.onehippo.cms7.channelmanager.common.CommonBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.pageeditor.PageEditorBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.plugins.PluginsBundle;

public class TemplateComposerResourceBehavior extends Behavior {

    private static final long serialVersionUID = 1L;

    private static final JavaScriptResourceReference[] DEVELOPMENT_REFERENCES;
    private static final JavaScriptResourceReference[] DEPLOYMENT_REFERENCES;

    static {
        List<JavaScriptResourceReference> developmentRefs = new ArrayList<JavaScriptResourceReference>();
        developmentRefs.add(new JavaScriptResourceReference(PluginsBundle.class, PluginsBundle.FLOATING_WINDOW));
        developmentRefs.add(new JavaScriptResourceReference(PluginsBundle.class, PluginsBundle.COLOR_FIELD));
        developmentRefs.add(new JavaScriptResourceReference(PluginsBundle.class, PluginsBundle.VTABS));

        developmentRefs.add(new JavaScriptResourceReference(TemplateComposerGlobalBundle.class, TemplateComposerGlobalBundle.GLOBALS));
        developmentRefs.add(new JavaScriptResourceReference(CommonBundle.class, CommonBundle.MARK_REQUIRED_FIELDS));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.ICON_GRID_VIEW));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.TOOLKIT_GRID_PANEL));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.ICON_TOOLBAR_WINDOW));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.MANAGE_CHANGES_WINDOW));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.NOTIFICATION));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.REST_STORE));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.PROPERTIES_PANEL) {
            @Override
            public Iterable<? extends HeaderItem> getDependencies() {
                return ExtWidgetRegistry.getRegistryHeaderItems();
            }
        });
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.DRAG_DROP_ONE));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.MSG));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.TOOLKIT_STORE));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.MESSAGE_BUS));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.IFRAME_PANEL));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.PAGE_MODEL_STORE));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.PAGE_CONTEXT));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.PAGE_CONTAINER));
        developmentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.PAGE_EDITOR) {
            @Override
            public Iterable<? extends HeaderItem> getDependencies() {
                return ExtWidgetRegistry.getRegistryHeaderItems();
            }
        });
        DEVELOPMENT_REFERENCES = developmentRefs.toArray(new JavaScriptResourceReference[developmentRefs.size()]);

        List<JavaScriptResourceReference> deploymentRefs = new ArrayList<JavaScriptResourceReference>();
        deploymentRefs.add(new JavaScriptResourceReference(PluginsBundle.class, PluginsBundle.ALL));
        deploymentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.ALL) {
            @Override
            public Iterable<? extends HeaderItem> getDependencies() {
                return ExtWidgetRegistry.getRegistryHeaderItems();
            }
        });
        DEPLOYMENT_REFERENCES = deploymentRefs.toArray(new JavaScriptResourceReference[deploymentRefs.size()]);
    }
    
    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        JavaScriptResourceReference[] references;
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            references = DEVELOPMENT_REFERENCES;
        } else {
            references = DEPLOYMENT_REFERENCES;
        }
        for (JavaScriptResourceReference reference : references) {
            response.render(JavaScriptHeaderItem.forReference(reference));
        }
    }

}
