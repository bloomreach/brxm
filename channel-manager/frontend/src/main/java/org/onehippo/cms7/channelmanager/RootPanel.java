/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.layout.IWireframe;
import org.hippoecm.frontend.plugins.yui.layout.WireframeUtils;
import org.hippoecm.frontend.service.IRestProxyService;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channeleditor.ChannelEditor;
import org.onehippo.cms7.channelmanager.channels.BlueprintStore;
import org.onehippo.cms7.channelmanager.channels.ChannelOverview;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.channels.ChannelStoreFactory;
import org.onehippo.cms7.channelmanager.restproxy.RestProxyServicesManager;
import org.onehippo.cms7.channelmanager.widgets.ExtLinkPicker;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.layout.BorderLayout;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.RootPanel")
public class RootPanel extends ExtPanel {

    private static final long serialVersionUID = 1L;

    private static final PackageResourceReference BREADCRUMB_ARROW = new PackageResourceReference(RootPanel.class, "breadcrumb-arrow.png");

    public enum CardId {
        CHANNEL_MANAGER(0),
        CHANNEL_EDITOR(1);

        private final int tabIndex;

        CardId(int tabIndex) {
            this.tabIndex = tabIndex;
        }

        int getTabIndex() {
            return tabIndex;
        }

    }

    public static final String CONFIG_CHANNEL_LIST = "channel-list";
    public static final String CONFIG_TEMPLATE_COMPOSER = "templatecomposer";
    public static final String COMPOSER_REST_MOUNT_PATH_PROPERTY = "composerRestMountPath";
    public static final String DEFAULT_COMPOSER_REST_MOUNT_PATH = "/_rp";

    private BlueprintStore blueprintStore;
    private ChannelStore channelStore;
    private ChannelEditor channelEditor;
    private ExtStoreFuture<Object> channelStoreFuture;

    private boolean redraw = false;

    @ExtProperty
    private final String perspectiveId;

    @ExtProperty
    private int activeItem = 0;

    @ExtProperty
    @SuppressWarnings("unused")
    private String composerRestMountPath;

    @ExtProperty
    private final String[] contextPaths;

    @ExtProperty
    private boolean showBreadcrumbInitially = false;

    @Override
    public void buildInstantiationJs(final StringBuilder js, final String extClass, final JSONObject properties) {
        js.append("try { ");
        super.buildInstantiationJs(js, extClass, properties);
        js.append("} catch(exception) { console.log('Error initializing channel manager. '+exception); } ");
    }

    public RootPanel(final IPluginContext context, final IPluginConfig config, final String id, final String perspectiveId) {
        super(id);

        this.perspectiveId = perspectiveId;

        final IPluginConfig channelListConfig = config.getPluginConfig(CONFIG_CHANNEL_LIST);

        // card 1: template composer
        final IPluginConfig editorConfig = config.getPluginConfig(CONFIG_TEMPLATE_COMPOSER);
        if (editorConfig == null) {
            composerRestMountPath = DEFAULT_COMPOSER_REST_MOUNT_PATH;
        } else {
            composerRestMountPath = editorConfig.getString(COMPOSER_REST_MOUNT_PATH_PROPERTY, DEFAULT_COMPOSER_REST_MOUNT_PATH);
        }

        final Map<String, IRestProxyService> liveRestProxyServices = RestProxyServicesManager.getLiveRestProxyServices(context, config);
        contextPaths = liveRestProxyServices.keySet().toArray(new String[liveRestProxyServices.size()]);

        this.blueprintStore = new BlueprintStore(liveRestProxyServices);
        this.channelStore = ChannelStoreFactory.createStore(context, channelListConfig, liveRestProxyServices, blueprintStore);
        this.channelStoreFuture = new ExtStoreFuture<>(channelStore);
        add(this.channelStore);
        add(this.channelStoreFuture);


        // channel manager
        final ExtPanel channelManagerCard = new ExtPanel();
        channelManagerCard.setBorder(false);
        channelManagerCard.setTitle(Model.of(getString("channel-manager")));
        channelManagerCard.setHeader(false);
        channelManagerCard.setLayout(new BorderLayout());

        final ChannelOverview channelOverview = new ChannelOverview(channelListConfig, composerRestMountPath,
                this.channelStoreFuture, !blueprintStore.isEmpty());
        channelOverview.setRegion(BorderLayout.Region.CENTER);
        channelManagerCard.add(channelOverview);

        channelManagerCard.add(this.blueprintStore);
        add(channelManagerCard);

        // channel editor
        channelEditor = new ChannelEditor(context, editorConfig, composerRestMountPath, channelStoreFuture, contextPaths);
        add(channelEditor);

        // folder picker
        add(new ExtLinkPicker(context));
    }

    public void redraw() {
        redraw = true;
    }

    public void render(final PluginRequestTarget target) {
        channelStore.update();
        if (target != null) {
            if (redraw) {
                selectActiveItem(target);
                redraw = false;
            }
        } else {
            this.showBreadcrumbInitially = true;
        }
    }

    private void selectActiveItem(final PluginRequestTarget target) {
        final String script = String.format("Ext.getCmp('rootPanel').selectCard(%s);", activeItem);
        target.appendJavaScript(script);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        final IWireframe parentWireframe = WireframeUtils.getParentWireframe(this);
        if (parentWireframe != null) {
            response.render(parentWireframe.getHeaderItem());
        }

        super.renderHead(response);

        response.render(ChannelManagerHeaderItem.get());
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
        properties.put("breadcrumbIconUrl", rc.urlFor(new ResourceReferenceRequestHandler(
                BREADCRUMB_ARROW)));
    }

    public ChannelEditor getChannelEditor() {
        return this.channelEditor;
    }

    public void setActiveCard(CardId rootPanelCard) {
        this.activeItem = rootPanelCard.getTabIndex();
        redraw();
    }

}
