package org.onehippo.cms7.channelmanager.channels;

import java.util.Arrays;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.ChannelIconPanel")
public class ChannelIconPanel extends ExtPanel {

    public static final String CHANNEL_ICON_PANEL_JS = "ChannelIconPanel.js";

    private ChannelStore store;

    @ExtProperty
    @SuppressWarnings("unused")
    private String composerRestMountUrl;

    public ChannelIconPanel(IPluginConfig channelListConfig, ExtStoreFuture storeFuture) {
        this.store = (ChannelStore) storeFuture.getStore();
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("store", new JSONIdentifier(this.store.getJsObjectId()));
    }
    
}
