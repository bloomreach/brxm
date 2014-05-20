/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.picker;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;

public class NodePickerControllerSettings implements IClusterable {
    private static final long serialVersionUID = 1L;


    //IPluginconfig keys
    private static final String SELECTION_SERVICE_KEY   = "wicket.model";
    private static final String FOLDER_SERVICE_KEY      = "model.folder";

    public static final String BASE_UUID                = "base.uuid";
    public static final String SELECTABLE_NODETYPES     = "nodetypes";

    public static final String LAST_VISITED_KEY         = "last.visited.key";
    public static final String LAST_VISITED_ENABLED     = "last.visited.enabled";
    public static final String LAST_VISITED_NODETYPES   = "last.visited.nodetypes";

    //Default values
    private static final String DEFAULT_CLUSTER = "cms-pickers/documents";
    private static final String DEFAULT_LAST_VISITED_KEY = "node-picker-controller";
    private static final boolean DEFAULT_LAST_VISITED_ENABLED = true;
    public static final String LANGUAGE_CONTEXT_AWARE = "language.context.aware";
    public static final boolean DEFAULT_LANGUAGE_CONTEXT_AWARE = true;

    private String clusterName;
    private IPluginConfig clusterOptions;

    private String selectionServiceKey;
    private String folderServiceKey;

    private String[] selectableNodeTypes;

    private String lastVisitedKey;
    private String[] lastVisitedNodeTypes;
    private boolean lastVisitedEnabled;

    private String defaultModelUUID;
    private boolean languageContextWare;

    public NodePickerControllerSettings() {
        selectionServiceKey = SELECTION_SERVICE_KEY;
        folderServiceKey = FOLDER_SERVICE_KEY;

        clusterName = DEFAULT_CLUSTER;
        lastVisitedKey = DEFAULT_LAST_VISITED_KEY;
        lastVisitedEnabled = DEFAULT_LAST_VISITED_ENABLED;
    }

    public static NodePickerControllerSettings fromPluginConfig(final IPluginConfig config) {
        NodePickerControllerSettings settings = new NodePickerControllerSettings();

        settings.setClusterName(config.getString("cluster.name", DEFAULT_CLUSTER));
        settings.setClusterOptions(new JavaPluginConfig(config.getPluginConfig("cluster.options")));

        // enforce a unique ID for this node picker
        settings.getClusterOptions().remove("wicket.id");

        settings.setSelectionServiceKey(
                config.getString("selection.service.key", SELECTION_SERVICE_KEY));
        settings.setFolderServiceKey(
                config.getString("folder.service.key", FOLDER_SERVICE_KEY));

        settings.setSelectableNodeTypes(config.getStringArray(SELECTABLE_NODETYPES));

        settings.setLastVisitedNodeTypes(config.getStringArray(LAST_VISITED_NODETYPES));
        settings.setLastVisitedKey(config.getString(LAST_VISITED_KEY, DEFAULT_LAST_VISITED_KEY));
        settings.setLastVisitedEnabled(config.getAsBoolean(LAST_VISITED_ENABLED, DEFAULT_LAST_VISITED_ENABLED));

        settings.setDefaultModelUUID(config.getString(BASE_UUID, null));
        settings.setLanguageContextAware(config.getAsBoolean(LANGUAGE_CONTEXT_AWARE, DEFAULT_LANGUAGE_CONTEXT_AWARE));
        return settings;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public IPluginConfig getClusterOptions() {
        return clusterOptions;
    }

    public void setClusterOptions(IPluginConfig clusterOptions) {
        this.clusterOptions = clusterOptions;
    }

    public String getSelectionServiceKey() {
        return selectionServiceKey;
    }

    public void setSelectionServiceKey(String selectionServiceKey) {
        this.selectionServiceKey = selectionServiceKey;
    }

    public String getFolderServiceKey() {
        return folderServiceKey;
    }

    public void setFolderServiceKey(String folderServiceKey) {
        this.folderServiceKey = folderServiceKey;
    }

    public boolean hasLastVisitedNodeTypes() {
        return lastVisitedNodeTypes != null && lastVisitedNodeTypes.length > 0;
    }

    public String[] getLastVisitedNodeTypes() {
        return lastVisitedNodeTypes;
    }

    public void setLastVisitedNodeTypes(String[] lastVisitedNodeTypes) {
        this.lastVisitedNodeTypes = lastVisitedNodeTypes;
    }

    public boolean hasSelectableNodeTypes() {
        return selectableNodeTypes != null && selectableNodeTypes.length > 0;
    }

    public String[] getSelectableNodeTypes() {
        return selectableNodeTypes;
    }

    public void setSelectableNodeTypes(String[] selectableNodeTypes) {
        this.selectableNodeTypes = selectableNodeTypes;
    }

    public boolean isLastVisitedEnabled() {
        return lastVisitedEnabled;
    }

    public void setLastVisitedEnabled(boolean lastVisitedEnabled) {
        this.lastVisitedEnabled = lastVisitedEnabled;
    }

    public String getLastVisitedKey() {
        return lastVisitedKey;
    }

    public void setLastVisitedKey(String name) {
        this.lastVisitedKey = name;
    }

    public String getBaseUUID() {
        return defaultModelUUID;
    }

    public void setDefaultModelUUID(String uuid) {
        this.defaultModelUUID = uuid;
    }

    public boolean hasBaseUUID() {
        return !Strings.isEmpty(defaultModelUUID);
    }

    public boolean isLanguageContextWare() {
        return languageContextWare;
    }

    public void setLanguageContextAware(final boolean languageContextAware) {
        this.languageContextWare = languageContextAware;
    }
}
