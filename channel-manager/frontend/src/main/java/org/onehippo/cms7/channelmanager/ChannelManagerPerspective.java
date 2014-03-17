/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.perspectives.common.ErrorMessagePanel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.hst.rest.SiteService;
import org.onehippo.cms7.channelmanager.service.IChannelManagerService;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.onehippo.cms7.channelmanager.templatecomposer.TemplateComposerApiHeaderItem;

import static org.onehippo.cms7.channelmanager.ChannelManagerConsts.CONFIG_REST_PROXY_SERVICE_ID;

@SuppressWarnings("serial")
public class ChannelManagerPerspective extends Perspective implements IChannelManagerService {

    private static final CssResourceReference CHANNEL_MANAGER_PERSPECTIVE_CSS = new CssResourceReference(ChannelManagerPerspective.class, "ChannelManagerPerspective.css");

    private RootPanel rootPanel;
    private boolean siteIsUp;
    private List<IRenderService> childservices = new LinkedList<IRenderService>();

    public ChannelManagerPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        // Check whether the site is up and running
        final IRestProxyService restProxyService = context.getService(config.getString(CONFIG_REST_PROXY_SERVICE_ID, IRestProxyService.class.getName()), IRestProxyService.class);
        final SiteService siteService = restProxyService.createSecureRestProxy(SiteService.class);

        // When site service is null most probably the site is down
        siteIsUp = (siteService != null);

        if (siteIsUp) {
            IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
            if (wfConfig != null) {
                WireframeSettings wfSettings = new WireframeSettings(wfConfig);
                add(new WireframeBehavior(wfSettings));
            }

            rootPanel = new RootPanel(context, config, "channel-root");
            add(rootPanel);

            final String channelManagerServiceId = config.getString("channel.manager.service.id", IChannelManagerService.class.getName());
            context.registerService(this, channelManagerServiceId);
        } else {
            final Fragment dimmedRootPanelFragment= new Fragment("channel-root", "dimmed-root-fragment", this);
            dimmedRootPanelFragment.add(new ErrorMessagePanel("dimmed-root-panel-div", new ResourceModel("site.is.down.message")));
            add(dimmedRootPanelFragment);
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("perspective-title", this, new Model<String>("Channel Manager"));
    }

    @Override
    public ResourceReference getIcon(IconSize type) {
        if (siteIsUp) {
            return new PackageResourceReference(ChannelManagerPerspective.class, "channel-manager-" + type.getSize() + ".png");
        } else {
            return new PackageResourceReference(ChannelManagerPerspective.class, "channel-manager-dimmed-" + type.getSize() + ".png");
        }
    }

    @Override
    public void render(final PluginRequestTarget target) {
        super.render(target);
        if (siteIsUp) {
            if (isActive()) {
                rootPanel.render(target);
            }
            for (IRenderService child : childservices) {
                child.render(target);
            }
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CHANNEL_MANAGER_PERSPECTIVE_CSS));
        response.render(TemplateComposerApiHeaderItem.get());
    }

    public void removeRenderService(final IRenderService service) {
        childservices.remove(service);
    }

    public void addRenderService(final IRenderService service) {
        childservices.add(service);
    }

    @Override
    public void viewChannel(final String channelId, String pathInfo, String contextPath, String cmsPreviewPrefix, String templateComposerContextPath) {
        if (siteIsUp) {
            PageEditor pageEditor = rootPanel.getPageEditor();
            pageEditor.setChannel(channelId);
            pageEditor.setRenderPathInfo(pathInfo);
            pageEditor.setRenderContextPath(contextPath);
            pageEditor.setCmsPreviewPrefix(cmsPreviewPrefix);
            pageEditor.setTemplateComposerContextPath(templateComposerContextPath);
            pageEditor.setPreviewMode(true);
            rootPanel.setActiveCard(RootPanel.CardId.TEMPLATE_COMPOSER);
            focus(null);
        }
    }

}
