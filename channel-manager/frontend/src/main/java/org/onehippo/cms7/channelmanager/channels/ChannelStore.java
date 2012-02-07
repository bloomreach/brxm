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
import java.util.Arrays;
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
import org.wicketstuff.js.ext.data.ActionFailedException;
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
    public enum ChannelField {
        composerModeEnabled,
        contentRoot,
        hostname,
        hstConfigPath,
        hstMountPoint,
        id, // channel id
        locale,
        mountId,
        name,
        mountPath,
        cmsPreviewPrefix,
        contextPath,
        type,
        url
    }
    public static final List<String> ALL_FIELD_NAMES;
    public static final List<String> INTERNAL_FIELDS;
    static {
        List<String> names = new ArrayList<String>();
        for (ChannelField field : ChannelField.values()) {
            names.add(field.name());
        }
        ALL_FIELD_NAMES = Collections.unmodifiableList(names);
        INTERNAL_FIELDS = Collections.unmodifiableList(Arrays.asList(ChannelField.cmsPreviewPrefix.name()));
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

    String getLocalizedFieldName(String fieldName) {
        if (isChannelField(fieldName)) {
            // known field of a Channel; translations are provided by the resource bundle of this class
            return getResourceValue("field." + fieldName);
        }

        // custom channel property; translations are provided by the resource bundle of the custom ChannelInfo class
        Map<String, Channel> channelMap = getChannels();
        for (Channel channel : channelMap.values()) {
            String header = ChannelResourceModel.getChannelResourceValue(channel, fieldName);
            if (header != null) {
                return header;
            }
        }

        // no translation found; is the site down?
        if (ChannelUtil.getChannelManager() == null) {
            log.info("Field '{}' is not a known Channel field, and no custom ChannelInfo class contains a translation of it for locale '{}'. It looks like the site is down. Falling back to the field name itself as the column header.",
                    fieldName, org.apache.wicket.Session.get().getLocale());
        } else {
            log.warn("Field '{}' is not a known Channel field, and no custom ChannelInfo class contains a translation of it for locale '{}'. Falling back to the field name itself as the column header.",
                    fieldName, org.apache.wicket.Session.get().getLocale());
        }

        return fieldName;
    }

    private boolean isChannelField(String fieldName) {
        try {
            ChannelField.valueOf(fieldName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String getResourceValue(String key, Object... parameters) {
        return new ClassResourceModel(key, ChannelStore.class, parameters).getObject();
    }

    public void reload() {
        channels = null;

        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            target.prependJavascript("Ext.StoreMgr.lookup('" + this.storeId + "').reload();");
        }
    }

    public boolean canModifyChannels() {
        final ChannelManager channelManager = ChannelUtil.getChannelManager();
        if (channelManager == null) {
            log.info("Cannot retrieve the channel manager, assuming that the current user cannot modify channels");
            return false;
        }

        Subject subject = createSubject();
        try {
            return HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws ChannelException {
                    return channelManager.canUserModifyChannels();
                }
            }, null);
        } catch (PrivilegedActionException e) {
            log.error("Could not determine privileges", e.getException());
            return false;
        } finally {
            HstSubject.clearSubject();
        }
    }

    @Override
    protected JSONObject createRecord(JSONObject record) throws ActionFailedException, JSONException {
        final ChannelManager channelManager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());

        // create new channel
        final String blueprintId = record.getString("blueprintId");
        final Channel newChannel;
        try {
            newChannel = channelManager.getBlueprint(blueprintId).createChannel();
        } catch (ChannelException e) {
            log.warn("Could not create new channel with blueprint '{}': {}", blueprintId, e.getMessage());
            log.debug("Cause:", e);
            throw new ActionFailedException(getResourceValue("error.blueprint.cannot.create.channel", blueprintId), e);
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
                if (StringUtils.isNotBlank(locale.getLanguage())) {
                    newChannel.setLocale(locale.toString());
                } else {
                    log.info("Ignoring locale '{}' of the content root path '{}' of channel '{}': the locale does not define a language",
                            new Object[]{locale, contentRoot, channelName});
                }
            }
        }

        // save channel (FIXME: move boilerplate to CMS engine)
        Subject subject = createSubject();
        try {
            String channelId = HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<String>() {
                        public String run() throws ChannelException {
                            return channelManager.persist(blueprintId, newChannel);
                        }
                    }, null);
            log.info("Created new channel with ID '{}'", channelId);
        } catch (PrivilegedActionException e) {
            log.info("Could not persist new channel '" + newChannel.getName() + "'", e.getException());
            throw createActionFailedException(e.getException(), newChannel);
        } finally {
            HstSubject.clearSubject();
        }

        // removed the old cached channels to force a refresh
        this.channels = null;

        // no need to return the create record; it will be loaded when the list of channels is refreshed
        return null;
    }

    private Subject createSubject() {
        UserSession session = (UserSession) Session.get();

        @SuppressWarnings("deprecation")
        Credentials credentials = session.getCredentials();

        Subject subject = new Subject();
        subject.getPrivateCredentials().add(credentials);
        subject.setReadOnly();
        return subject;
    }

    private ActionFailedException createActionFailedException(Exception cause, Channel newChannel) {
        if (cause instanceof ChannelException) {
            ChannelException ce = (ChannelException)cause;
            switch(ce.getType()) {
                case MOUNT_NOT_FOUND:
                case MOUNT_EXISTS:
                    String channelUrl = newChannel.getUrl();
                    String parentUrl = StringUtils.substringBeforeLast(channelUrl, "/");
                    return new ActionFailedException(getResourceValue("channelexception." + ce.getType().getKey(), channelUrl, parentUrl), cause);
                default:
                    return new ActionFailedException(getResourceValue("channelexception." + ce.getType().getKey(), (Object[])ce.getParameters()), cause);
            }
        }
        log.warn("Could not create new channel '" + newChannel.getName() + "': " + cause.getMessage());
        log.debug("Stacktrace:", cause);
        return new ActionFailedException(getResourceValue("error.cannot.create.channel", newChannel.getName()));
    }

    private Map<String, Channel> getChannels() {
        if (channels == null) {
            // reload channels
            ChannelManager channelManager = ChannelUtil.getChannelManager();
            if (channelManager == null) {
                log.info("Cannot load the channel manager. No channels will be shown.");
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
