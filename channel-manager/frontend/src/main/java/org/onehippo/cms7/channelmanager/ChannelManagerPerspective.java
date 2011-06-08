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
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.service.IconSize;
import org.onehippo.cms7.channelmanager.channels.Channel;
import org.onehippo.cms7.channelmanager.channels.ChannelDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * ChannelManagerPerspective
 *
 * @author Vijay Kiran
 */
public class ChannelManagerPerspective extends Perspective {
    private static final Logger log = LoggerFactory.getLogger(ChannelManagerPerspective.class);
    private static final String CHANNEL_MANAGER_PANEL_SERVICE_ID = "channelmanager.panel";
    private static final String HOST_GROUP_CONFIG_PROP = "hst.virtualhostgroup.path";


    public ChannelManagerPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        Label errorLabel = new Label("error-msg", new Model<String>("No host group is configured for retrieving list of channels, please set the property " +
                    HOST_GROUP_CONFIG_PROP + " on the channel-manager configuration!"));

        final String hstConfigLocation = config.getString(HOST_GROUP_CONFIG_PROP);
        if (hstConfigLocation == null) {
            log.error("No host group is configured for retrieving list of channels, please set the property " +
                    HOST_GROUP_CONFIG_PROP + " on the channel-manager configuration!");
            
            errorLabel.setVisible(true);
            add(new EmptyPanel("channels-data-table"));
        } else {
            JcrNodeModel hstConfigNodeModel = new JcrNodeModel(hstConfigLocation);
            ChannelDataProvider channelDataProvider = new ChannelDataProvider(hstConfigNodeModel);
            DataTable<Channel> channelDataTable = new DataTable<Channel>("channels-data-table", getTableColumns(), channelDataProvider, 20);
            add(channelDataTable);
            errorLabel.setVisible(false);
        }

        add(errorLabel);
        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig != null) {
            WireframeSettings wfSettings = new WireframeSettings(wfConfig);
            add(new WireframeBehavior(wfSettings));
        }
    }

    @SuppressWarnings({"unchecked"})
    private IColumn<Channel>[] getTableColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("title", "Channel Name"), "title"));
        columns.add(new PropertyColumn(new ResourceModel("content-root", "Content Root"), "contentRoot"));
        columns.add(new PropertyColumn(new ResourceModel("hst-config-path", "HST Configuration"), "hstConfigPath"));
        return columns.toArray(new IColumn[columns.size()]);
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
