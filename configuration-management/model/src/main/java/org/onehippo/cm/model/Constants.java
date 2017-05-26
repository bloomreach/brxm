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

package org.onehippo.cm.model;

public final class Constants {

    public static final boolean DEFAULT_EXPLICIT_SEQUENCING = false;

    // Constants used by engine to locate config files

    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
    public static final String BOOTSTRAP_MODULES_PROPERTY = "repo.bootstrap.modules";
    public static final String HCM_CONTENT_FOLDER = "hcm-content";
    public static final String HCM_CONFIG_FOLDER = "hcm-config";

    public static final String YAML_EXT =".yaml";
    public static final String HCM_MODULE_YAML = "hcm-module" + YAML_EXT;
    public static final String ACTIONS_YAML = "hcm-actions" + YAML_EXT;
    public static final String ACTION_LISTS_NODE = "action-lists";

    // Following keys used in hcm-module.yaml
    public static final String GROUPS_KEY = "groups";
    public static final String PROJECTS_KEY = "projects";
    public static final String MODULES_KEY = "modules";
    public static final String GROUP_KEY = "group";
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
    public static final String META_CATEGORY_KEY = META_KEY_PREFIX + "category";
    public static final String META_RESIDUAL_CHILD_NODE_CATEGORY_KEY = META_KEY_PREFIX + "residual-child-node-category";

    public static final String DEFAULT_DIGEST = "MD5";

    // Path within a Maven module where we expect the module descriptor to be
    public static final String MAVEN_MODULE_DESCRIPTOR = "src/main/resources/" + HCM_MODULE_YAML;
}
