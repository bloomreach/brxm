/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
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

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.internal.InternalHstSiteMap;
import org.hippoecm.hst.configuration.internal.InternalHstSiteMapItem;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.util.DuplicateKeyNotAllowedHashMap;

public class RuntimeHstSiteMap extends GenericHstSiteMapWrapper {

    private final HstSiteMap delegatee;
    private final HstSite hstSite;
    private final Map<String, HstSiteMapItem> childrenSiteMapItems = new LinkedHashMap<>();
    private final Map<String, HstSiteMapItem> childrenSiteMapDescendants = new DuplicateKeyNotAllowedHashMap<>();
    private final Map<String, HstSiteMapItem> childrenSiteMapDescendantsByRefId = new DuplicateKeyNotAllowedHashMap<>();
    private final List<HstSiteMapItem> siteMapItems;

    public RuntimeHstSiteMap(final InternalHstSiteMap delegatee, final RuntimeHstSite hstSite) {
        super(delegatee);
        this.delegatee = delegatee;
        this.hstSite = hstSite;

        delegatee.getSiteMapItems().forEach(child -> {
            RuntimeHstSiteMapItem runtimeHstSiteMapItem = new RuntimeHstSiteMapItem((InternalHstSiteMapItem) child,
                    RuntimeHstSiteMap.this, null);
            childrenSiteMapItems.put(runtimeHstSiteMapItem.getValue(), runtimeHstSiteMapItem);
            childrenSiteMapDescendants.put(runtimeHstSiteMapItem.getId(), runtimeHstSiteMapItem);
            if (runtimeHstSiteMapItem.getRefId() != null) {
                childrenSiteMapDescendantsByRefId.put(runtimeHstSiteMapItem.getRefId(), runtimeHstSiteMapItem);
            }
        });

        siteMapItems = unmodifiableList(new ArrayList<>(childrenSiteMapItems.values()));
    }

    @Override
    public HstSite getSite() {
        return hstSite;
    }

    @Override
    public List<HstSiteMapItem> getSiteMapItems() {
        return siteMapItems;
    }

    @Override
    public HstSiteMapItem getSiteMapItem(String value) {
        return childrenSiteMapItems.get(value);
    }

    @Override
    public HstSiteMapItem getSiteMapItemById(String id) {
        return childrenSiteMapDescendants.get(id);
    }

    @Override
    public HstSiteMapItem getSiteMapItemByRefId(String refId) {
        return childrenSiteMapDescendantsByRefId.get(refId);
    }

    @Override
    public String toString() {
        return "RuntimeHstSiteMap{" + "delegatee=" + delegatee + '}';
    }

}
