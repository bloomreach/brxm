package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Ext Grid Panel for Channels Listing.
 */
@ExtClass("Hippo.ChannelManager.ChannelGridPanel")
public class ChannelGridPanel extends ExtPanel {

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private static final String HOST_GROUP_CONFIG_PROP = "hst.virtualhostgroup.path";
    private ChannelStore store;

    public ChannelGridPanel(String id, IPluginConfig config) {
        super(id);
        add(JavascriptPackageResource.getHeaderContribution(ChannelGridPanel.class,
                "Hippo.ChannelManager.ChannelGridPanel.js"));

        List<ExtField> fieldList = new ArrayList<ExtField>();
        fieldList.add(new ExtField("title"));
        final String hstConfigLocation = config.getString(HOST_GROUP_CONFIG_PROP);
        if (hstConfigLocation == null) {
            log.error("No host group is configured for retrieving list of channels, please set the property " +
                    HOST_GROUP_CONFIG_PROP + " on the channel-manager configuration!");

        } else {
            this.store = new ChannelStore(fieldList, new JcrNodeModel(hstConfigLocation));
            add(this.store);
        }
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("store", new JSONIdentifier(this.store.getJsObjectId()));
    }

}
