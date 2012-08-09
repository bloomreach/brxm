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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelNotFoundException;
import org.hippoecm.hst.rest.BlueprintService;
import org.hippoecm.hst.rest.ChannelService;
import org.hippoecm.hst.rest.SiteService;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
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

    public static final String DEFAULT_TYPE = "website";
    public static final String DEFAULT_CHANNEL_ICON_PATH = "/content/gallery/channels/${name}.png/${name}.png/hippogallery:original";

    // the names are used to access
    // the getters of Channel via reflection
    public enum ChannelField {
        composerModeEnabled,
        contentRoot,
        hostname,
        hstConfigPath,
        hstMountPoint,
        hstPreviewMountPoint,
        id, // channel id
        locale,
        mountId,
        name,
        mountPath,
        cmsPreviewPrefix,
        contextPath,
        url,
        lockedBy,
        lockedOn
    }

    public static final List<String> ALL_FIELD_NAMES;
    public static final List<String> INTERNAL_FIELDS;

    static {
        List<String> names = new ArrayList<String>();
        for (ChannelField field : ChannelField.values()) {
            names.add(field.name());
        }
        ALL_FIELD_NAMES = Collections.unmodifiableList(names);
        INTERNAL_FIELDS = Collections.unmodifiableList(
                                    Arrays.asList(ChannelField.cmsPreviewPrefix.name(),
                                                  ChannelField.hstPreviewMountPoint.name()));
    }

    public static enum SortOrder {ascending, descending}

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
    private final IRestProxyService restProxyService;
    private String channelRegionIconPath = DEFAULT_CHANNEL_ICON_PATH;
    private String channelTypeIconPath = DEFAULT_CHANNEL_ICON_PATH;

    public ChannelStore(String storeId, List<ExtField> fields, String sortFieldName, SortOrder sortOrder, LocaleResolver localeResolver, IRestProxyService restProxyService) {

        super(fields);
        this.storeId = storeId;
        this.sortFieldName = sortFieldName;
        this.sortOrder = sortOrder;
        this.localeResolver = localeResolver;
        this.restProxyService = restProxyService;

        if (this.restProxyService == null) {
            log.warn("No RESTful proxy service configured!");
        }
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

        for (Channel channel : getChannels()) {
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

            populateChannelTypeAndRegion(channel, object);

            data.put(object);
        }

        return data;
    }

    private void populateChannelTypeAndRegion(final Channel channel, final JSONObject object) throws JSONException {
        String type = channel.getType();
        if (StringUtils.isEmpty(type)) {
            type = DEFAULT_TYPE;
            channel.setType(DEFAULT_TYPE);
        }
        object.put("channelType", type);

        final Map<String, String> channelFieldValues = getChannelFieldValues(channel);

        String channelIconUrl = getChannelTypeIconUrl(channelFieldValues);
        if (StringUtils.isEmpty(channelIconUrl)) {
            channelIconUrl = getIconResourceReferenceUrl(type + ".png");
        }
        object.put("channelTypeImg", channelIconUrl);

        if (StringUtils.isNotEmpty(channel.getLocale())) {
            object.put("channelRegion", channel.getLocale());
            String regionIconUrl = getChannelRegionIconUrl(channelFieldValues);
            if (StringUtils.isEmpty(regionIconUrl)) {
                regionIconUrl = getIconResourceReferenceUrl(channel.getLocale() + ".png");
            }
            if (StringUtils.isNotEmpty(regionIconUrl)) {
                object.put("channelRegionImg", regionIconUrl);
            }
        }
    }

    private Map<String, String> getChannelFieldValues(final Channel channel) {
        final Map<String, String> channelFieldValues = new HashMap<String, String>();
        for (Field field : channel.getClass().getDeclaredFields()) {
            if (field.getType() != String.class) {
                continue;
            }
            String fieldValue = ReflectionUtil.getStringValue(channel, field.getName());
            channelFieldValues.put(field.getName(), fieldValue);
        }

        final String locale = channel.getLocale();
        if (locale != null) {
            channelFieldValues.put("locale", locale.toLowerCase());
        }
        return channelFieldValues;
    }

    private String getChannelRegionIconUrl(final Map<String, String> channelFieldValues) {
        MapVariableInterpolator mapVariableInterpolator = new MapVariableInterpolator(this.channelRegionIconPath,
                                                                                      channelFieldValues);
        return getChannelIconUrl(mapVariableInterpolator.toString());
    }

    private String getChannelTypeIconUrl(final Map<String, String> channelFieldValues) {
        MapVariableInterpolator mapVariableInterpolator = new MapVariableInterpolator(this.channelTypeIconPath,
                                                                                      channelFieldValues);
        return getChannelIconUrl(mapVariableInterpolator.toString());
    }

    private String getChannelIconUrl(final String channelIconPath) {
        if (StringUtils.isEmpty(channelIconPath)) {
            return null;
        }

        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle != null) {
            javax.jcr.Session session = ((UserSession) requestCycle.getSession()).getJcrSession();
            try {
                if (session.nodeExists(channelIconPath)) {
                    String url = encodeUrl("binaries" + channelIconPath);
                    return requestCycle.getResponse().encodeURL(url).toString();
                }
            } catch (RepositoryException repositoryException) {
                log.error("Error getting the channel icon resource url.", repositoryException);
            }
        }

        return null;
    }

    private String encodeUrl(String path) {
        String[] elements = StringUtils.split(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = WicketURLEncoder.PATH_INSTANCE.encode(elements[i], "UTF-8");
        }
        return StringUtils.join(elements, '/');
    }

    public String getChannelRegionIconPath() {
        return channelRegionIconPath;
    }

    public void setChannelRegionIconPath(final String channelRegionIconPath) {
        this.channelRegionIconPath = channelRegionIconPath;
    }

    public String getChannelTypeIconPath() {
        return this.channelTypeIconPath;
    }

    public void setChannelTypeIconPath(final String channelTypeIconPath) {
        this.channelTypeIconPath = channelTypeIconPath;
    }

    private String getIconResourceReferenceUrl(final String resource) {
        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle != null) {
            ResourceReference iconResource = new ResourceReference(getClass(), resource);
            iconResource.bind(requestCycle.getApplication());
            CharSequence typeImgUrl = requestCycle.urlFor(iconResource);
            return typeImgUrl.toString();
        }
        return null;
    }

    String getLocalizedFieldName(String fieldName) {
        if (isChannelField(fieldName)) {
            // known field of a Channel; translations are provided by the resource bundle of this class
            return getResourceValue("field." + fieldName);
        }

        // Custom channel property; translations are provided by the resource bundle of the custom ChannelInfo class
        Properties properties;

        for (Channel channel : getChannels()) {
            properties = getChannelResourceValues(channel);
            String header = properties.getProperty(fieldName);
            if (header != null) {
                return header;
            }
        }

        // No translation found, is the site down?
        SiteService siteService = restProxyService.createRestProxy(SiteService.class);
        if (!siteService.isAlive()) {
            log.info(
                    "Field '{}' is not a known Channel field, and no custom ChannelInfo class contains a translation of it for locale '{}'. It looks like the site is down. Falling back to the field name itself as the column header.",
                    fieldName, Session.get().getLocale());
        } else {
            log.warn(
                    "Field '{}' is not a known Channel field, and no custom ChannelInfo class contains a translation of it for locale '{}'. Falling back to the field name itself as the column header.",
                    fieldName, Session.get().getLocale());
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
        ChannelService channelService = restProxyService.createSecureRestProxy(ChannelService.class);
        return channelService.canUserModifyChannels();
    }

    public List<Channel> getChannels() {
        if (channels == null) {
            loadChannels();
        }

        return Collections.unmodifiableList(new ArrayList<Channel>(channels.values()));
    }

    public Channel getChannel(String id) throws ChannelNotFoundException {
        if (channels == null) {
            loadChannels();
        }

        Channel channel = channels.get(id);
        if (channel == null) {
            log.warn("No channel found with id '{}'!", id);
            throw new ChannelNotFoundException("No channel found with id '" + id + "'");
        }

        return channel;
    }

    public List<HstPropertyDefinitionInfo> getChannelPropertyDefinitions(Channel channel) {
        ChannelService channelService = restProxyService.createRestProxy(ChannelService.class);
        return channelService.getChannelPropertyDefinitions(channel.getId());
    }

    public Properties getChannelResourceValues(Channel channel) {
        ChannelService channelService = restProxyService.createRestProxy(ChannelService.class);
        return channelService.getChannelResourceValues(channel.getId(), Session.get().getLocale().toString());
    }

    public void saveChannel(Channel channel) {
        ChannelService channelService = restProxyService.createSecureRestProxy(ChannelService.class);
        channelService.save(channel);
    }

    /**
     * @param channel the channel to get the ChannelInfo class for
     * @return the ChannelInfo class for the given channel, or <code>null</code> if the channel does not have a custom
     *         ChannelInfo class or the channel manager could not be loaded (e.g. because the site is down).
     */
    public ChannelInfoClassInfo getChannelInfoClassInfo(Channel channel) {
        ChannelService channelService = restProxyService.createRestProxy(ChannelService.class);
        return channelService.getChannelInfoClassInfo(channel.getId());
    }

    public void update() {
        int previous = getChannelsHash();
        loadChannels();
        if (getChannelsHash() != previous) {
            reload();
        }
    }

    private int getChannelsHash() {
        int hashCode = 0;
        if (channels != null) {
            for (Channel channel : channels.values()) {
                hashCode += channel.toString().hashCode();
            }
        }
        return hashCode;
    }

    protected void loadChannels() {
        ChannelService channelService = restProxyService.createRestProxy(ChannelService.class);

        if (channelService == null) {
            log.error("Could not get channelservice REST service!");
        }

        List<Channel> channelsList = channelService.getChannels();
        channels = new HashMap<String, Channel>(channelsList.size());
        for (Channel channel : channelsList) {
            channels.put(channel.getId(), channel);
        }
    }

    @Override
    protected JSONObject createRecord(JSONObject record) throws ActionFailedException, JSONException {
        // Create new channel
        final String blueprintId = record.getString("blueprintId");
        final Channel newChannel;
        BlueprintService blueprintService = restProxyService.createRestProxy(BlueprintService.class);

        if (blueprintService == null) {
            log.error("Could not get blueprint REST service!");
        }

        newChannel = blueprintService.getBlueprint(blueprintId).getPrototypeChannel();

        // Set channel parameters
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
                    log.info(
                            "Ignoring locale '{}' of the content root path '{}' of channel '{}': the locale does not define a language",
                            new Object[]{locale, contentRoot, channelName});
                }
            }
        }

        String channelId = persistChannel(blueprintId, newChannel);
        log.info("Created new channel with ID '{}'", channelId);

        // Removed the old cached channels to force a refresh
        this.channels = null;

        // No need to return the create record; it will be loaded when the list of channels is refreshed
        return null;
    }

    protected String persistChannel(String blueprintId, Channel newChannel) {
        ChannelService channelService = restProxyService.createSecureRestProxy(ChannelService.class);
        return channelService.persist(blueprintId, newChannel);
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
