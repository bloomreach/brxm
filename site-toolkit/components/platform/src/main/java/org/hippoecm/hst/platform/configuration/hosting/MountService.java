/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration.hosting;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_HST_LINK_URL_PREFIX;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PAGE_MODEL_API;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_RESPONSE_HEADERS;
import static org.hippoecm.hst.configuration.HstNodeTypes.MOUNT_PROPERTY_NOCHANNELINFO;
import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.platform.configuration.site.HstSiteFactory;
import org.hippoecm.hst.platform.configuration.site.MountSiteMapConfiguration;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.util.HttpHeaderUtils;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class MountService implements ContextualizableMount, MutableMount {

    private static final Logger log = LoggerFactory.getLogger(MountService.class);

    private static final String DEFAULT_TYPE = Mount.LIVE_NAME;
    /**
     * The name of this {@link Mount}. If it is the root, it is called hst:root
     */
    private String name;

    private String jcrLocation;

    /**
     * the identifier of this {@link Mount}
     */
    private String uuid;

    /**
     * The virtual host of where this {@link Mount} belongs to
     */
    private VirtualHost virtualHost;

    /**
     * The parent of this {@link Mount} or null when this {@link Mount} is the root
     */
    private Mount parent;

    /**
     * the HstSite this {@link Mount} points to. It can be <code>null</code>
     */
    private HstSite hstSite;

    /**
     * the previewHstSite equivalent of this {@link Mount}. If this {@link Mount} is a preview,
     * then previewHstSite == hstSite.  It can be <code>null</code>
     */
    private HstSite previewHstSite;

    /**
     * The child {@link Mount} below this {@link Mount}
     */
    private Map<String, MutableMount> childMountServices = new HashMap<String, MutableMount>();

    /**
     * the alias of this {@link Mount}. <code>null</code> if there is no alias property
     */
    private String alias;

    private Map<String, Object> allProperties;

    /**
     * The primary type of this {@link Mount}. If not specified, we use {@link #DEFAULT_TYPE} as a value
     */
    private String type = DEFAULT_TYPE;

    /**
     * The list of types excluding the primary <code>type</code> this {@link Mount} also belongs to
     */
    private List<String> types;


    /**
     * When the {@link Mount} is preview, and this isVersionInPreviewHeader is true, the used HST version is set as a response header.
     * Default this variable is true when it is not configured explicitly
     */
    private boolean versionInPreviewHeader;

    /**
     * If this {@link Mount} must use some custom other than the default pipeline, the name of the pipeline is contained by <code>namedPipeline</code>
     */
    private String namedPipeline;


    /**
     * The mountpath of this {@link Mount}. Note that it can contain wildcards
     */
    private String mountPath;

    /**
     * The absolute path of the content
     */
    private String contentPath;

    /**
     * The path where the {@link Mount} is pointing to
     */
    private String mountPoint;

    /**
     * If a {@link Mount} has 'noChannelInfo=true', it means that it is not visible in the channel manager as channel.
     * (For example needed to skip rest endpoints that do point to an hst site because it uses a sitemap)
     */
    private boolean noChannelInfo;

    /**
     * <code>true</code> (default) when this {@link Mount} is used as a site. False when used only as content mount point and possibly a namedPipeline
     */
    private boolean isMapped = true;

    /**
     * The homepage for this {@link Mount}. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHost} is
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;


    /**
     * The pagenotfound for this {@link Mount}. When the backing configuration does not contain a pagenotfound, then, the pagenotfound from the backing {@link VirtualHost} is
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;

    /**
     * whether the context path should be in the url.
     */
    private boolean contextPathInUrl;

    /**
     * whether the port number should be in the url.
     */
    private boolean showPort;

    /**
     * default port is 0, which means, the {@link Mount} is port agnostic
     */
    private int port;

    private String scheme;
    private boolean schemeAgnostic;

    // volatile because lazy computed after model has been loaded
    private volatile Boolean containsMultipleSchemes = null;
    private int schemeNotMatchingResponseCode = -1;

    /**
     * The locale for this {@link Mount}. When the backing configuration does not contain a locale, the value from a parent {@link Mount} is used. If there is
     * no parent, the value will be {@link VirtualHosts#getLocale()}. The locale can be <code>null</code>
     */
    private String locale;

    private boolean authenticated;

    private Set<String> roles;

    private Set<String> users;

    private boolean subjectBasedSession;

    private boolean sessionStateful;

    private final boolean cacheable;

    private String [] defaultResourceBundleIds;

    private String formLoginPage;

    private String[] defaultSiteMapItemHandlerIds;

    private Map<String, String> parameters;

    private HstSiteMapMatcher matcher;

    private Map<String, String> responseHeaders;

    private Set<String> finalPipelines = ImmutableSet.of(PAGE_MODEL_PIPELINE_NAME);

    private String hstLinkUrlPrefix;

    // if non-null, the name of the autocreated PMA child mount
    private String pageModelApi;

    public MountService(final HstNode mount,
                        final Mount parent,
                        final VirtualHost virtualHost,
                        final HstNodeLoadingCache hstNodeLoadingCache,
                        final HstConfigurationLoadingCache hstConfigurationLoadingCache,
                        final int port) throws ModelLoadingException {
        this.virtualHost = virtualHost;
        this.parent = parent;
        this.port = port;
        this.name = StringPool.get(mount.getValueProvider().getName());
        this.jcrLocation = mount.getValueProvider().getPath();
        this.uuid = mount.getValueProvider().getIdentifier();

        matcher =  HippoServiceRegistry.getService(HstModelRegistry.class).getHstModel(getContextPath()).getHstSiteMapMatcher();

        // default for when there is no alias property

        this.allProperties = mount.getValueProvider().getProperties();

        parameters = new HashMap<String, String>();
        String[] parameterNames = mount.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = mount.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);
        if(parameterNames != null && parameterValues != null){
            if(parameterNames.length != parameterValues.length) {
                log.warn("Skipping parameters for mount '{}' at '{}' because they only make sense if there are equal number of names and values",
                        getName(), mount.getValueProvider().getPath());
            }  else {
                for(int i = 0; i < parameterNames.length ; i++) {
                    this.parameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                }
            }
        }
        if(parent != null){
            // add the parent parameters that are not already present
            for(Entry<String, String> parentParam : parent.getParameters().entrySet()) {
                if(!parameters.containsKey(parentParam.getKey())) {
                    parameters.put(StringPool.get(parentParam.getKey()), StringPool.get(parentParam.getValue()));
                }
            }
        }
        parameters = CollectionOptimizer.optimizeHashMap(this.parameters);

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ALIAS)) {
            this.alias = StringPool.get(mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_ALIAS).toLowerCase());
        }

        if(parent == null) {
            mountPath = "";
        } else {
            mountPath = StringPool.get((parent.getMountPath() + "/" + name));
        }

        // is the context path visible in the url
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SHOWCONTEXTPATH);
        } else {
            if(parent != null) {
                this.contextPathInUrl = parent.isContextPathInUrl();
            } else {
                this.contextPathInUrl = virtualHost.isContextPathInUrl();
            }
        }

        // is the port number visible in the url
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SHOWPORT)) {
            this.showPort = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SHOWPORT);
        } else {
            if(parent != null) {
                this.showPort = parent.isPortInUrl();
            } else {
                this.showPort = virtualHost.isPortInUrl();
            }
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SCHEME)) {
            scheme = StringPool.get(mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_SCHEME));
        }
        if (StringUtils.isBlank(scheme)) {
            scheme = parent != null ? parent.getScheme() : virtualHost.getScheme();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC)) {
            schemeAgnostic = mount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC);
        } else {
            schemeAgnostic = parent != null ? parent.isSchemeAgnostic() : virtualHost.isSchemeAgnostic();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            schemeNotMatchingResponseCode = (int)mount.getValueProvider().getLong(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (!ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(schemeNotMatchingResponseCode)) {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new String[]{HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        mount.getValueProvider().getPath(), ConfigurationUtils.supportedSchemeNotMatchingResponseCodesAsString()});
                schemeNotMatchingResponseCode = -1;
            }
        }
        if (schemeNotMatchingResponseCode == -1) {
            schemeNotMatchingResponseCode = parent != null ?
                    parent.getSchemeNotMatchingResponseCode() : virtualHost.getSchemeNotMatchingResponseCode();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
            homepage = StringPool.get(homepage);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.homepage = parent.getHomePage();
            } else {
                this.homepage = virtualHost.getHomePage();
            }
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
            this.locale = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
            locale = StringPool.get(locale);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.locale = parent.getLocale();
            } else {
                this.locale = virtualHost.getLocale();
            }
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
            pageNotFound = StringPool.get(pageNotFound);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.pageNotFound = parent.getPageNotFound();
            } else {
                this.pageNotFound = virtualHost.getPageNotFound();
            }
        }


        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = mount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.versionInPreviewHeader = parent.isVersionInPreviewHeader();
            } else {
                this.versionInPreviewHeader = virtualHost.isVersionInPreviewHeader();
            }
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_TYPE)) {
            this.type = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_TYPE);
            type = StringPool.get(type);
            if (!(Mount.LIVE_NAME.equals(type) || Mount.PREVIEW_NAME.equals(type))) {
                log.error("Invalid type '{}' found for mount '{}'. Only supported values are '{}' or '{}'",
                        type, jcrLocation, Mount.LIVE_NAME, Mount.PREVIEW_NAME);
            }
        } else if(parent != null) {
            this.type = parent.getType();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_TYPES)) {
            String[] typesProperty = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_TYPES);
            for(int i = 0; i< typesProperty.length ; i++) {
                typesProperty[i] = StringPool.get(typesProperty[i]);
            }
            this.types = Arrays.asList(typesProperty);
        } else if(parent != null) {
            // because the parent.getTypes also includes the primary type, below we CANNOT use parent.getTypes() !!
            this.types = ((MountService)parent).types;
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ISMAPPED)) {
            this.isMapped = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_ISMAPPED);
        } else if(parent != null) {
            this.isMapped = parent.isMapped();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_NAMEDPIPELINE);
            namedPipeline  = StringPool.get(namedPipeline);
        } else if(parent != null) {
            this.namedPipeline = parent.getNamedPipeline();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            this.mountPoint = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT);
            mountPoint = StringPool.get(mountPoint);
            // now, we need to create the HstSite object
            if("".equals(mountPoint)){
                mountPoint = null;
            }
        } else if(parent != null) {
            this.mountPoint = ((MountService)parent).mountPoint;
            if(mountPoint != null) {
                log.info("mountPoint for Mount '{}' is inherited from its parent Mount and is '{}'", getName() , mountPoint);
            }
        }

        if (mount.getValueProvider().hasProperty(MOUNT_PROPERTY_NOCHANNELINFO)) {
            noChannelInfo = mount.getValueProvider().getBoolean(MOUNT_PROPERTY_NOCHANNELINFO);
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_AUTHENTICATED)) {
            this.authenticated = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_AUTHENTICATED);
        } else if (parent != null){
            this.authenticated = parent.isAuthenticated();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ROLES)) {
            String [] rolesProp = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_ROLES);
            this.roles = new HashSet<String>();
            CollectionUtils.addAll(this.roles, rolesProp);
        } else if (parent != null){
            this.roles = new HashSet<String>(parent.getRoles());
        } else {
            this.roles = new HashSet<String>();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_USERS)) {
            String [] usersProp = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_USERS);
            this.users = new HashSet<String>();
            CollectionUtils.addAll(this.users, usersProp);
        } else if (parent != null){
            this.users = new HashSet<String>(parent.getUsers());
        } else {
            this.users = new HashSet<String>();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SUBJECTBASEDSESSION)) {
            this.subjectBasedSession = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SUBJECTBASEDSESSION);
        } else if (parent != null){
            this.subjectBasedSession = parent.isSubjectBasedSession();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SESSIONSTATEFUL)) {
            this.sessionStateful = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SESSIONSTATEFUL);
        } else if (parent != null){
            this.sessionStateful = parent.isSessionStateful();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_FORMLOGINPAGE)) {
            this.formLoginPage = StringPool.get(mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_FORMLOGINPAGE));
        } else if (parent != null){
            this.formLoginPage = parent.getFormLoginPage();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            this.cacheable = mount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
        } else if(parent != null) {
            this.cacheable = parent.isCacheable();
        } else {
            this.cacheable =  virtualHost.isCacheable();
        }

        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID)) {
            this.defaultResourceBundleIds = StringUtils.split(mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID), " ,\t\f\r\n");
        } else if(parent != null) {
            this.defaultResourceBundleIds = parent.getDefaultResourceBundleIds();
        } else {
            this.defaultResourceBundleIds =  virtualHost.getDefaultResourceBundleIds();
        }

        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_DEFAULTSITEMAPITEMHANDLERIDS)) {
            defaultSiteMapItemHandlerIds = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_DEFAULTSITEMAPITEMHANDLERIDS);
        } else if (parent != null) {
            defaultSiteMapItemHandlerIds = parent.getDefaultSiteMapItemHandlerIds();
        }

        if (mount.getValueProvider().hasProperty(GENERAL_PROPERTY_RESPONSE_HEADERS)) {
            String[] resHeaders = mount.getValueProvider().getStrings(GENERAL_PROPERTY_RESPONSE_HEADERS);
            if (resHeaders.length != 0) {
                responseHeaders = HttpHeaderUtils.parseHeaderLines(resHeaders);
            }
        } else if (parent != null) {
            Map<String, String> resHeaderMap = parent.getResponseHeaders();
            if (resHeaderMap != null && !resHeaderMap.isEmpty()) {
                responseHeaders = new LinkedHashMap<>(resHeaderMap);
            }
        } else {
            Map<String, String> resHeaderMap = virtualHost.getResponseHeaders();
            if (resHeaderMap != null && !resHeaderMap.isEmpty()) {
                responseHeaders = new LinkedHashMap<>(resHeaderMap);
            }
        }


        if(mount.getValueProvider().hasProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX)) {
            hstLinkUrlPrefix = mount.getValueProvider().getString(GENERAL_PROPERTY_HST_LINK_URL_PREFIX);
            if (!VirtualHostService.validURLPrefix(hstLinkUrlPrefix)) {
                log.error("Ignoring invalid property '{}={}' on Mount '{}'. Supported formats are " +
                        "http://hostname, https://hostname or //hostname (hostname can include portnumber). " +
                        "After the hostname a path info is allowed which is not allowed to end with a '/'. " +
                        "Querystring or fragment is not allowed", GENERAL_PROPERTY_HST_LINK_URL_PREFIX, hstLinkUrlPrefix,
                        mount.getValueProvider().getPath());
                hstLinkUrlPrefix = null;
            }
        }

        if (hstLinkUrlPrefix == null) {
            if (parent != null) {
                hstLinkUrlPrefix = parent.getHstLinkUrlPrefix();
            } else {
                hstLinkUrlPrefix = virtualHost.getHstLinkUrlPrefix();
            }
        }

        else {
            // try to get the one from the parent
            if(parent != null) {
                this.locale = parent.getLocale();
            } else {
                this.locale = virtualHost.getLocale();
            }
        }

        try {
            if (mountPoint == null) {
                log.info("Mount '{}' at '{}' does have an empty mountPoint. This means the Mount is not using a HstSite and does not have a content path", getName(), mount.getValueProvider().getPath());
                ((VirtualHostsService) virtualHost.getVirtualHosts()).addMount(this);
            } else if (!mountPoint.startsWith("/")) {
                throw new ModelLoadingException("Mount at '" + mount.getValueProvider().getPath() + "' has an invalid mountPoint '" + mountPoint + "'. A mount point is absolute and must start with a '/'");
            } else if (!isMapped()) {
                log.info("Mount '{}' at '{}' does contain a mountpoint, but is configured to not use a HstSiteMap because isMapped() is false", getName(), mount.getValueProvider().getPath());

                // check if the mountpoint points to a hst:site node:
                if (mountPoint.startsWith(hstNodeLoadingCache.getRootPath())) {
                    HstNode hstSiteNodeForMount = hstNodeLoadingCache.getNode(mountPoint);
                    if (hstSiteNodeForMount != null && hstSiteNodeForMount.getNodeTypeName().equals(HstNodeTypes.NODETYPE_HST_SITE)) {
                        contentPath = hstSiteNodeForMount.getValueProvider().getString(HstNodeTypes.SITE_CONTENT);
                    } else {
                        contentPath = mountPoint;
                    }
                } else {
                    // when not mapped we normally do not need the mount for linkrewriting. Hence we just take it to be the same as the contentPath.
                    contentPath = mountPoint;
                }

                if (StringUtils.isNotEmpty(contentPath)) {
                    contentPath = "/"+ PathUtils.normalizePath(contentPath);
                }
                assertContentPathNotEmpty(mount, contentPath);
                // add this Mount to the maps in the VirtualHostsService
                ((VirtualHostsService) virtualHost.getVirtualHosts()).addMount(this);
            } else {

                if (!mountPoint.startsWith(hstNodeLoadingCache.getRootPath())) {
                    mountException(mount);
                }

                HstNode hstSiteNodeForMount = hstNodeLoadingCache.getNode(mountPoint);
                if (hstSiteNodeForMount == null) {
                    mountException(mount);
                }

                contentPath = hstSiteNodeForMount.getValueProvider().getString(HstNodeTypes.SITE_CONTENT);

                if (StringUtils.isNotEmpty(contentPath)) {
                    contentPath = "/"+ PathUtils.normalizePath(contentPath);
                }

                MountSiteMapConfiguration mountSiteMapConfiguration = new MountSiteMapConfiguration(this);
                long start = System.currentTimeMillis();
                if (Mount.PREVIEW_NAME.equals(type)) {
                    // explicit preview
                    previewHstSite = new HstSiteFactory(hstNodeLoadingCache, hstConfigurationLoadingCache).createPreviewSiteService(hstSiteNodeForMount, this, mountSiteMapConfiguration);
                    hstSite = previewHstSite;
                } else {
                    hstSite = new HstSiteFactory(hstNodeLoadingCache, hstConfigurationLoadingCache).createLiveSiteService(hstSiteNodeForMount, this, mountSiteMapConfiguration);
                    previewHstSite = new HstSiteFactory(hstNodeLoadingCache, hstConfigurationLoadingCache).createPreviewSiteService(hstSiteNodeForMount, this, mountSiteMapConfiguration);
                }

                assertContentPathNotEmpty(mount, contentPath);

                log.info("Successfull initialized hstSite '{}' for Mount '{}' in '{}' ms.",
                        new String[]{hstSite.getName(), getName(), String.valueOf(System.currentTimeMillis() - start)});
                // add this Mount to the maps in the VirtualHostsService
                ((VirtualHostsService) virtualHost.getVirtualHosts()).addMount(this);
            }
        } catch (ModelLoadingException e) {
            log.warn("Configured Mount '{}' is incorrect. Available child mounts will still be loaded.", jcrLocation, e);
        }

        for (HstNode childMount : mount.getNodes()) {
            if (HstNodeTypes.NODETYPE_HST_MOUNT.equals(childMount.getNodeTypeName())) {
                try {
                    MountService childMountService = new MountService(childMount, this, virtualHost, hstNodeLoadingCache, hstConfigurationLoadingCache, port);
                    childMountServices.put(childMountService.getName(), childMountService);

                } catch (ModelLoadingException e) {
                    String path = childMount.getValueProvider().getPath();
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping incorrect mount for mount node '" + path + "'. ", e);
                    } else {
                        log.warn("Skipping incorrect mount for mount node '{}' because of '{}'. ", path, e.toString());
                    }
                }

            }
        }

        pageModelApi = mount.getValueProvider().getString(GENERAL_PROPERTY_PAGE_MODEL_API);
        if (pageModelApi == null) {
            pageModelApi = ((VirtualHostService) virtualHost).getPageModelApi();
        }

        if (pageModelApi != null && isMapped() && !hasNoChannelInfo()) {
            if (childMountServices.containsKey(pageModelApi)) {
                log.info("Skipping automatic resource api for path '{}' below mount '{}' because it has an explicitly " +
                        "configured mount with the same path.", pageModelApi, this);
            } else if (pageModelApi.contains("/")) {
                log.error("Incorrect configured page model api value '{}'. / is not allowed in the path element",
                        pageModelApi);
            } else {
                try {
                    addMount(new PageModelApiMount(pageModelApi, this, hstNodeLoadingCache, hstConfigurationLoadingCache));
                } catch (Exception e) {
                    log.error("Cannot add PageModelApiMount for mount '{}'", this, e);
                }
            }
        }

    }

    private void assertContentPathNotEmpty(final HstNode mount, final String contentPath) throws ModelLoadingException {
        if (StringUtils.isEmpty(contentPath)) {
            throw new ModelLoadingException("Mount '"+mount.getValueProvider().getPath()+"' does have an empty or null contentPath, " +
                    "hence has broken configuration. Fix the hst:content property when the mountpoint points to a hst:site node, or make sure" +
                    " hst:ismapped = false if this mount does not need a mountpoint to a hst:site node. Available child mounts will still be loaded");
        }
    }

    private void mountException(final HstNode mount) throws ModelLoadingException {
        throw new ModelLoadingException("mountPoint '" + mountPoint
                + "' does not point to a hst:site node for Mount '" + mount.getValueProvider().getPath()
                + "'. Cannot create HstSite for Mount. Either fix the mountpoint or add 'hst:ismapped=false' " +
                "if this mount is not meant to have a mount point");
    }

    /**
     * @param siteMapItems
     * @return <code>true</code> if any of the <code>siteMapItems</code> or its descendents uses a different scheme than
     * the scheme of this {@link MountService}
     */
    private Boolean multipleSchemesUsed(final List<HstSiteMapItem> siteMapItems) {
        for (HstSiteMapItem siteMapItem : siteMapItems) {
            if (siteMapItem.isSchemeAgnostic()) {
                continue;
            }
            if (!scheme.equals(siteMapItem.getScheme())) {
                return Boolean.TRUE;
            }
            if (multipleSchemesUsed(siteMapItem.getChildren())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }


    @Override
    public void addMount(MutableMount mount) throws ModelLoadingException {
        if(childMountServices.containsKey(mount.getName())) {
            throw new ModelLoadingException("Cannot add Mount with name '"+mount.getName()+"' because already exists for " + this.toString());
        }
        childMountServices.put(mount.getName(), mount);
        ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(mount);
    }

    @Override
    public List<Mount> getChildMounts() {
        return Collections.unmodifiableList(new ArrayList<Mount>(childMountServices.values()));
    }

    public Mount getChildMount(String name) {
        return childMountServices.get(name);
    }

    public HstSite getHstSite() {
        return hstSite;
    }

    // not an API method. Only internal for the core
    public HstSite getPreviewHstSite() {
        return previewHstSite;
    }


    public String getName() {
        return name;
    }

    public String getIdentifier() {
        return uuid;
    }

    public String getAlias() {
        return alias;
    }

    public String getMountPath() {
        return mountPath;
    }

    public String getContentPath() {
        return contentPath;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    @Override
    public boolean hasNoChannelInfo() {
        return noChannelInfo;
    }

    public boolean isMapped() {
        return isMapped;
    }


    public Mount getParent() {
        return parent;
    }

    public String getScheme() {
        return scheme;
    }

    public boolean isSchemeAgnostic() {
        return schemeAgnostic;
    }

    @Override
    public String getHstLinkUrlPrefix() {
        return hstLinkUrlPrefix;
    }

    @Override
    public boolean containsMultipleSchemes() {
        if (containsMultipleSchemes != null) {
            return containsMultipleSchemes.booleanValue();
        }
        synchronized (this) {
            if (containsMultipleSchemes != null) {
                return containsMultipleSchemes.booleanValue();
            }
            if (hstSite == null) {
                containsMultipleSchemes = Boolean.FALSE;
                return false;
            }
            containsMultipleSchemes = multipleSchemesUsed(hstSite.getSiteMap().getSiteMapItems());
            return containsMultipleSchemes.booleanValue();
        }
    }

    public int getSchemeNotMatchingResponseCode() {
        return schemeNotMatchingResponseCode;
    }

    public String getLocale() {
        return locale;
    }

    public String getHomePage() {
        return homepage;
    }

    public String getPageNotFound() {
        return pageNotFound;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public int getPort() {
        return port;
    }

    public boolean isPortInUrl() {
        return showPort;
    }

    public String getContextPath() {
        return virtualHost.getContextPath();
    }

    public boolean isPreview() {
        return isOfType(Mount.PREVIEW_NAME);
    }

    public String getType() {
        return type;
    }

    public List<String> getTypes(){
        List<String> combined = new ArrayList<String>();
        // add the primary type  first
        combined.add(getType());

        if(types != null) {
            if(types.contains(getType())) {
                for(String extraType : types) {
                    if(extraType != null) {
                       if(extraType.equals(getType())) {
                           // already got it
                           continue;
                       }
                       combined.add(extraType);
                    }
                }
            } else {
                combined.addAll(types);
            }
        }
        return Collections.unmodifiableList(combined);
    }

    public boolean isOfType(String type) {
        return getTypes().contains(type);
    }


    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }

    public String getNamedPipeline(){
        return namedPipeline;
    }

    @Override
    public boolean isFinalPipeline() {
        return finalPipelines.contains(getNamedPipeline());
    }

    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return matcher;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }

    public Set<String> getUsers() {
        return Collections.unmodifiableSet(this.users);
    }

    public boolean isSubjectBasedSession() {
        return subjectBasedSession;
    }

    public boolean isSessionStateful() {
        return sessionStateful;
    }

    public String getFormLoginPage() {
        return formLoginPage;
    }

    public String getProperty(String name) {
        Object o = allProperties.get(name);
        if(o != null) {
            return o.toString();
        }
        return null;
    }

    @Override
    public List<String> getPropertyNames() {
        return ImmutableList.copyOf(allProperties.keySet());
    }

    @Override
    public String[] getDefaultSiteMapItemHandlerIds() {
        return defaultSiteMapItemHandlerIds;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public String [] getDefaultResourceBundleIds() {
        if (defaultResourceBundleIds == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return (String[]) ArrayUtils.clone(defaultResourceBundleIds);
    }

    public Map<String, String> getMountProperties() {
        Map<String, String> mountProperties = new HashMap<String, String>();
        for(Entry<String, Object> entry : allProperties.entrySet()) {
            if(entry.getValue() instanceof String) {
                if(entry.getKey().startsWith(PROPERTY_NAME_MOUNT_PREFIX)) {
                    if(entry.getKey().equals(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                        // skip the hst:mountpoint property as this is a reserved property with a different meaning
                        continue;
                    }
                    mountProperties.put(entry.getKey().substring(PROPERTY_NAME_MOUNT_PREFIX.length()).toLowerCase(), ((String)entry.getValue()).toLowerCase());
                }
            }
        }
        return mountProperties;
    }

    @Override
    public String getParameter(String name) {
        return this.parameters.get(name);
    }

    @Override
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    @Override
    public Channel getChannel() {
        HstSite hstSite = getHstSite();
        if (hstSite == null) {
            return null;
        }
        return hstSite.getChannel();
    }

    @Override
    public Channel getPreviewChannel() {
        HstSite hstSite = getPreviewHstSite();
        if (hstSite == null) {
            return null;
        }
        return hstSite.getChannel();
    }

    @SuppressWarnings("unchecked")
    public <T extends ChannelInfo> T getChannelInfo() {
        HstSite hstSite = getHstSite();
        if (hstSite == null) {
            return null;
        }
        return hstSite.getChannelInfo();
    }

    @Override
    public <T extends ChannelInfo> T getPreviewChannelInfo() {
        HstSite hstSite = getPreviewHstSite();
        if (hstSite == null) {
            return null;
        }
        return hstSite.getChannelInfo();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("MountService [jcrPath=");
        // use getVirtualHost() since in a decorated monut it can return a different host name
        builder.append(jcrLocation).append(", hostName=").append(getVirtualHost().getHostName()).append("]");
        return  builder.toString();
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        if (responseHeaders == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(responseHeaders);
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

    @Override
    public String getPageModelApi() {
        return pageModelApi;
    }
}
