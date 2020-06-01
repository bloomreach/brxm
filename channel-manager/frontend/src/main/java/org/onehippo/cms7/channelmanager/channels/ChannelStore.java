/*
 * Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ChannelManagerHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ActionFailedException;
import org.wicketstuff.js.ext.data.ExtDataField;
import org.wicketstuff.js.ext.data.ExtGroupingStore;
import org.wicketstuff.js.ext.util.ExtClass;

import static org.onehippo.cms7.channelmanager.HstUtil.getHostGroup;

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
        hostGroup,
        hstConfigPath,
        hstMountPoint,
        id, // channel id
        locale,
        channelNodeLockedBy,
        mountId,
        name,
        mountPath,
        cmsPreviewPrefix,
        contextPath,
        url,
        changedBySet,
        defaultDevice,
        devices,
        previewHstConfigExists
    }

    public static final List<String> ALL_FIELD_NAMES;
    public static final List<String> INTERNAL_FIELDS;

    static {
        List<String> names = new ArrayList<>();
        for (ChannelField field : ChannelField.values()) {
            names.add(field.name());
        }
        ALL_FIELD_NAMES = Collections.unmodifiableList(names);
        INTERNAL_FIELDS = Collections.unmodifiableList(
                Arrays.asList(ChannelField.cmsPreviewPrefix.name(),
                        ChannelField.changedBySet.name(),
                        ChannelField.devices.name(),
                        ChannelField.previewHstConfigExists.name(),
                        ChannelField.hostGroup.name()));
    }

    public enum SortOrder {ascending, descending}

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
    private final BlueprintStore blueprintStore;
    private final Map<String, Properties> channelResourcesCache;
    private String channelRegionIconPath = DEFAULT_CHANNEL_ICON_PATH;
    private String channelTypeIconPath = DEFAULT_CHANNEL_ICON_PATH;

    public ChannelStore(final String storeId,
                        final List<ExtDataField> fields,
                        final String sortFieldName,
                        final SortOrder sortOrder,
                        final LocaleResolver localeResolver,
                        final BlueprintStore blueprintStore) {

        super(fields);
        this.storeId = storeId;
        this.sortFieldName = sortFieldName;
        this.sortOrder = sortOrder;
        this.localeResolver = localeResolver;
        this.blueprintStore = blueprintStore;
        this.channelResourcesCache = new HashMap<>();

    }

    @Override
    public void renderHead(Component component, final IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(ChannelManagerHeaderItem.get());
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        //Need the sortinfo and xaction params since we are using GroupingStore instead of
        //JsonStore
        final JSONObject props = super.getProperties();

        props.put("storeId", this.storeId);

        final Map<String, String> sortInfo = new HashMap<>();
        sortInfo.put("field", this.sortFieldName);
        sortInfo.put("direction", this.sortOrder.equals(SortOrder.ascending) ? "ASC" : "DESC");
        props.put("sortInfo", sortInfo);

        final Map<String, String> baseParams = new HashMap<>();
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

        channels = null;
        for (Channel channel : getChannels()) {
            Map<String, Object> channelProperties = channel.getProperties();
            JSONObject object = new JSONObject();

            for (ExtDataField field : getFields()) {
                if (ChannelField.devices.name().equals(field.getName())) {
                    JSONArray values = new JSONArray();
                    final List<String> devices = channel.getDevices();
                    if (devices != null) {
                        for (String device : devices) {
                            values.put(device);
                        }
                    }
                    object.put(field.getName(), values);
                } else if (ChannelField.changedBySet.name().equals(field.getName())) {
                    JSONArray values = new JSONArray();
                    final Set<String> lockedBySet = channel.getChangedBySet();
                    if (lockedBySet != null) {
                        for (String lockedBy : lockedBySet) {
                            // deserialization seems to insert a null element for empty set, hence check for null
                            if (lockedBy == null) {
                                continue;
                            }
                            values.put(lockedBy);
                        }
                    }
                    object.put(field.getName(), values);
                } else {
                    String fieldValue = ReflectionUtil.getStringValue(channel, field.getName());
                    if (fieldValue == null) {
                        Object value = channelProperties.get(field.getName());
                        fieldValue = value == null ? StringUtils.EMPTY : value.toString();
                    }
                    object.put(field.getName(), fieldValue);
                }

            }

            populateChannelType(channel, object);
            populateChannelRegion(channel, object);

            data.put(object);
        }

        return data;
    }

    protected void populateChannelType(final Channel channel, final JSONObject object) throws JSONException {
        String type = channel.getType();
        object.put("channelType", type);

        final Map<String, String> channelFieldValuesWithType = new HashMap<>();
        channelFieldValuesWithType.put("type", channel.getType());

        String channelIconUrl = getChannelIconUrl(channelFieldValuesWithType, getChannelTypeIconPath());
        if (StringUtils.isEmpty(channelIconUrl)) {
            channelIconUrl = getIconResourceReferenceUrl(type + ".png");
        }
        object.put("channelTypeImg", channelIconUrl);
    }

    protected void populateChannelRegion(final Channel channel, final JSONObject object) throws JSONException {
        if (StringUtils.isNotEmpty(channel.getLocale())) {
            object.put("channelRegion", channel.getLocale());

            final Map<String, String> channelFieldValuesWithRegion = new HashMap<>();
            channelFieldValuesWithRegion.put("region", channel.getLocale());

            String regionIconUrl = getChannelIconUrl(channelFieldValuesWithRegion, getChannelRegionIconPath());
            if (StringUtils.isEmpty(regionIconUrl)) {
                regionIconUrl = getIconResourceReferenceUrl(channel.getLocale() + ".png");
            }
            if (StringUtils.isNotEmpty(regionIconUrl)) {
                object.put("channelRegionImg", regionIconUrl);
            }
        }
    }

    protected String getChannelIconUrl(final Map<String, String> channelFieldValues, final String channelIconPathTemplate) {
        String channelIconPath = new MapVariableInterpolator(channelIconPathTemplate, channelFieldValues).toString();
        if (StringUtils.isEmpty(channelIconPath)) {
            return null;
        }

        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle != null) {
            javax.jcr.Session session = getUserJcrSession();
            try {
                if (session.nodeExists(channelIconPath)) {
                    String url = encodeUrl("binaries" + channelIconPath);
                    return requestCycle.getResponse().encodeURL(url);
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
            elements[i] = UrlEncoder.PATH_INSTANCE.encode(elements[i], "UTF-8");
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

    protected String getIconResourceReferenceUrl(final String resource) {
        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle != null) {
            PackageResourceReference iconResource = new PackageResourceReference(getClass(), resource);
            return ChannelStore.resourceExists(iconResource) ?
                    requestCycle.urlFor(new ResourceReferenceRequestHandler(iconResource)).toString() :
                    null;
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

        Channel channel = null;
        try {
            for (final Channel c : getChannels()) {
                channel = c;
                properties = getChannelResourceValues(channel);
                String header = properties.getProperty(fieldName);
                if (header != null) {
                    return header;
                }
            }
        } catch (ChannelException ce) {
            final String channelId = (channel == null) ? "" : channel.getId();
            if (log.isDebugEnabled()) {
                log.warn("Could not get localized value of field '{}' for channel with id '{}'", fieldName, channelId, ce);
            } else {
                log.warn("Could not get localized value of field '{}' for channel with id '{}' - {}", fieldName, channelId, ce);

            }
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

    protected static boolean resourceExists(final PackageResourceReference iconResource) {
        IResourceStream resourceStream = null;
        try{
            resourceStream = iconResource.getResource().getResourceStream();
            return resourceStream != null;
        } catch (Exception e) {
            log.warn("Resource could not be found and exception was thrown:", e);
            return false;
        } finally {
            if(resourceStream != null){
                try {
                    resourceStream.close();
                } catch (IOException e) {
                    log.warn("Could not close resource stream. This may create a memory leak");
                }
            }
        }
    }

    public void reload() {
        channels = null;

        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.prependJavaScript("Ext.StoreMgr.lookup('" + this.storeId + "').reload();");
        }
    }

    private ChannelService getChannelService() {
        return HippoServiceRegistry.getService(PlatformServices.class).getChannelService();
    }

    javax.jcr.Session getUserJcrSession() {
        return UserSession.get().getJcrSession();
    }

    public List<Channel> getChannels() {
        if (channels == null) {
            loadChannels();
        }
        return Collections.unmodifiableList(new ArrayList<>(channels.values()));
    }

    public Properties getChannelResourceValues(final Channel channel) throws ChannelException {
        return getCachedChannelResources(channel);
    }

    private Properties getCachedChannelResources(final Channel channel) throws ChannelException {
        Properties resources = channelResourcesCache.get(channel.getId());

        if (resources == null || Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            resources = fetchChannelResources(channel);
            channelResourcesCache.put(channel.getId(), resources);
        } else {
            log.info("Using cached i18n resources for channel '{}'", channel.getId());
        }

        return resources;
    }

    private Properties fetchChannelResources(final Channel channel) throws ChannelException {
        log.info("Fetching i18n resources for channel '{}'", channel.getId());
        final String locale = Session.get().getLocale().toString();
        try {
            return getChannelService().getChannelResourceValues(getHostGroup(), channel.getId(), locale);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Cannot get channels resources", e);
            } else {
                log.warn("Cannot get channels resources: {}", e.getMessage());
            }
            return new Properties();
        }
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
                hashCode += channel.hashCode();
            }
        }
        return hashCode;
    }

    protected void loadChannels() {

        List<Channel> channelList;
        try {
            channelList = getChannelService().getPreviewChannels(getUserJcrSession(), getHostGroup());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Cannot get channels", e);
            } else {
                log.warn("Cannot get channels: {}", e.getMessage());
            }
            channelList = Collections.emptyList();
        }
        channels = channelList.stream().map(channel -> {
            if (StringUtils.isEmpty(channel.getType())) {
                channel.setType(DEFAULT_TYPE);
            }
            return channel;
        }).collect(Collectors.toMap(Channel::getId, Function.identity()));

    }

    @Override
    protected JSONObject createRecord(JSONObject record) throws ActionFailedException, JSONException {
        // Create new channel
        final String blueprintId = record.getString("blueprintId");
        final Blueprint blueprint = blueprintStore.getBlueprints().stream()
                .filter(b -> b.getId().equals(blueprintId))
                .findFirst()
                .orElseThrow(() -> new ActionFailedException(String.format("Cannot find blueprint for id '%s'.", blueprintId)));

        final Channel newChannel = blueprint.getPrototypeChannel();
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
                            locale, contentRoot, channelName);
                }
            }
        }

        String channelId;
        try {
            channelId = persistChannel(blueprintId, newChannel);
        } catch (ChannelException ce) {
            throw createActionFailedException(ce, newChannel);
        }

        log.info("Created new channel with ID '{}'", channelId);

        // Removed the old cached channels to force a refresh
        this.channels = null;

        // No need to return the create record; it will be loaded when the list of channels is refreshed
        return null;
    }

    protected String persistChannel(String blueprintId, Channel newChannel) throws ChannelException {
        return getChannelService().persist(getUserJcrSession(), blueprintId, newChannel);
    }

    private Locale getLocale(String absPath) {
        if (StringUtils.isEmpty(absPath)) {
            return null;
        }
        javax.jcr.Session session = getUserJcrSession();
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

    private ActionFailedException createActionFailedException(Exception cause, Channel newChannel) {
        if (cause instanceof ChannelException) {
            ChannelException ce = (ChannelException) cause;
            switch (ce.getType()) {
                case MOUNT_NOT_FOUND:
                case MOUNT_EXISTS:
                    String channelUrl = newChannel.getUrl();
                    String parentUrl = StringUtils.substringBeforeLast(channelUrl, "/");
                    return new ActionFailedException(getResourceValue("channelexception." + ce.getType().getKey(), channelUrl, parentUrl), cause);
                default:
                    return new ActionFailedException(getResourceValue("channelexception." + ce.getType().getKey(), (Object[]) ce.getParameters()), cause);
            }
        }
        log.warn("Could not create new channel '{}': {}", newChannel.getName(), cause.getMessage());
        log.debug("Stacktrace:", cause);
        return new ActionFailedException(getResourceValue("error.cannot.create.channel", newChannel.getName()));
    }

}
