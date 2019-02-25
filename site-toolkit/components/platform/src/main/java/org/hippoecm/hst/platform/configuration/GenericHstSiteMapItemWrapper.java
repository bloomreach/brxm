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
package org.hippoecm.hst.platform.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.internal.InternalHstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;

public class GenericHstSiteMapItemWrapper implements InternalHstSiteMapItem {

    private final InternalHstSiteMapItem delegatee;

    public GenericHstSiteMapItemWrapper(final InternalHstSiteMapItem delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public String getId() {
        return delegatee.getId();
    }

    @Override
    public String getRefId() {
        return delegatee.getRefId();
    }

    @Override
    public String getApplicationId() {
        return delegatee.getApplicationId();
    }

    @Override
    public String getQualifiedId() {
        return delegatee.getQualifiedId();
    }

    @Override
    public String getValue() {
        return delegatee.getValue();
    }

    @Override
    public String getPageTitle() {
        return delegatee.getPageTitle();
    }

    @Override
    public boolean isWildCard() {
        return delegatee.isWildCard();
    }

    @Override
    public boolean containsWildCard() {
        return delegatee.containsWildCard();
    }

    @Override
    public boolean isAny() {
        return delegatee.isAny();
    }

    @Override
    public boolean containsAny() {
        return delegatee.containsAny();
    }

    @Override
    public boolean isExplicitElement() {
        return delegatee.isExplicitElement();
    }

    @Override
    public boolean isExplicitPath() {
        return delegatee.isExplicitPath();
    }

    @Override
    public String getRelativeContentPath() {
        return delegatee.getRelativeContentPath();
    }

    @Override
    public String getComponentConfigurationId() {
        return delegatee.getComponentConfigurationId();
    }

    @Override
    public Map<String, String> getComponentConfigurationIdMappings() {
        return delegatee.getComponentConfigurationIdMappings();
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
    public boolean isExcludedForLinkRewriting() {
        return delegatee.isExcludedForLinkRewriting();
    }

    @Override
    public List<HstSiteMapItem> getChildren() {
        return delegatee.getChildren();
    }

    @Override
    public HstSiteMapItem getChild(String value) {
        return delegatee.getChild(value);
    }

    @Override
    public String getParameter(String name) {
        return delegatee.getParameter(name);
    }

    @Override
    public String getLocalParameter(String name) {
        return delegatee.getLocalParameter(name);
    }

    @Override
    public Map<String, String> getParameters() {
        return delegatee.getParameters();
    }

    @Override
    public Map<String, String> getLocalParameters() {
        return delegatee.getLocalParameters();
    }

    @Override
    public HstSiteMapItem getParentItem() {
        return delegatee.getParentItem();
    }

    @Override
    public HstSiteMap getHstSiteMap() {
        return delegatee.getHstSiteMap();
    }

    @Override
    public int getStatusCode() {
        return delegatee.getStatusCode();
    }

    @Override
    public int getErrorCode() {
        return delegatee.getErrorCode();
    }

    @Override
    public String getNamedPipeline() {
        return delegatee.getNamedPipeline();
    }

    @Override
    public String getLocale() {
        return delegatee.getLocale();
    }

    @Override
    public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String handlerId) {
        return delegatee.getSiteMapItemHandlerConfiguration(handlerId);
    }

    @Override
    public List<HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
        return delegatee.getSiteMapItemHandlerConfigurations();
    }

    @Override
    public boolean isCacheable() {
        return delegatee.isCacheable();
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
    public int getSchemeNotMatchingResponseCode() {
        return delegatee.getSchemeNotMatchingResponseCode();
    }

    @Override
    public String[] getResourceBundleIds() {
        return delegatee.getResourceBundleIds();
    }

    @Override
    public boolean isContainerResource() {
        return delegatee.isContainerResource();
    }

    @Override
    public boolean isHiddenInChannelManager() {
        return delegatee.isHiddenInChannelManager();
    }

    @Override
    public boolean isMarkedDeleted() {
        return delegatee.isMarkedDeleted();
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return delegatee.getResponseHeaders();
    }

    @Override
    public List<HstSiteMapItemHandler> getHstSiteMapItemHandlers() {
        return delegatee.getHstSiteMapItemHandlers();
    }

    @Override
    public InternalHstSiteMapItem getWildCardPatternChild(String value, List<InternalHstSiteMapItem> excludeList) {
        return delegatee.getWildCardPatternChild(value, excludeList);
    }

    @Override
    public InternalHstSiteMapItem getAnyPatternChild(String[] elements, int position,
            List<InternalHstSiteMapItem> excludeList) {
        return delegatee.getAnyPatternChild(elements, position, excludeList);
    }

    @Override
    public boolean patternMatch(String value, String prefix, String postfix) {
        return delegatee.patternMatch(value, prefix, postfix);
    }

    @Override
    public String getWildCardPrefix() {
        return delegatee.getWildCardPrefix();
    }

    @Override
    public String getWildCardPostfix() {
        return delegatee.getWildCardPostfix();
    }

    @Override
    public String getCanonicalIdentifier() {
        return delegatee.getCanonicalIdentifier();
    }

    @Override
    public String getCanonicalPath() {
        return delegatee.getCanonicalPath();
    }

    @Override
    public boolean isWorkspaceConfiguration() {
        return delegatee.isWorkspaceConfiguration();
    }

    @Override
    public String toString() {
        return "GenericHstSiteMapItemWrapper{" + "delegatee=" + delegatee + '}';
    }
}
