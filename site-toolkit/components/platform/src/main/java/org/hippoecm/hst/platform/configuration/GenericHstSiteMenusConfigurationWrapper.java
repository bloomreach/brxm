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

import java.util.Map;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;

public class GenericHstSiteMenusConfigurationWrapper implements HstSiteMenusConfiguration {

    private final HstSiteMenusConfiguration delegatee;

    public GenericHstSiteMenusConfigurationWrapper(final HstSiteMenusConfiguration delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public HstSite getSite() {
        return delegatee.getSite();
    }

    @Override
    public Map<String, HstSiteMenuConfiguration> getSiteMenuConfigurations() {
        return delegatee.getSiteMenuConfigurations();
    }

    @Override
    public HstSiteMenuConfiguration getSiteMenuConfiguration(String name) {
        return delegatee.getSiteMenuConfiguration(name);
    }

    @Override
    public String toString() {
        return "GenericHstSiteMenusConfigurationWrapper{" + "delegatee=" + delegatee + '}';
    }
}
