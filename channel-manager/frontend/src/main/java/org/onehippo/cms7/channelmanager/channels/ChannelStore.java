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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.core.container.ComponentManager;
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

    // the names are used to access
    // the getters of Channel via reflection
    public enum Column {
        composerModeEnabled,
        contentRoot,
        hostname,
        hstConfigPath,
        hstMountPoint,
        id, // channel id
        locale,
        mountId,
        name,
        subMountPath,
        type,
        url
    }
    public static final List<String> ALL_COLUMN_NAMES = new ArrayList<String>();
    static {
        for (Column column : Column.values()) {
            ALL_COLUMN_NAMES.add(column.name());
        }
    }

    public static enum SortOrder { ascending, descending }

    /**
     * The first serialized version of this source. Version {@value}.
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ChannelStore.class);
    private transient Map<String, Channel> channels;

    private final String storeId;
    private final String sortFieldName;
    private final SortOrder sortOrder;
    private final LocaleResolver localeResolver;

    public ChannelStore(String storeId, List<ExtField> fields, String sortFieldName, SortOrder sortOrder, LocaleResolver localeResolver) {
        super(fields);
        this.storeId = storeId;
        this.sortFieldName = sortFieldName;
        this.sortOrder = sortOrder;
        this.localeResolver = localeResolver;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        //Need the sortinfo and xaction params since we are using GroupingStore instead of
        //JsonStore
        final JSONObject props = super.getProperties();

        props.put("storeId", this.storeId);

        final Map<String, String> sortInfo = new HashMap<String, String>();
        sortInfo.put("field", this.sortFieldName);
        sortInfo.put("direction", this.sortOrder.equals(SortOrder.ascending) ? "ASC" : "DESC");
        props.put("sortInfo", sortInfo);

        final Map<String, String> baseParams = new HashMap<String, String>();
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
            Map<String, Object> channelProperties = channel.getProperties();
            JSONObject object = new JSONObject();
            for (ExtField field : getFields()) {
                String fieldValue = ReflectionUtil.getStringValue(channel, field.getName());
                if (fieldValue == null) {
                    Object value = channelProperties.get(field.getName());
                    fieldValue = value == null ? StringUtils.EMPTY : value.toString();
                }
                object.put(field.getName(), fieldValue);
            }
            data.put(object);
        }

        return data;
    }

    String getColumnHeader(String columnName) {
        if (isChannelColumn(columnName)) {
            // known field of a Channel; translations are provided by the resource bundle of this class
            return getResourceValue("column." + columnName);
        }

        // custom channel property; translations are provided by the resource bundle of the custom ChannelInfo class
        getChannels();
        for (Channel channel : channels.values()) {
            String header = ChannelResourceModel.getChannelResourceValue(channel, columnName);
            if (header != null) {
                return header;
            }
        }

        log.warn("Column '{}' is not a known Channel field, and no custom ChannelInfo class contains a translation of it for locale '{}'. Falling back to the column name itself as the column header.",
                columnName, org.apache.wicket.Session.get().getLocale());

        return columnName;
    }

    private boolean isChannelColumn(String columnName) {
        try {
            Column.valueOf(columnName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String getResourceValue(String key) {
        return new ClassResourceModel(key, ChannelStore.class).getObject();
    }

    public void reload() {
        channels = null;

        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            target.prependJavascript("Ext.StoreMgr.lookup('" + this.storeId + "').reload();");
        }
    }

    @Override
    protected JSONObject createRecord(JSONObject record) throws JSONException {
        final ChannelManager channelManager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());

        // create new channel
        final String blueprintId = record.getString("blueprintId");
        final Channel newChannel;
        try {
            newChannel = channelManager.getBlueprint(blueprintId).createChannel();
        } catch (ChannelException e) {
            final String errorMsg = "Could not create new channel with blueprint '" + blueprintId + "'";
            log.warn(errorMsg, e);
            return createdRecordResult(false, errorMsg + ": " + e.getMessage());
        }

        // set channel parameters
        final String channelName = record.getString("name");
        newChannel.setName(channelName);
        newChannel.setUrl(record.getString("url"));

        final String contentRoot = record.getString("contentRoot");
        if (StringUtils.isNotEmpty(contentRoot)) {
            newChannel.setContentRoot(contentRoot);

            Locale locale = getLocale(contentRoot);
            if (locale != null) {
                newChannel.setLocale(locale.toString());
            }
        }

        // save channel (FIXME: move boilerplate to CMS engine)
        UserSession session = (UserSession) org.apache.wicket.Session.get();
        Credentials credentials = session.getCredentials();
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(credentials);
        subject.setReadOnly();

        try {
            HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<Void>() {
                        public Void run() throws ChannelException {
                            channelManager.persist(blueprintId, channelName, newChannel);
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

        // removed the old cached channels to force a refresh
        this.channels = null;

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
            // reload channels
            ComponentManager componentManager = HstServices.getComponentManager();
            if (componentManager == null) {
                log.warn("Cannot retrieve channels: HST component manager could not be loaded. Is the site running?");
                return Collections.emptyMap();
            }

            ChannelManager channelManager = componentManager.getComponent(ChannelManager.class.getName());
            if (channelManager == null) {
                log.error("Cannot retrieve channels: component '{}' not found", ChannelManager.class.getName());
                return Collections.emptyMap();
            }
            try {
                channels = channelManager.getChannels();
            } catch (ChannelException e) {
                log.error("Failed to retrieve channels", e);
                return Collections.emptyMap();
            }
        }
        return channels;
    }

    private Locale getLocale(String absPath) {
        if (StringUtils.isEmpty(absPath)) {
            return null;
        }
        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
        try {
            if (!session.nodeExists(absPath)) {
                return null;
            }
            Node node = session.getNode(absPath);
            return localeResolver.getLocale(node);
        } catch (RepositoryException e) {
            log.warn("Could not retrieve the locale of node '" + absPath + "'", e);
        }
        return null;
    }

}
