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

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channels.BlueprintStore;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelPropertiesPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.hstconfig.HstConfigEditor;
import org.onehippo.cms7.channelmanager.templatecomposer.PageEditor;
import org.onehippo.cms7.channelmanager.widgets.ExtLinkPicker;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.layout.BorderLayout;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.RootPanel")
public class RootPanel extends ExtPanel {

    public enum Card {
        CHANNEL_MANAGER(0),
        TEMPLATE_COMPOSER(1),
        HST_CONFIG_EDITOR(2);

        private Integer tabIndex;

        private Card(Integer tabIndex) {
            this.tabIndex = tabIndex;
        }

    }

    public static final String CONFIG_CHANNEL_LIST = "channel-list";

    private BlueprintStore blueprintStore;
    private ChannelStore channelStore;
    private PageEditor pageEditor;

    @ExtProperty
    private Integer activeItem = 0;

    @Override
    public void buildInstantiationJs(final StringBuilder js, final String extClass, final JSONObject properties) {
        js.append("try { ");
        super.buildInstantiationJs(js, extClass, properties);
        js.append("} catch(exception) { console.log('Error initializing channel manager. '+exception); } ");
    }

    public RootPanel(final IPluginContext context, final IPluginConfig config, String id) {
        super(id);

        add(new ChannelManagerResourceBehaviour());

        // card 0: channel manager
        final ExtPanel channelManagerCard = new ExtPanel();
        channelManagerCard.setLayout(new BorderLayout());

        final IPluginConfig channelListConfig = config.getPluginConfig(CONFIG_CHANNEL_LIST);
        final ChannelGridPanel channelPanel = new ChannelGridPanel(context, channelListConfig);
        channelPanel.setRegion(BorderLayout.Region.CENTER);
        channelManagerCard.add(channelPanel);

        //Use the same store variable as the grid panel, no need to create another store.
        this.channelStore = channelPanel.getStore();

        final HstConfigEditor hstConfigEditor = new HstConfigEditor(context);

        final ChannelPropertiesPanel channelPropertiesPanel = new ChannelPropertiesPanel(context, channelStore, hstConfigEditor);
        channelPropertiesPanel.setRegion(BorderLayout.Region.EAST);
        channelManagerCard.add(channelPropertiesPanel);

        this.blueprintStore = new BlueprintStore();
        channelManagerCard.add(this.blueprintStore);

        add(channelManagerCard);

        // card 1: template composer
        final IPluginConfig pageEditorConfig = config.getPluginConfig("templatecomposer");
        pageEditor = new PageEditor(context, pageEditorConfig);
        add(pageEditor);

        // card 2: HST config editor
        add(hstConfigEditor);

        // card 3: folder picker
        add(new ExtLinkPicker(context));
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

    public PageEditor getPageEditor() {
        return this.pageEditor;
    }

    public void setActiveCard(Card rootPanelCard) {
        this.activeItem = rootPanelCard.tabIndex;
    }

}
