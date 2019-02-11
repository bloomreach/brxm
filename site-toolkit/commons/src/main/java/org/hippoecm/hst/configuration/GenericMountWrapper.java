/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.onehippo.cms7.services.hst.Channel;

public class GenericMountWrapper implements Mount {

    private final Mount delegatee;

    public GenericMountWrapper(final Mount delegatee) {

        this.delegatee = delegatee;
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public String getAlias() {
        return delegatee.getAlias();
    }

    @Override
    public boolean isMapped() {
        return delegatee.isMapped();
    }

    @Override
    public Mount getParent() {
        return delegatee.getParent();
    }

    @Override
    public String getMountPoint() {
        return delegatee.getMountPoint();
    }

    @Override
    public boolean hasNoChannelInfo() {
        return delegatee.hasNoChannelInfo();
    }

    @Override
    public String getContentPath() {
        return delegatee.getContentPath();
    }

    @Override
    public String getMountPath() {
        return delegatee.getMountPath();
    }

    @Override
    public List<Mount> getChildMounts() {
        return delegatee.getChildMounts();
    }

    @Override
    public Mount getChildMount(final String name) {
        return delegatee.getChildMount(name);
    }

    @Override
    public VirtualHost getVirtualHost() {
        return delegatee.getVirtualHost();
    }

    @Override
    public HstSite getHstSite() {
        return delegatee.getHstSite();
    }

    @Override
    public boolean isContextPathInUrl() {
        return delegatee.isContextPathInUrl();
    }

    @Override
    public boolean isPortInUrl() {
        return delegatee.isPortInUrl();
    }

    @Override
    public int getPort() {
        return delegatee.getPort();
    }

    @Override
    public String getContextPath() {
        return delegatee.getContextPath();
    }

    @Override
    public String getHomePage() {
        return delegatee.getHomePage();
    }

    @Override
    public String getPageNotFound() {
        return delegatee.getPageNotFound();
    }

    @Override
    public String getScheme() {
        return delegatee.getScheme();
    }

    @Override
    public boolean isSchemeAgnostic() {
        return delegatee.isSchemeAgnostic();
    }

    @Override
    public boolean containsMultipleSchemes() {
        return delegatee.containsMultipleSchemes();
    }

    @Override
    public int getSchemeNotMatchingResponseCode() {
        return delegatee.getSchemeNotMatchingResponseCode();
    }

    @Override
    public boolean isPreview() {
        return delegatee.isPreview();
    }

    @Override
    public boolean isOfType(final String type) {
        return delegatee.isOfType(type);
    }

    @Override
    public String getType() {
        return delegatee.getType();
    }

    @Override
    public List<String> getTypes() {
        return delegatee.getTypes();
    }

    @Override
    public boolean isVersionInPreviewHeader() {
        return delegatee.isVersionInPreviewHeader();
    }

    @Override
    public String getNamedPipeline() {
        return delegatee.getNamedPipeline();
    }

    @Override
    public boolean isFinalPipeline() {
        return delegatee.isFinalPipeline();
    }

    @Override
    public String getLocale() {
        return delegatee.getLocale();
    }

    @Override
    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return delegatee.getHstSiteMapMatcher();
    }

    @Override
    public boolean isAuthenticated() {
        return delegatee.isAuthenticated();
    }

    @Override
    public Set<String> getRoles() {
        return delegatee.getRoles();
    }

    @Override
    public Set<String> getUsers() {
        return delegatee.getUsers();
    }

    @Override
    public boolean isSubjectBasedSession() {
        return delegatee.isSubjectBasedSession();
    }

    @Override
    public boolean isSessionStateful() {
        return delegatee.isSessionStateful();
    }

    @Override
    public String getFormLoginPage() {
        return delegatee.getFormLoginPage();
    }

    @Override
    public String getProperty(final String name) {
        return delegatee.getProperty(name);
    }

    @Override
    public List<String> getPropertyNames() {
        return delegatee.getPropertyNames();
    }

    @Override
    public Map<String, String> getMountProperties() {
        return delegatee.getMountProperties();
    }

    @Override
    public String getParameter(final String name) {
        return delegatee.getParameter(name);
    }

    @Override
    public Map<String, String> getParameters() {
        return delegatee.getParameters();
    }

    @Override
    public String getIdentifier() {
        return delegatee.getIdentifier();
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo() {
        return delegatee.getChannelInfo();
    }

    @Override
    public Channel getChannel() {
        return delegatee.getChannel();
    }

    @Override
    public String[] getDefaultSiteMapItemHandlerIds() {
        return delegatee.getDefaultSiteMapItemHandlerIds();
    }

    @Override
    public boolean isCacheable() {
        return delegatee.isCacheable();
    }

    @Override
    public String[] getDefaultResourceBundleIds() {
        return delegatee.getDefaultResourceBundleIds();
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return delegatee.getResponseHeaders();
    }

    @Override
    public boolean isExplicit() {
        return delegatee.isExplicit();
    }

    @Override
    public String toString() {
        return "GenericMountWrapper{" +
                "delegatee=" + delegatee +
                '}';
    }
}
