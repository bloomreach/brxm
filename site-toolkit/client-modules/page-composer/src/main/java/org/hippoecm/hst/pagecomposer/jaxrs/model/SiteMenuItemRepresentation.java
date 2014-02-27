/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class SiteMenuItemRepresentation {

    private String id;

    // Property is called title instead of name because using
    // annotation JsonProperty("title") does not seem to work.
    private String title;

    private boolean repositoryBased;
    private Map<String, String> localParameters;
    private Set<String> roles;
    private SiteMenuItemLink siteMenuItemLink = new SiteMenuItemLink(null, null);

    // Property is called items instead of children because using
    // annotation JsonProperty("items") does not seem to work.
    private List<SiteMenuItemRepresentation> items = new ArrayList<>();

    public SiteMenuItemRepresentation() {
        super();
    }

    public SiteMenuItemRepresentation(HstSiteMenuItemConfiguration item)
            throws IllegalArgumentException {
        if (!(item instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected object of type CanonicalInfo");
        }

        title = item.getName();
        id = ((CanonicalInfo) item).getCanonicalIdentifier();
        repositoryBased = item.isRepositoryBased();
        localParameters = item.getLocalParameters();
        roles = item.getRoles();
        for (HstSiteMenuItemConfiguration childItem : item.getChildItemConfigurations()) {
            items.add(new SiteMenuItemRepresentation(childItem));
        }
        this.siteMenuItemLink = new SiteMenuItemLink(item);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public boolean isRepositoryBased() {
        return repositoryBased;
    }

    public void setRepositoryBased(final boolean repositoryBased) {
        this.repositoryBased = repositoryBased;
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

    public List<SiteMenuItemRepresentation> getItems() {
        return items;
    }

    public void setItems(final List<SiteMenuItemRepresentation> items) {
        this.items = items;
    }

    public LinkType getLinkType() {
        return siteMenuItemLink.getLinkType();
    }

    public void setLinkType(LinkType linkType) {
        this.siteMenuItemLink = new SiteMenuItemLink(linkType, siteMenuItemLink.getLink());
    }

    public String getLink() {
        return siteMenuItemLink.getLink();
    }

    public void setLink(String link) {
        this.siteMenuItemLink = new SiteMenuItemLink(siteMenuItemLink.getLinkType(), link);
    }

}
