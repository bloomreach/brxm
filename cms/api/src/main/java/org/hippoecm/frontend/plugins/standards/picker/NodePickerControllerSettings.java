/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.service.render.AbstractRenderService;

public class NodePickerControllerSettings implements IClusterable {

    //IPluginconfig keys
    public static final String BASE_UUID = "base.uuid";
    public static final String BASE_PATH = "base.path";
    public static final String CLUSTER_NAME = "cluster.name";
    public static final String CLUSTER_OPTIONS = "cluster.options";
    public static final String FOLDER_SERVICE_KEY = "folder.service.key";
    public static final String LAST_VISITED_KEY = "last.visited.key";
    public static final String LAST_VISITED_ENABLED = "last.visited.enabled";
    public static final String LAST_VISITED_NODETYPES = "last.visited.nodetypes";
    public static final String SELECTABLE_NODETYPES = "nodetypes";
    public static final String SELECTION_SERVICE_KEY = "selection.service.key";

    //Default values
    private static final String DEFAULT_CLUSTER = "cms-pickers/documents";
    private static final String DEFAULT_FOLDER_SERVICE_KEY = "model.folder";
    private static final String DEFAULT_LAST_VISITED_KEY = "node-picker-controller";
    private static final boolean DEFAULT_LAST_VISITED_ENABLED = true;
    private static final String DEFAULT_SELECTION_SERVICE_KEY = "wicket.model";

    private String clusterName;
    private IPluginConfig clusterOptions;
    private String defaultModelUUID;
    private String folderServiceKey;
    private boolean lastVisitedEnabled;
    private String lastVisitedKey;
    private String[] lastVisitedNodeTypes;
    private String[] selectableNodeTypes;
    private String selectionServiceKey;

    public NodePickerControllerSettings() {
        clusterName = DEFAULT_CLUSTER;
        folderServiceKey = DEFAULT_FOLDER_SERVICE_KEY;
        lastVisitedEnabled = DEFAULT_LAST_VISITED_ENABLED;
        lastVisitedKey = DEFAULT_LAST_VISITED_KEY;
        selectionServiceKey = DEFAULT_SELECTION_SERVICE_KEY;
    }

    public static NodePickerControllerSettings fromPluginConfig(final IPluginConfig config) {
        final JavaPluginConfig clusterOptions = new JavaPluginConfig(config.getPluginConfig(CLUSTER_OPTIONS));
        clusterOptions.remove(AbstractRenderService.WICKET_ID); // enforce a unique ID for this node picker

        final NodePickerControllerSettings settings = new NodePickerControllerSettings();
        settings.setClusterName(config.getString(CLUSTER_NAME, DEFAULT_CLUSTER));
        settings.setClusterOptions(clusterOptions);
        settings.setSelectionServiceKey(config.getString(SELECTION_SERVICE_KEY, DEFAULT_SELECTION_SERVICE_KEY));
        settings.setFolderServiceKey(config.getString(FOLDER_SERVICE_KEY, DEFAULT_FOLDER_SERVICE_KEY));
        settings.setSelectableNodeTypes(config.getStringArray(SELECTABLE_NODETYPES));
        settings.setLastVisitedNodeTypes(config.getStringArray(LAST_VISITED_NODETYPES));
        settings.setLastVisitedKey(config.getString(LAST_VISITED_KEY, DEFAULT_LAST_VISITED_KEY));
        settings.setLastVisitedEnabled(config.getAsBoolean(LAST_VISITED_ENABLED, DEFAULT_LAST_VISITED_ENABLED));
        settings.setDefaultModelUUID(config.getString(BASE_UUID, null));

        return settings;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(final String clusterName) {
        this.clusterName = clusterName;
    }

    public IPluginConfig getClusterOptions() {
        return clusterOptions;
    }

    public void setClusterOptions(final IPluginConfig clusterOptions) {
        this.clusterOptions = clusterOptions;
    }

    public String getSelectionServiceKey() {
        return selectionServiceKey;
    }

    public void setSelectionServiceKey(final String selectionServiceKey) {
        this.selectionServiceKey = selectionServiceKey;
    }

    public String getFolderServiceKey() {
        return folderServiceKey;
    }

    public void setFolderServiceKey(final String folderServiceKey) {
        this.folderServiceKey = folderServiceKey;
    }

    public boolean hasLastVisitedNodeTypes() {
        return lastVisitedNodeTypes != null && lastVisitedNodeTypes.length > 0;
    }

    public String[] getLastVisitedNodeTypes() {
        return lastVisitedNodeTypes;
    }

    public void setLastVisitedNodeTypes(final String[] lastVisitedNodeTypes) {
        this.lastVisitedNodeTypes = lastVisitedNodeTypes;
    }

    public boolean hasSelectableNodeTypes() {
        return selectableNodeTypes != null && selectableNodeTypes.length > 0;
    }

    public String[] getSelectableNodeTypes() {
        return selectableNodeTypes;
    }

    public void setSelectableNodeTypes(final String[] selectableNodeTypes) {
        this.selectableNodeTypes = selectableNodeTypes;
    }

    public boolean isLastVisitedEnabled() {
        return lastVisitedEnabled;
    }

    public void setLastVisitedEnabled(final boolean lastVisitedEnabled) {
        this.lastVisitedEnabled = lastVisitedEnabled;
    }

    public String getLastVisitedKey() {
        return lastVisitedKey;
    }

    public void setLastVisitedKey(final String name) {
        this.lastVisitedKey = name;
    }

    public String getBaseUUID() {
        return defaultModelUUID;
    }

    public void setDefaultModelUUID(final String uuid) {
        this.defaultModelUUID = uuid;
    }

    public boolean hasBaseUUID() {
        return !Strings.isEmpty(defaultModelUUID);
    }
}
