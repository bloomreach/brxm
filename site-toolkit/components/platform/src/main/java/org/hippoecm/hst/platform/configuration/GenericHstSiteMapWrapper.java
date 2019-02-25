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

import org.hippoecm.hst.configuration.internal.InternalHstSiteMap;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public class GenericHstSiteMapWrapper implements InternalHstSiteMap {

    private final InternalHstSiteMap delegatee;

    public GenericHstSiteMapWrapper(final InternalHstSiteMap delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public HstSite getSite() {
        return delegatee.getSite();
    }

    @Override
    public List<HstSiteMapItem> getSiteMapItems() {
        return delegatee.getSiteMapItems();
    }

    @Override
    public HstSiteMapItem getSiteMapItem(String value) {
        return delegatee.getSiteMapItem(value);
    }

    @Override
    public HstSiteMapItem getSiteMapItemById(String id) {
        return delegatee.getSiteMapItemById(id);
    }

    @Override
    public HstSiteMapItem getSiteMapItemByRefId(String refId) {
        return delegatee.getSiteMapItemByRefId(refId);
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
        return "GenericHstSiteMapWrapper{" + "delegatee=" + delegatee + '}';
    }
}
