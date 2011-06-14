package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.json.JSONException;
import org.json.JSONObject;
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
    private ChannelStore store;

    public ChannelGridPanel(String id) {
        super(id);
        add(JavascriptPackageResource.getHeaderContribution(ChannelGridPanel.class,
                "Hippo.ChannelManager.ChannelGridPanel.js"));

        List<ExtField> fieldList = new ArrayList<ExtField>();
        fieldList.add(new ExtField("title"));
        this.store = new ChannelStore(fieldList);
        add(this.store);
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
