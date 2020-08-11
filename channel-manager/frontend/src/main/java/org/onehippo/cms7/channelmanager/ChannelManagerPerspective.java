/**
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IRenderService;
import org.onehippo.cms7.channelmanager.channeleditor.ChannelEditorApiHeaderItem;
import org.onehippo.cms7.channelmanager.service.IChannelManagerService;

public class ChannelManagerPerspective extends Perspective implements IChannelManagerService {

    private static final CssResourceReference CHANNEL_MANAGER_PERSPECTIVE_CSS = new CssResourceReference(ChannelManagerPerspective.class, "ChannelManagerPerspective.css");
    private static final String EVENT_ID = "channels";
    public static final String EVENT_CMSCHANNELS_DEACTIVATED = "CMSChannels-deactivated";

    private final RootPanel rootPanel;
    private final List<IRenderService> childServices = new LinkedList<>();

    public ChannelManagerPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config, EVENT_ID);

        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig != null) {
            WireframeSettings wfSettings = new WireframeSettings(wfConfig);
            add(new WireframeBehavior(wfSettings));
        }

        rootPanel = new RootPanel(context, config, "channel-root", EVENT_ID);
        add(rootPanel);

        final String channelManagerServiceId = config.getString("channel.manager.service.id", IChannelManagerService.class.getName());
        context.registerService(this, channelManagerServiceId);
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("perspective-title", this, Model.of("Channel Manager"));
    }

    @Override
    protected void onDeactivated() {
        super.onDeactivated();
        publishEvent(EVENT_CMSCHANNELS_DEACTIVATED);
    }

    @Override
    public void render(final PluginRequestTarget target) {
        super.render(target);
        // a hard page refresh should always show the channel manager overview again
        if (target == null) {
            rootPanel.setActiveCard(RootPanel.CardId.CHANNEL_MANAGER);
        }

        if (isActive()) {
            rootPanel.render(target);
        }

        for (IRenderService child : childServices) {
            child.render(target);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CHANNEL_MANAGER_PERSPECTIVE_CSS));
        response.render(ChannelEditorApiHeaderItem.get());
    }

    public void removeRenderService(final IRenderService service) {
        childServices.remove(service);
    }

    public void addRenderService(final IRenderService service) {
        childServices.add(service);
    }

    @Override
    public void viewChannel(final String channelId, final String channelPath) {
        viewChannel(channelId, channelPath, "master");
    }

    @Override
    public void viewChannel(final String channelId, final String channelPath, final String branchId) {
        rootPanel.activateCard(RootPanel.CardId.CHANNEL_EDITOR);
        rootPanel.getChannelEditor().viewChannel(channelId, channelPath, branchId);
        focus(null);
    }
}
