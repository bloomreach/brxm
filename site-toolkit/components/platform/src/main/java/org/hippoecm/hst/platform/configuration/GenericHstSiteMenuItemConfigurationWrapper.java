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
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class GenericHstSiteMenuItemConfigurationWrapper implements HstSiteMenuItemConfiguration {

    private final HstSiteMenuItemConfiguration delegatee;

    public GenericHstSiteMenuItemConfigurationWrapper(final HstSiteMenuItemConfiguration delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public String getSiteMapItemPath() {
        return delegatee.getSiteMapItemPath();
    }

    @Override
    public String getExternalLink() {
        return delegatee.getExternalLink();
    }

    @Override
    public HstSiteMenuConfiguration getHstSiteMenuConfiguration() {
        return delegatee.getHstSiteMenuConfiguration();
    }

    @Override
    public List<HstSiteMenuItemConfiguration> getChildItemConfigurations() {
        return delegatee.getChildItemConfigurations();
    }

    @Override
    public HstSiteMenuItemConfiguration getParentItemConfiguration() {
        return delegatee.getParentItemConfiguration();
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegatee.getProperties();
    }

    @Override
    public boolean isRepositoryBased() {
        return delegatee.isRepositoryBased();
    }

    @Override
    public int getDepth() {
        return delegatee.getDepth();
    }

    @Override
    public String getParameter(String name) {
        return delegatee.getParameter(name);
    }

    @Override
    public String getLocalParameter(String name) {
        return delegatee.getLocalParameter(name);
    }

    @Override
    public Map<String, String> getParameters() {
        return delegatee.getParameters();
    }

    @Override
    public Map<String, String> getLocalParameters() {
        return delegatee.getLocalParameters();
    }

    @Override
    public String getMountAlias() {
        return delegatee.getMountAlias();
    }

    @Override
    public Set<String> getRoles() {
        return delegatee.getRoles();
    }

    @Override
    public String toString() {
        return "GenericHstSiteMenuItemConfigurationWrapper{" + "delegatee=" + delegatee + '}';
    }
}
