/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model.hst;

import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;

/**
 * @version "$Id$"
 */
@PersistentNode(type = "hst:sitemenuitem")
public class HstSiteMenuItem extends BaseJcrModel {


    @PersistentProperty(name = "hst:referencesitemapitem")
    private String referenceSitemapItem;
    @PersistentProperty(name = "externallink")
    private String externalLink;
    @PersistentProperty(name = "hst:repobased")
    private Boolean repoBased;

    public HstSiteMenuItem(final String name, final String referenceSitemapItem) {
        setName(name);
        this.referenceSitemapItem = referenceSitemapItem;
    }

    public HstSiteMenuItem() {

    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(final String externalLink) {
        this.externalLink = externalLink;
    }

    public Boolean getRepoBased() {
        return repoBased;
    }

    public void setRepoBased(final Boolean repoBased) {
        this.repoBased = repoBased;
    }

    public String getReferenceSitemapItem() {
        return referenceSitemapItem;
    }

    public void setReferenceSitemapItem(final String referenceSitemapItem) {
        this.referenceSitemapItem = referenceSitemapItem;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HstSiteMenuItem{");
        sb.append("referenceSitemapItem='").append(referenceSitemapItem).append('\'');
        sb.append(", externalLink='").append(externalLink).append('\'');
        sb.append(", repoBased=").append(repoBased);
        sb.append('}');
        return sb.toString();
    }
}
