/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMapItemRepresentation {

    private static final Logger log = LoggerFactory.getLogger(SiteMapItemRepresentation.class);

    private String name;
    private String id;
    private String parentId;
    private String pathInfo;
    private String parentPathInfo;
    private boolean isHomePage;
    private String pageTitle;
    private Map<String, String> localParameters;
    private Set<String> roles;
    private String componentConfigurationId;
    private boolean cacheable;
    private boolean workspaceConfiguration;
    private boolean inherited;
    private String lockedBy;
    private Calendar lockedOn;
    private long versionStamp;
    private String relativeContentPath;
    private DocumentRepresentation primaryDocumentRepresentation;
    private Set<DocumentRepresentation> availableDocumentRepresentations;
    private String scheme;
    private boolean wildCard;
    private boolean any;
    private boolean containsWildCard;
    private boolean containsAny;
    private boolean isExplicitElement;
    private Location parentLocation;

    // whether the page has at least one container item in its page definition.
    // Note that although the backing {@link HstComponentConfiguration} might have containers,
    // this does not mean the component has it in its page definition: The page definition
    // is the canonical configuration, without inheritance (and thus the container might be present in inherited config only)
    private boolean hasContainerItemInPageDefinition;

    private int numberOfChildren;

    public SiteMapItemRepresentation represent(final HstSiteMapItem item,
                                               final Mount mount,
                                               final DocumentRepresentation primaryDocumentRepresentation,
                                               final Set<DocumentRepresentation> availableDocumentRepresentations) {

        if (!(item instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected object of type CanonicalInfo");
        }
        if (!(item instanceof ConfigurationLockInfo)) {
            throw new IllegalArgumentException("Expected object of type ConfigurationLockInfo");
        }
        name = NodeNameCodec.decode(item.getValue());
        id = ((CanonicalInfo) item).getCanonicalIdentifier();

        this.pathInfo = HstSiteMapUtils.getPath(item, null);
        final String homePagePathInfo = HstSiteMapUtils.getPath(mount, mount.getHomePage());
        if (this.pathInfo.equals(homePagePathInfo)) {
            this.pathInfo = "/";
            this.isHomePage = true;
        }

        if (item.getParentItem() != null) {
            parentId = ((CanonicalInfo) item.getParentItem()).getCanonicalIdentifier();
            parentPathInfo = HstSiteMapUtils.getPath(item.getParentItem(), null);
        }

        pageTitle = item.getPageTitle();
        componentConfigurationId = item.getComponentConfigurationId();
        final HstComponentConfiguration page = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration(componentConfigurationId);
        hasContainerItemInPageDefinition = hasContainerItemInPageDefinition(page);

        this.primaryDocumentRepresentation = primaryDocumentRepresentation;
        this.availableDocumentRepresentations = availableDocumentRepresentations;

        cacheable = item.isCacheable();
        workspaceConfiguration = ((CanonicalInfo) item).isWorkspaceConfiguration();
        inherited = !((CanonicalInfo) item).getCanonicalPath().startsWith(mount.getHstSite().getConfigurationPath() + "/");
        lockedBy = ((ConfigurationLockInfo) item).getLockedBy();
        lockedOn = ((ConfigurationLockInfo) item).getLockedOn();
        if (lockedOn != null) {
            versionStamp = lockedOn.getTimeInMillis();
        }
        relativeContentPath = item.getRelativeContentPath();
        scheme = item.getScheme();
        wildCard = item.isWildCard();
        any = item.isAny();
        containsWildCard = item.containsWildCard();
        containsAny = item.containsAny();
        isExplicitElement = item.isExplicitElement();
        localParameters = item.getLocalParameters();
        roles = item.getRoles();

        parentLocation = findParentLocation(mount, item);

        numberOfChildren = item.getChildren().size();

        return this;
    }

    Location findParentLocation(final Mount mount, final HstSiteMapItem item) {
        String prefix = mount.getVirtualHost().getHostName();
        if (StringUtils.isNotEmpty(mount.getMountPath())) {
            prefix += mount.getMountPath();
        }
        if (item.getParentItem() == null) {
            return new Location(prefix + "/", null);
        } else {
            pathInfo = HstSiteMapUtils.getPath(item.getParentItem(), null);
            if (pathInfo.equals(HstSiteMapUtils.getPath(mount, mount.getHomePage()))) {
                pathInfo = "/";
            } else if (!pathInfo.startsWith("/")) {
                pathInfo = "/" + pathInfo;
            }
            return new Location(prefix + pathInfo + "/", ((CanonicalInfo) item.getParentItem()).getCanonicalIdentifier());
        }
    }

    private boolean hasContainerItemInPageDefinition(final HstComponentConfiguration root) {
        if (root == null) {
            return false;
        }
        return hasContainerItemInPageDefinition(root, root.getCanonicalStoredLocation());
    }

    private boolean hasContainerItemInPageDefinition(final HstComponentConfiguration config, final String pageDefinitionRootPath) {
        if (HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.equals(config.getComponentType())
                && config.getCanonicalStoredLocation().startsWith(pageDefinitionRootPath)) {
            return true;
        }
        for (HstComponentConfiguration child : config.getChildren().values()) {
            if (hasContainerItemInPageDefinition(child, pageDefinitionRootPath)) {
                return true;
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean getIsHomePage() {
        return isHomePage;
    }

    public void setIsHomePage(final boolean homePage) {
        isHomePage = homePage;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getParentPathInfo() {
        return parentPathInfo;
    }

    public void setParentPathInfo(final String parentPathInfo) {
        this.parentPathInfo = parentPathInfo;
    }

    public String getComponentConfigurationId() {
        return componentConfigurationId;
    }

    public void setComponentConfigurationId(final String componentConfigurationId) {
        this.componentConfigurationId = componentConfigurationId;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(final String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public void setCacheable(final boolean cacheable) {
        this.cacheable = cacheable;
    }

    public boolean isWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    public void setWorkspaceConfiguration(final boolean workspaceConfiguration) {
        this.workspaceConfiguration = workspaceConfiguration;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(final boolean inherited) {
        this.inherited = inherited;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(final String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public Calendar getLockedOn() {
        return lockedOn;
    }

    public void setLockedOn(final Calendar lockedOn) {
        this.lockedOn = lockedOn;
    }

    public long getVersionStamp() {
        return versionStamp;
    }

    public void setVersionStamp(final long versionStamp) {
        this.versionStamp = versionStamp;
    }

    public String getRelativeContentPath() {
        return relativeContentPath;
    }

    public void setRelativeContentPath(final String relativeContentPath) {
        this.relativeContentPath = relativeContentPath;
    }

    public DocumentRepresentation getPrimaryDocumentRepresentation() {
        return primaryDocumentRepresentation;
    }

    public void setPrimaryDocumentRepresentation(final DocumentRepresentation primaryDocumentRepresentation) {
        this.primaryDocumentRepresentation = primaryDocumentRepresentation;
    }

    public Set<DocumentRepresentation> getAvailableDocumentRepresentations() {
        return availableDocumentRepresentations;
    }

    public void setAvailableDocumentRepresentations(final Set<DocumentRepresentation> availableDocumentRepresentations) {
        this.availableDocumentRepresentations = availableDocumentRepresentations;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public boolean isWildCard() {
        return wildCard;
    }

    public void setWildCard(final boolean wildCard) {
        this.wildCard = wildCard;
    }

    public boolean isAny() {
        return any;
    }

    public void setAny(final boolean any) {
        this.any = any;
    }

    public boolean isContainsWildCard() {
        return containsWildCard;
    }

    public void setContainsWildCard(final boolean containsWildCard) {
        this.containsWildCard = containsWildCard;
    }

    public boolean isContainsAny() {
        return containsAny;
    }

    public void setContainsAny(final boolean containsAny) {
        this.containsAny = containsAny;
    }

    public boolean isExplicitElement() {
        return isExplicitElement;
    }

    public void setExplicitElement(final boolean explicitElement) {
        isExplicitElement = explicitElement;
    }

    public Map<String, String> getLocalParameters() {
        return localParameters;
    }

    public void setLocalParameters(final Map<String, String> localParameters) {
        this.localParameters = localParameters;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(final Set<String> roles) {
        this.roles = roles;
    }

    public boolean getHasContainerItemInPageDefinition() {
        return hasContainerItemInPageDefinition;
    }

    public void setHasContainerItemInPageDefinition(final boolean hasContainerItemInPageDefinition) {
        this.hasContainerItemInPageDefinition = hasContainerItemInPageDefinition;
    }

    public Location getParentLocation() {
        return parentLocation;
    }

    public void setParentLocation(final Location parentLocation) {
        this.parentLocation = parentLocation;
    }

    public void setNumberOfChildren(final int numberOfChildren) {
        this.numberOfChildren = numberOfChildren;
    }

    public int getNumberOfChildren() {
        return numberOfChildren;
    }
}
