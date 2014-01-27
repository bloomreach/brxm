/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SiteMenuHelperTest {

    private SiteMenuHelper siteMenuHelper;

    @Before
    public void setUp() {
        this.siteMenuHelper = new SiteMenuHelper();
    }

    @Test(expected = RepositoryException.class)
    public void testGetEditingPreviewHstSiteThrowsWhenSiteIsNull() throws RepositoryException {
        siteMenuHelper.getEditingPreviewHstSite(null);
    }

    @Test
    public void testGetMenuConfig() throws RepositoryException {
        final HstSite site = createMock(HstSite.class);
        final HstSiteMenusConfiguration menusConfig = createMock(HstSiteMenusConfiguration.class);
        final HstSiteMenuConfiguration menuConfig = createMock(HstSiteMenuConfiguration.class);

        expect(site.getSiteMenusConfiguration()).andReturn(menusConfig);

        final Map<String, HstSiteMenuConfiguration> map = new HashMap<>();
        expect(menusConfig.getSiteMenuConfigurations()).andReturn(map);

        final String menuId = "menuId";
        map.put(menuId, menuConfig);
        expect(menuConfig.getCanonicalIdentifier()).andReturn(menuId);

        replay(site, menusConfig, menuConfig);
        assertThat(siteMenuHelper.getMenuConfig(menuId, site), is(menuConfig));
    }

    @Test
    public void testGetMenuItemConfig() throws RepositoryException {
        final String itemId = "itemId";
        final HstSiteMenuConfiguration menuConfig = createMock(HstSiteMenuConfiguration.class);
        final HstSiteMenuItemConfiguration item = createMock(HstSiteMenuItemConfiguration.class);
        final List<HstSiteMenuItemConfiguration> items = Arrays.asList(item);
        expect(menuConfig.getSiteMenuConfigurationItems()).andReturn(items);
        expect(item.getCanonicalIdentifier()).andReturn(itemId);
        replay(menuConfig, item);
        assertThat(siteMenuHelper.getMenuItemConfig(itemId, menuConfig), is(item));
    }
}
