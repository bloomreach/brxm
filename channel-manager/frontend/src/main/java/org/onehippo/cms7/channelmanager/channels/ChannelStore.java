package org.onehippo.cms7.channelmanager.channels;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.site.HstServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.util.List;
import java.util.Map;

/**
 * Channel JSON Store.
 */
public class ChannelStore extends ExtJsonStore<Object> {

    private static final Logger log = LoggerFactory.getLogger(ChannelStore.class);

    private Map<String, Channel> channels;


    public ChannelStore(List<ExtField> fields) {
        super(fields);
        ChannelManager channelManager = null;
        try {
            channelManager = ((HstManager) HstServices.getComponentManager().getComponent(HstManager.class.getName())).getChannelManager();
        } catch (RepositoryNotAvailableException e) {
            log.error("Unable to get the ChannelManager from HST: " + e.getMessage(), e);
        }
        if (channelManager != null) {
            this.channels = channelManager.getChannels();
        } else {
            //TODO Show error message in the Plugin UI
        }
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        final JSONObject properties = super.getProperties();
        properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        return properties;
    }

    @Override
    protected long getTotal() {
        return this.channels.size();
    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray data = new JSONArray();
        for (Channel channel : channels.values()) {
            JSONObject object = new JSONObject();
            object.put("title", channel.getTitle());
            object.put("contentRoot", channel.getContentRoot());
            data.put(object);
        }
        return data;
    }

}
