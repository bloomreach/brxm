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

import java.util.Collection;
import java.util.Collections;

import javax.jcr.RepositoryException;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

import static com.google.common.base.Optional.fromNullable;

class SiteMenuHelper {

    public HstSite getEditingPreviewHstSite(HstSite editingPreviewHstSite) throws RepositoryException {
        if (editingPreviewHstSite == null) {
            throw new RepositoryException("Could not get the editing site to create the page model representation.");
        }
        return editingPreviewHstSite;
    }

    public HstSiteMenuConfiguration getMenuConfig(final String menuId, HstSite hstSite) throws RepositoryException {
        final Collection<HstSiteMenuConfiguration> configs = hstSite.getSiteMenusConfiguration().getSiteMenuConfigurations().values();
        final Optional<HstSiteMenuConfiguration> result = Iterables.tryFind(configs, new Predicate<HstSiteMenuConfiguration>() {
            @Override
            public boolean apply(HstSiteMenuConfiguration menuConfiguration) {
                return menuConfiguration.getCanonicalIdentifier().equals(menuId);
            }
        });
        if (!result.isPresent()) {
            throw new RepositoryException(String.format("Site menu with id '%s' is not part of currently edited preview site.", menuId));
        }
        return result.get();
    }

    public HstSiteMenuItemConfiguration getMenuItemConfig(final String itemId, HstSiteMenuConfiguration hstSiteMenuConfiguration) throws RepositoryException {
        final Collection<HstSiteMenuItemConfiguration> configs =
                fromNullable(hstSiteMenuConfiguration.getSiteMenuConfigurationItems())
                        .or(Collections.<HstSiteMenuItemConfiguration>emptyList());
        final Optional<HstSiteMenuItemConfiguration> result = Iterables.tryFind(configs, new Predicate<HstSiteMenuItemConfiguration>() {
            @Override
            public boolean apply(HstSiteMenuItemConfiguration itemConfiguration) {
                return itemConfiguration.getCanonicalIdentifier().equals(itemId);
            }
        });
        if (!result.isPresent()) {
            throw new RepositoryException(String.format("Site menu item with id '%s' is not part of currently edited preview site.", itemId));
        }
        return result.get();
    }

}
