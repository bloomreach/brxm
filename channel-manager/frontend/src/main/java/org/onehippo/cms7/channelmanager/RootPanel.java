/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.channelmanager;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channels.BlueprintStore;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelPropertiesPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.wicketstuff.js.ext.ExtButton;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.layout.BorderLayout;
import org.wicketstuff.js.ext.layout.FitLayout;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.RootPanel")
public class RootPanel extends ExtPanel {

    public static final String CONFIG_CHANNEL_LIST = "channel-list";

    private BlueprintStore blueprintStore;
    private ChannelStore channelStore;

    @Override
    public void buildInstantiationJs(final StringBuilder js, final String extClass, final JSONObject properties) {
        js.append("try { ");
        super.buildInstantiationJs(js, extClass, properties);    //To change body of overridden methods use File | Settings | File Templates.
        js.append("} catch(exception) { console.log('Error initializing channel manager. '+exception); } ");
    }

    public RootPanel(final IPluginContext context, final IPluginConfig config, String id) {
        super(id);

        add(new ChannelManagerResourceBehaviour());

        ExtPanel channelManagerCard = new ExtPanel();
        channelManagerCard.setLayout(new BorderLayout());

        final IPluginConfig channelListConfig = config.getPluginConfig(CONFIG_CHANNEL_LIST);
        final ChannelGridPanel channelPanel = new ChannelGridPanel(channelListConfig);
        channelPanel.setRegion(BorderLayout.Region.CENTER);
        channelManagerCard.add(channelPanel);

        //Use the same store variable as the grid panel, no need to create another store.
        this.channelStore = channelPanel.getStore();

        final ChannelPropertiesPanel channelPropertiesPanel = new ChannelPropertiesPanel(context);
        channelPropertiesPanel.setRegion(BorderLayout.Region.EAST);
        channelManagerCard.add(channelPropertiesPanel);

        final List<ExtField> blueprintFieldList = new ArrayList<ExtField>();
        blueprintFieldList.add(new ExtField("name"));
        blueprintFieldList.add(new ExtField("description"));
        this.blueprintStore = new BlueprintStore(blueprintFieldList);
        channelManagerCard.add(this.blueprintStore);

        add(channelManagerCard);

        IPluginConfig pageEditorConfig = config.getPluginConfig("templatecomposer");
        PageEditor pageEditor = new PageEditor(pageEditorConfig);
        add(pageEditor);

    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        blueprintStore.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("blueprintStore", new JSONIdentifier(this.blueprintStore.getJsObjectId()));
        properties.put("channelStore", new JSONIdentifier(this.channelStore.getJsObjectId()));
    }

}
