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

package org.hippoecm.hst.configuration.sitemap;

import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.site.HstSite;

public class HstNoopSiteMap implements HstSiteMap {

    private final HstSite hstSite;

    public HstNoopSiteMap(final HstSite hstSite) {
        this.hstSite = hstSite;
    }

    @Override
    public HstSite getSite() {
        return hstSite;
    }

    @Override
    public List<HstSiteMapItem> getSiteMapItems() {
        return Collections.emptyList();
    }

    @Override
    public HstSiteMapItem getSiteMapItem(final String value) {
        return null;
    }

    @Override
    public HstSiteMapItem getSiteMapItemById(final String id) {
        return null;
    }

    @Override
    public HstSiteMapItem getSiteMapItemByRefId(final String refId) {
        return null;
    }
}
