/**
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
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
package org.onehippo.cms7.channelmanager;


import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.IconSize;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.onehippo.cms7.channelmanager.templatecomposer.pageeditor.PageEditorBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.plugins.PluginsBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateComposerPerspective extends Perspective {
    private static final Logger log = LoggerFactory.getLogger(ChannelManagerPerspective.class);

    public TemplateComposerPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IPluginConfig pageEditorConfig = config.getPluginConfig("templatecomposer");
        add(new PageEditor("page-editor", pageEditorConfig));

        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig != null) {
            WireframeSettings wfSettings = new WireframeSettings(wfConfig);
            add(new WireframeBehavior(wfSettings));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        IRenderService renderService = this;
        while (renderService.getParentService() != null) {
            renderService = renderService.getParentService();
        }
        Page page = renderService.getComponent().getPage();
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.MI_FRAME));
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.MI_FRAME_MSG));
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.FLOATING_WINDOW));
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.BASE_GRID));
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.COLOR_FIELD));
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.JSONP));

            page.add(JavascriptPackageResource.getHeaderContribution(PageEditor.class, "globals.js"));
            page.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PROPERTIES_PANEL));
            page.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PAGE_MODEL));
            page.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.PAGE_EDITOR));
        } else {
            page.add(JavascriptPackageResource.getHeaderContribution(PluginsBundle.class, PluginsBundle.ALL));
            page.add(JavascriptPackageResource.getHeaderContribution(PageEditorBundle.class, PageEditorBundle.ALL));
        }
    }


    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("perspective-title", this, new Model<String>("Template Composer"));
    }


    @Override
    public ResourceReference getIcon(IconSize type) {
        return new ResourceReference(ChannelManagerPerspective.class, "channel-manager-" + type.getSize() + ".png");

    }
}
