/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.platform.configuration.site.HstSiteFactory;
import org.hippoecm.hst.platform.configuration.site.MountSiteMapConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.onehippo.cms7.services.hst.Channel;

import static java.util.Collections.emptyList;

public class PageModelApiMount implements ContextualizableMount {

    private final String name;
    private final Mount parent;
    private final String id;
    private final HstSite hstSite;
    private final HstSite previewHstSite;

    public PageModelApiMount(final String name, final Mount parent, final HstNodeLoadingCache hstNodeLoadingCache,
                             final HstConfigurationLoadingCache hstConfigurationLoadingCache) {
        if (StringUtils.isEmpty(name) || name.contains("/")) {
            throw new ModelLoadingException(String.format("Name '%s' is illegal for page model api pipeline", name));
        }
        this.name = name;
        this.parent = parent;
        id = UUID.randomUUID().toString();

        MountSiteMapConfiguration mountSiteMapConfiguration = new MountSiteMapConfiguration(this);
        HstNode hstSiteNodeForMount = hstNodeLoadingCache.getNode(getMountPoint());

        if (Mount.PREVIEW_NAME.equals(parent.getType())) {
            previewHstSite = new HstSiteFactory(hstNodeLoadingCache, hstConfigurationLoadingCache).createPreviewSiteService(hstSiteNodeForMount, this, mountSiteMapConfiguration);
            hstSite = previewHstSite;
        } else {
            hstSite = new HstSiteFactory(hstNodeLoadingCache, hstConfigurationLoadingCache).createLiveSiteService(hstSiteNodeForMount, this, mountSiteMapConfiguration);
            previewHstSite = new HstSiteFactory(hstNodeLoadingCache, hstConfigurationLoadingCache).createPreviewSiteService(hstSiteNodeForMount, this, mountSiteMapConfiguration);

        }

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlias() {
        if (parent.getAlias() == null) {
            return null;
        }
        return parent.getAlias() + "/" + name;
    }

    @Override
    public boolean isMapped() {
        return parent.isMapped();
    }

    @Override
    public Mount getParent() {
        return parent;
    }

    @Override

    public String getMountPoint() {
        return parent.getMountPoint();
    }

    @Override
    public boolean hasNoChannelInfo() {
        return parent.hasNoChannelInfo();
    }

    @Override
    public String getContentPath() {
        return parent.getContentPath();
    }

    @Override
    public String getMountPath() {
        return parent.getMountPath() + "/" + getName();
    }

    @Override
    public List<Mount> getChildMounts() {
        return emptyList();
    }

    @Override
    public Mount getChildMount(final String name) {
        return null;
    }

    @Override
    public VirtualHost getVirtualHost() {
        return parent.getVirtualHost();
    }

    @Override
    public HstSite getHstSite() {
        return hstSite;
    }

    @Override
    public HstSite getPreviewHstSite() {
        return previewHstSite;
    }

    @Override
    public boolean isContextPathInUrl() {
        return parent.isContextPathInUrl();
    }

    @Override
    public boolean isPortInUrl() {
        return parent.isPortInUrl();
    }

    @Override
    public int getPort() {
        return parent.getPort();
    }

    @Override
    public String getContextPath() {
        return parent.getContextPath();
    }

    @Override
    public String getHomePage() {
        return parent.getHomePage();
    }

    @Override
    public String getPageNotFound() {
        return parent.getPageNotFound();
    }

    @Override
    public String getScheme() {
        return parent.getScheme();
    }

    @Override
    public String getHstLinkUrlPrefix() {
        return parent.getHstLinkUrlPrefix();
    }

    @Override
    public boolean isSchemeAgnostic() {
        return parent.isSchemeAgnostic();
    }

    @Override
    public boolean containsMultipleSchemes() {
        return parent.containsMultipleSchemes();
    }

    @Override
    public int getSchemeNotMatchingResponseCode() {
        return parent.getSchemeNotMatchingResponseCode();
    }

    @Override
    public boolean isPreview() {
        return parent.isPreview();
    }

    @Override
    public boolean isOfType(final String type) {
        return parent.isOfType(type);
    }

    @Override
    public String getType() {
        // is parent is preview, this page model api is also preview
        return parent.getType();
    }

    @Override
    public List<String> getTypes() {
        // next to parent types, we append type 'pagemodelapi' so linkrewriting can make smarter choices when doing
        // cross mount linking between two 'pagemodelapi's' if this would ever be needed
        final ArrayList<String> types = new ArrayList<>(parent.getTypes());
        types.add("pagemodelapi");
        return types;
    }

    @Override
    public boolean isVersionInPreviewHeader() {
        return parent.isVersionInPreviewHeader();
    }

    @Override
    public String getNamedPipeline() {
        return ContainerConstants.PAGE_MODEL_PIPELINE_NAME;
    }

    @Override
    public boolean isFinalPipeline() {
        // The ContainerConstants.PAGE_MODEL_PIPELINE_NAME must be final and not allowed to be reset on sitemap item level
        return true;
    }

    @Override
    public String getLocale() {
        return parent.getLocale();
    }

    @Override
    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return parent.getHstSiteMapMatcher();
    }

    @Override
    public boolean isAuthenticated() {
        return parent.isAuthenticated();
    }

    @Override
    public Set<String> getRoles() {
        return parent.getRoles();
    }

    @Override
    public Set<String> getUsers() {
        return parent.getUsers();
    }

    @Override
    public boolean isSubjectBasedSession() {
        return parent.isSubjectBasedSession();
    }

    @Override
    public boolean isSessionStateful() {
        return parent.isSessionStateful();
    }

    @Override
    public String getFormLoginPage() {
        // no login page for page model api pipeline needed: Responsibility of the parent mount
        return null;
    }

    @Override
    public String getProperty(final String name) {
        return parent.getProperty(name);
    }

    @Override
    public List<String> getPropertyNames() {
        return parent.getPropertyNames();
    }

    @Override
    public Map<String, String> getMountProperties() {
        return parent.getMountProperties();
    }

    @Override
    public String getParameter(final String name) {
        return parent.getParameter(name);
    }

    @Override
    public Map<String, String> getParameters() {
        return parent.getParameters();
    }

    @Override
    public String getIdentifier() {
        // random non-jcr id since never needed in channel mngr
        return id;
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo() {
        // no channel for page model api mount
        return hstSite.getChannelInfo();
    }

    @Override
    public Channel getChannel() {
        return hstSite.getChannel();
    }

    @Override
    public Channel getPreviewChannel() {
        return previewHstSite.getChannel();
    }

    @Override
    public <T extends ChannelInfo> T getPreviewChannelInfo() {
        return previewHstSite.getChannelInfo();
    }

    @Override
    public String[] getDefaultSiteMapItemHandlerIds() {
        return parent.getDefaultSiteMapItemHandlerIds();
    }

    @Override
    public boolean isCacheable() {
        return parent.isCacheable();
    }

    @Override
    public String[] getDefaultResourceBundleIds() {
        return parent.getDefaultResourceBundleIds();
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return parent.getResponseHeaders();
    }

    @Override
    public boolean isExplicit() {
        // auto-created mount so not explicit
        return false;
    }

    @Override
    public void addMount(final MutableMount mount) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not allowed to add mounts below the page model api pipeline");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PageModelApiMount [parent=").append(parent).append("]");
        return builder.toString();
    }

    @Override
    public String getPageModelApi() {
        return null;
    }
}
