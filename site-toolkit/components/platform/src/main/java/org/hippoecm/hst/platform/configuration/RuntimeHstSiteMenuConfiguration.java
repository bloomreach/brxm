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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;

public class RuntimeHstSiteMenuConfiguration extends GenericHstSiteMenuConfigurationWrapper {

    private final HstSiteMenuConfiguration delegatee;
    private final HstSiteMenusConfiguration hstSiteMenusConfiguration;
    private final List<HstSiteMenuItemConfiguration> siteMenuItems;

    public RuntimeHstSiteMenuConfiguration(final HstSiteMenuConfiguration delegatee,
            final RuntimeHstSiteMenusConfiguration hstSiteMenusConfiguration) {
        super(delegatee);
        this.delegatee = delegatee;
        this.hstSiteMenusConfiguration = hstSiteMenusConfiguration;

        final List<HstSiteMenuItemConfiguration> childrenSiteMenuItems = new ArrayList<>();
        delegatee.getSiteMenuConfigurationItems().forEach(child -> {
            RuntimeHstSiteMenuItemConfiguration siteMenuConfigurationItem = new RuntimeHstSiteMenuItemConfiguration(
                    child, RuntimeHstSiteMenuConfiguration.this);
            childrenSiteMenuItems.add(siteMenuConfigurationItem);
        });

        siteMenuItems = Collections.unmodifiableList(childrenSiteMenuItems);
    }

    @Override
    public List<HstSiteMenuItemConfiguration> getSiteMenuConfigurationItems() {
        return siteMenuItems;
    }

    @Override
    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return hstSiteMenusConfiguration;
    }

    @Override
    public String toString() {
        return "RuntimeHstSiteMenuConfiguration{" + "delegatee=" + delegatee + '}';
    }

}
