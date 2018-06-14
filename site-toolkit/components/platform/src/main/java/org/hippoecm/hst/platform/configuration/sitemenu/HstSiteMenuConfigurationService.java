/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.sitemenu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.hippoecm.hst.core.internal.StringPool;

import static org.hippoecm.hst.configuration.ConfigurationUtils.isWorkspaceConfig;

public class HstSiteMenuConfigurationService implements HstSiteMenuConfiguration, CanonicalInfo, ConfigurationLockInfo {

    private String name;
    private final String canonicalIdentifier;
    private final String canonicalPath;
    private final boolean workspaceConfiguration;
    private HstSiteMenusConfiguration hstSiteMenusConfiguration;
    private List<HstSiteMenuItemConfiguration> siteMenuItems = new ArrayList<HstSiteMenuItemConfiguration>();
    private HstSiteMenuItemConfiguration prototypeItem;

    private String lockedBy;
    private Calendar lockedOn;

    public HstSiteMenuConfigurationService(HstSiteMenusConfiguration hstSiteMenusConfiguration, HstNode siteMenu) {
        this.hstSiteMenusConfiguration = hstSiteMenusConfiguration;
        this.name = StringPool.get(siteMenu.getValueProvider().getName());
        this.canonicalIdentifier = siteMenu.getValueProvider().getIdentifier();
        this.canonicalPath =  siteMenu.getValueProvider().getPath();
        this.workspaceConfiguration = isWorkspaceConfig(siteMenu);
        for (HstNode siteMenuItem : siteMenu.getNodes()) {
            HstSiteMenuItemConfiguration siteMenuItemConfiguration = new HstSiteMenuItemConfigurationService(siteMenuItem, null, this);
            if (HstNodeTypes.SITEMENUITEM_HST_PROTOTYPEITEM.equals(siteMenuItem.getName())) {
                prototypeItem = siteMenuItemConfiguration;
            } else {
                siteMenuItems.add(siteMenuItemConfiguration);
            }
        }
        this.lockedBy = siteMenu.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
        this.lockedOn = siteMenu.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON);
    }


    public String getName() {
        return this.name;
    }

    @Override
    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    @Override
    public String getCanonicalPath() {
        return canonicalPath;
    }

    @Override
    public boolean isWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    public List<HstSiteMenuItemConfiguration> getSiteMenuConfigurationItems() {
        return Collections.unmodifiableList(siteMenuItems);
    }

    public HstSiteMenusConfiguration getSiteMenusConfiguration() {
        return hstSiteMenusConfiguration;
    }

    @Override
    public String getLockedBy() {
        return lockedBy;
    }

    @Override
    public Calendar getLockedOn() {
        return lockedOn;
    }

    /**
     * @return The site menu item with name {@link HstNodeTypes#SITEMENUITEM_HST_PROTOTYPEITEM}
     * or null if it does not exist
     */
    public HstSiteMenuItemConfiguration getPrototypeItem() {
        return prototypeItem;
    }
}
