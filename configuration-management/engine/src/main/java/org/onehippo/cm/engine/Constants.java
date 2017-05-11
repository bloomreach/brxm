/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine;

public final class Constants {

    public static final boolean DEFAULT_EXPLICIT_SEQUENCING = false;

    // Constants used by engine to locate config files

    public static final String REPO_CONTENT_FOLDER = "hcm-content";
    public static final String REPO_CONFIG_FOLDER = "hcm-config";

    public static final String YAML_EXT =".yaml";
    public static final String REPO_CONFIG_YAML = "hcm-module" + YAML_EXT;
    public static final String ACTIONS_YAML = "hcm-actions" + YAML_EXT;

    // Following keys used in hcm-module.yaml
    public static final String CONFIGURATIONS_KEY = "groups";
    public static final String PROJECTS_KEY = "projects";
    public static final String MODULES_KEY = "modules";
    public static final String CONFIGURATION_KEY = "group";
    public static final String PROJECT_KEY = "project";
    public static final String MODULE_KEY = "module";
    public static final String AFTER_KEY = "after";

    // Following keys used in module yaml
    public static final String DEFINITIONS = "definitions";
    public static final String OPERATION_KEY = "operation";
    public static final String TYPE_KEY = "type";
    public static final String VALUE_KEY = "value";
    public static final String RESOURCE_KEY = "resource";
    public static final String PATH_KEY = "path";
    public static final String PREFIX_KEY = "prefix";
    public static final String URI_KEY = "uri";
    public static final String META_KEY_PREFIX = ".meta:";
    public static final String META_IGNORE_REORDERED_CHILDREN = META_KEY_PREFIX + "ignore-reordered-children";
    public static final String META_DELETE_KEY = META_KEY_PREFIX + "delete";
    public static final String META_ORDER_BEFORE_KEY = META_KEY_PREFIX + "order-before";

    // JCR node where the HCM configuration baseline is stored
    // NOTE: currently this MUST only be a single new node with all required parents existing
    // TODO: Move under hippo:configuration?
    public static final String BASELINE_PATH = "hcm:baseline";
    public static final String BASELINE_TYPE = "hcm:baseline";
    public static final String GROUP_TYPE = "hcm:group";
    public static final String PROJECT_TYPE = "hcm:project";
    public static final String MODULE_TYPE = "hcm:module";

    public static final String LAST_UPDATED_PROPERTY = "hcm:lastupdated";
    public static final String MANIFEST_PROPERTY = "hcm:manifest";

    public static final String MODULE_DESCRIPTOR_NODE = REPO_CONFIG_YAML;
    public static final String MODULE_DESCRIPTOR_TYPE = "hcm:descriptor";
    public static final String ACTIONS_TYPE = "hcm:actions";
    public static final String ACTIONS_NODE = ACTIONS_YAML;
    public static final String CONTENT_TYPE = "hcm:content";

    public static final String CONTENT_FOLDER_TYPE = "hcm:contentfolder";
    public static final String CONTENT_PATH_PROPERTY = "hcm:contentpath";
    public static final String CONFIG_FOLDER_TYPE = "hcm:configfolder";

    public static final String DEFINITIONS_TYPE = "hcm:definitions";
    public static final String CND_TYPE = "hcm:cnd";
    public static final String BINARY_TYPE = "hcm:binary";

    public static final String YAML_PROPERTY = "hcm:yaml";
    public static final String CND_PROPERTY = "hcm:cnd";

    // This should be one of the required digest algorithms (MD5, SHA-1, or SHA-256)
    public static final String DIGEST_PROPERTY = "hcm:digest";
    public static final String DEFAULT_DIGEST = "MD5";
}
