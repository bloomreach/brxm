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

package org.onehippo.cms7.essentials.rest.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "system")
public class SystemInfo implements Restful {

    private static final long serialVersionUID = 1L;
    private int totalPlugins;
    private int totalTools;
    private int installedFeatures;
    private int configurablePlugins;
    private boolean needsRebuild;
    private boolean initialized;
    private List<Plugin> rebuildPlugins;

    public void incrementPlugins() {
        totalPlugins++;
    }

    public void incrementTools() {
        totalTools++;
    }

    public void incrementConfigurablePlugins() {
        configurablePlugins++;
    }

    public void incrementInstalledFeatures() {
        installedFeatures++;
    }

    public int getConfigurablePlugins() {
        return configurablePlugins;
    }

    public void setConfigurablePlugins(final int configurablePlugins) {
        this.configurablePlugins = configurablePlugins;
    }

    public int getTotalPlugins() {
        return totalPlugins;
    }


    public void setTotalPlugins(final int totalPlugins) {
        this.totalPlugins = totalPlugins;
    }


    public int getTotalTools() {
        return totalTools;
    }

    public void setTotalTools(final int totalTools) {
        this.totalTools = totalTools;
    }

    public int getInstalledFeatures() {
        return installedFeatures;
    }

    public void setInstalledFeatures(final int installedFeatures) {
        this.installedFeatures = installedFeatures;
    }

    public boolean isNeedsRebuild() {
        return needsRebuild;
    }

    public void setNeedsRebuild(final boolean needsRebuild) {
        this.needsRebuild = needsRebuild;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    public void addRebuildPlugin(final Plugin plugin) {
        getRebuildPlugins();
        rebuildPlugins.add(plugin);

    }

    public List<Plugin> getRebuildPlugins() {
        if (rebuildPlugins == null) {
            rebuildPlugins = new ArrayList<>();
        }
        return rebuildPlugins;
    }

    public void setRebuildPlugins(final List<Plugin> rebuildPlugins) {
        this.rebuildPlugins = rebuildPlugins;
    }
}
