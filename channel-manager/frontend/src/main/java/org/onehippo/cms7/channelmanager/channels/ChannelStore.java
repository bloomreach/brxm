/**
 * Copyright 2011 Hippo
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

package org.onehippo.cms7.channelmanager.channels;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.security.auth.Subject;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtGroupingStore;
import org.wicketstuff.js.ext.util.ExtClass;

/**
 * Channel JSON Store.
 */
@ExtClass("Hippo.ChannelManager.ChannelStore")
public class ChannelStore extends ExtGroupingStore<Object> {

    /**
     * The first serialized version of this source. Version {@value}.
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ChannelStore.class);
    private transient Map<String, Channel> channels;

    public ChannelStore(List<ExtField> fields) {
        super(fields);
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        //Need the sortinfo and xaction params since we are using GroupingStore instead of
        //JsonStore
        final JSONObject props = super.getProperties();
        Map<String, String> sortInfo = new HashMap<String, String>();
        sortInfo.put("field", "name");
        sortInfo.put("direction", "ASC");
        Map<String, String> baseParams = new HashMap<String, String>();
        baseParams.put("xaction", "read");
        props.put("baseParams", baseParams);
        return props;
    }

    @Override
    protected long getTotal() {
        return getChannels().size();
    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray data = new JSONArray();
        for (Channel channel : getChannels().values()) {
            JSONObject object = new JSONObject();
            object.put("id", channel.getId());
            object.put("name", channel.getName());
            object.put("contentRoot", channel.getContentRoot());
            data.put(object);
        }
        return data;
    }

    @Override
    protected JSONObject createRecord(JSONObject record) throws JSONException {
        final ChannelManager channelManager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());

        // create new channel
        final String blueprintId = record.getString("blueprintId");
        final Channel newChannel;
        try {
            newChannel = channelManager.createChannel(blueprintId);
        } catch (ChannelException e) {
            final String errorMsg = "Could not create new channel with blueprint '" + blueprintId + "'";
            log.warn(errorMsg, e);
            return createdRecordResult(false, errorMsg + ": " + e.getMessage());
        }

        // set channel parameters
        newChannel.setName(record.getString("name"));
        newChannel.setUrl(record.getString("url"));

        // save channel (FIXME: move boilerplate to CMS engine)
        UserSession session = (UserSession) org.apache.wicket.Session.get();
        Credentials credentials = session.getCredentials().getJcrCredentials();
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(credentials);
        subject.setReadOnly();

        try {
            HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<Void>() {
                        public Void run() throws ChannelException {
                            channelManager.save(newChannel);
                            return null;
                        }
                    }, null);
        } catch (PrivilegedActionException e) {
            final String errorMsg = "Could not save channel '" + newChannel.getName() + "'";
            log.error(errorMsg, e.getException());
            return createdRecordResult(false, errorMsg + ": " + e.getException().getMessage());
        } finally {
            HstSubject.clearSubject();
        }

        return createdRecordResult(true, "");
    }

    private JSONObject createdRecordResult(boolean success, String message) throws JSONException {
        final JSONObject result = new JSONObject();
        result.put("success", success);
        result.put("msg", message);
        return result;
    }

    private Map<String, Channel> getChannels() {
        if (channels == null) {
            ChannelManager channelManager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
            if (channelManager != null) {
                try {
                    channels = channelManager.getChannels();
                } catch (ChannelException e) {
                    throw new RuntimeException("Unable to get the channels from Channel Manager", e);
                }
            } else {
                throw new RuntimeException("Unable to get the channels from Channel Manager");
            }
        }
        return channels;
    }
}
