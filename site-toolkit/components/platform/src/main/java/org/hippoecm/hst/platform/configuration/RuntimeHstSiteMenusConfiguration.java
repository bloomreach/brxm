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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;

public class RuntimeHstSiteMenusConfiguration extends GenericHstSiteMenusConfigurationWrapper {

    private final HstSiteMenusConfiguration delegatee;
    private final Map<String, HstSiteMenuConfiguration> childrenSiteMenuConfigurations;
    private final HstSite hstSite;

    public RuntimeHstSiteMenusConfiguration(final HstSiteMenusConfiguration delegatee, final RuntimeHstSite hstSite) {
        super(delegatee);
        this.delegatee = delegatee;
        this.hstSite = hstSite;

        final Map<String, HstSiteMenuConfiguration> siteMenuConfigurations = new HashMap<>();
        delegatee.getSiteMenuConfigurations().entrySet().forEach(child -> {
            RuntimeHstSiteMenuConfiguration siteMenuConfiguration = new RuntimeHstSiteMenuConfiguration(
                    child.getValue(), RuntimeHstSiteMenusConfiguration.this);
            siteMenuConfigurations.put(siteMenuConfiguration.getName(), siteMenuConfiguration);
        });

        childrenSiteMenuConfigurations = Collections.unmodifiableMap(siteMenuConfigurations);
    }

    @Override
    public HstSite getSite() {
        return hstSite;
    }

    @Override
    public Map<String, HstSiteMenuConfiguration> getSiteMenuConfigurations() {
        return childrenSiteMenuConfigurations;
    }

    @Override
    public HstSiteMenuConfiguration getSiteMenuConfiguration(String name) {
        return childrenSiteMenuConfigurations.get(name);
    }

    @Override
    public String toString() {
        return "RuntimeHstSiteMenusConfiguration{" + "delegatee=" + delegatee + '}';
    }

}
