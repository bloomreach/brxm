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

public class RuntimeHstSiteMenuItemConfiguration extends GenericHstSiteMenuItemConfigurationWrapper {

    private final HstSiteMenuItemConfiguration delegatee;
    private final HstSiteMenuConfiguration hstSiteMenuConfiguration;
    private final HstSiteMenuItemConfiguration parentSiteMenuItemConfiguration;
    private final List<HstSiteMenuItemConfiguration> childrenSiteMenuItemConfigurations;

    public RuntimeHstSiteMenuItemConfiguration(final HstSiteMenuItemConfiguration delegatee,
            final RuntimeHstSiteMenuConfiguration hstSiteMenuConfiguration) {
        super(delegatee);
        this.delegatee = delegatee;
        this.hstSiteMenuConfiguration = hstSiteMenuConfiguration;

        if (delegatee.getParentItemConfiguration() != null) {
            parentSiteMenuItemConfiguration = new RuntimeHstSiteMenuItemConfiguration(
                    delegatee.getParentItemConfiguration(), hstSiteMenuConfiguration);
        } else {
            parentSiteMenuItemConfiguration = null;
        }

        final List<HstSiteMenuItemConfiguration> siteMenuItemConfigurations = new ArrayList<>();
        delegatee.getChildItemConfigurations().forEach(child -> {
            RuntimeHstSiteMenuItemConfiguration siteMenuItemConfiguration = new RuntimeHstSiteMenuItemConfiguration(
                    child, hstSiteMenuConfiguration);
            siteMenuItemConfigurations.add(siteMenuItemConfiguration);
        });

        childrenSiteMenuItemConfigurations = Collections.unmodifiableList(siteMenuItemConfigurations);
    }

    @Override
    public HstSiteMenuConfiguration getHstSiteMenuConfiguration() {
        return hstSiteMenuConfiguration;
    }

    @Override
    public List<HstSiteMenuItemConfiguration> getChildItemConfigurations() {
        return childrenSiteMenuItemConfigurations;
    }

    @Override
    public HstSiteMenuItemConfiguration getParentItemConfiguration() {
        return parentSiteMenuItemConfiguration;
    }

    @Override
    public String toString() {
        return "RuntimePortMount{" + "delegatee=" + delegatee + '}';
    }

}
