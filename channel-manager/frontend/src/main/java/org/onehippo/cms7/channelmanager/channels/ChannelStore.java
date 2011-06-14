package org.onehippo.cms7.channelmanager.channels;

import org.json.JSONArray;
import org.json.JSONException;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;

import java.util.List;

/**
 * Channel JSON Store.
 */
public class ChannelStore extends ExtJsonStore {

    public ChannelStore(List<ExtField> fields) {
        super(fields);
    }

    @Override
    protected JSONArray getData() throws JSONException {
        return null;
    }
}
