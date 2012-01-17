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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channels.BlueprintStore;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.channels.ChannelStoreFactory;
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

    private static final long serialVersionUID = 1L;

    public enum CardId {
        CHANNEL_MANAGER(0),
        TEMPLATE_COMPOSER(1),
        HST_CONFIG_EDITOR(2);

        private final int tabIndex;

        private CardId(int tabIndex) {
            this.tabIndex = tabIndex;
        }

        int getTabIndex() {
            return tabIndex;
        }

    }

    public static final String CONFIG_CHANNEL_LIST = "channel-list";

    private BlueprintStore blueprintStore;
    private ChannelStore channelStore;
    private PageEditor pageEditor;
    private ExtStoreFuture<Object> channelStoreFuture;

    private boolean redraw = false;

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

        final IPluginConfig channelListConfig = config.getPluginConfig(CONFIG_CHANNEL_LIST);

        this.channelStore = ChannelStoreFactory.createStore(context, channelListConfig);
        this.channelStoreFuture = new ExtStoreFuture<Object>(channelStore);
        add(this.channelStore);
        add(this.channelStoreFuture);

        this.blueprintStore = new BlueprintStore();

        // card 0: channel manager
        final ExtPanel channelManagerCard = new ExtPanel();
        channelManagerCard.setTitle(new Model<String>("Channel Manager"));
        channelManagerCard.setHeader(false);
        channelManagerCard.setLayout(new BorderLayout());

        final ChannelGridPanel channelPanel = new ChannelGridPanel(channelListConfig, this.channelStoreFuture,
                !blueprintStore.isEmpty());
        channelPanel.setRegion(BorderLayout.Region.CENTER);
        channelManagerCard.add(channelPanel);

        final HstConfigEditor hstConfigEditor = new HstConfigEditor(context);

        channelManagerCard.add(this.blueprintStore);

        add(channelManagerCard);

        // card 1: template composer
        final IPluginConfig pageEditorConfig = config.getPluginConfig("templatecomposer");
        pageEditor = new PageEditor(context, pageEditorConfig, hstConfigEditor, this.channelStoreFuture);
        add(pageEditor);

        // card 2: HST config editor
        add(hstConfigEditor);

        // card 3: folder picker
        add(new ExtLinkPicker(context));
    }

    public void redraw() {
        redraw = true;
    }

    public void render(PluginRequestTarget target) {
        pageEditor.render(target);
        if (redraw) {
            JSONObject update = new JSONObject();
            try {
                update.put("activeItem", activeItem);
            } catch (JSONException e) {
                throw new RuntimeException("could not populate property updates", e);
            }
            target.appendJavascript("Ext.getCmp('rootPanel').update(" + update.toString() + ");");
            redraw = false;
        }
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        blueprintStore.onRenderExtHead(js);
        channelStore.onRenderExtHead(js);
        channelStoreFuture.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("blueprintStore", new JSONIdentifier(this.blueprintStore.getJsObjectId()));
        properties.put("channelStore", new JSONIdentifier(this.channelStore.getJsObjectId()));
        properties.put("channelStoreFuture", new JSONIdentifier(this.channelStoreFuture.getJsObjectId()));
        RequestCycle rc = RequestCycle.get();
        properties.put("breadcrumbIconUrl", rc.urlFor(new ResourceReference(RootPanel.class, "breadcrumb-arrow.png")));
    }

    public PageEditor getPageEditor() {
        return this.pageEditor;
    }

    public void setActiveCard(CardId rootPanelCard) {
        this.activeItem = rootPanelCard.getTabIndex();
        redraw();
    }

}
