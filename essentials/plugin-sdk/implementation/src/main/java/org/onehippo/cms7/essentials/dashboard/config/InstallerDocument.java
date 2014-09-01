/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;


import java.util.Calendar;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * @version "$Id$"
 */

@XmlRootElement()
public class InstallerDocument extends BaseDocument {


    private String pluginId;

    // date when the plugin got installed in the dashboard
    private Calendar dateInstalled;

    // date when the plugin got added to the project
    private Calendar dateAdded;

    // plugin installation state
    private String installationState;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(final String pluginId) {
        this.pluginId = pluginId;
    }

    public Calendar getDateInstalled() {
        return dateInstalled;
    }

    public void setDateInstalled(final Calendar dateInstalled) {
        this.dateInstalled = dateInstalled;
    }

    public Calendar getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(final Calendar dateAdded) {
        this.dateAdded = dateAdded;
    }

    public boolean hasDateAdded() { return dateAdded != null; }

    public String getInstallationState() {
        return installationState;
    }

    public void setInstallationState(String installationState) {
        this.installationState = installationState;
    }

    public boolean hasInstallationState() {
        return installationState != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InstallerDocument{");
        sb.append("pluginId='").append(pluginId).append('\'');
        sb.append(", dateInstalled=").append(dateInstalled);
        sb.append('}');
        return sb.toString();
    }
}
