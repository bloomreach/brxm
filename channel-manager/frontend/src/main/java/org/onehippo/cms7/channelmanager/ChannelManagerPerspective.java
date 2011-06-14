/**
 * Copyright 2010 Hippo
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

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.BreadCrumbBar;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IconSize;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelsListPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChannelManagerPerspective
 *
 * @author Vijay Kiran
 */
public class ChannelManagerPerspective extends Perspective {
    private static final Logger log = LoggerFactory.getLogger(ChannelManagerPerspective.class);
    private static final String CHANNEL_MANAGER_PANEL_SERVICE_ID = "channelmanager.panel";

    public ChannelManagerPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        ChannelGridPanel channelGridPanel = new ChannelGridPanel("channel-list", config);
        add(channelGridPanel);

        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig != null) {
            WireframeSettings wfSettings = new WireframeSettings(wfConfig);
            add(new WireframeBehavior(wfSettings));
        }
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("perspective-title", this, new Model<String>("Channel Manager"));
    }


    @Override
    public ResourceReference getIcon(IconSize type) {
        return new ResourceReference(ChannelManagerPerspective.class, "channel-manager-" + type.getSize() + ".png");

    }
}
