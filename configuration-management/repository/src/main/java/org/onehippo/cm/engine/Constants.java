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

import static org.onehippo.cm.model.Constants.ACTIONS_YAML;
import static org.onehippo.cm.model.Constants.HCM_MODULE_YAML;

public final class Constants {

    // JCR node where the HCM configuration baseline is stored
    // NOTE: currently this MUST only be a single new node with all required parents existing
    // TODO: Move under a new /hcm:hcm node at JCR root, and new node type hcm:hcm
    public static final String HCM_ROOT_NODE = "hcm:hcm";
    public static final String BASELINE_NODE = "hcm:baseline";
    public static final String BASELINE_TYPE = "hcm:baseline";
    public static final String GROUP_TYPE = "hcm:group";
    public static final String PROJECT_TYPE = "hcm:project";
    public static final String MODULE_TYPE = "hcm:module";

    public static final String LAST_UPDATED_PROPERTY = "hcm:lastupdated";

    public static final String MODULE_DESCRIPTOR_NODE = HCM_MODULE_YAML;
    public static final String MODULE_DESCRIPTOR_TYPE = "hcm:descriptor";
    public static final String MODULE_SEQUENCE_NUMBER = "hcm:seqnumber";
    public static final String ACTIONS_TYPE = "hcm:actions";
    public static final String ACTIONS_NODE = ACTIONS_YAML;
    public static final String CONTENT_TYPE = "hcm:content";
    public static final String CONTENT_SOURCE_TYPE = "hcm:contentsource";
    public static final String HCM_CONTENT_PATHS_APPLIED = "hcm:contentPathsApplied";
    public static final String HCM_MODULE_ACTIONS_APPLIED = "hcm:moduleActionsApplied";

    public static final String CONTENT_FOLDER_TYPE = "hcm:contentfolder";
    public static final String HCM_PROCESSED = "hcm:processed";
    public static final String CONTENT_PATH_PROPERTY = "hcm:contentpath";
    public static final String CONFIG_FOLDER_TYPE = "hcm:configfolder";

    public static final String DEFINITIONS_TYPE = "hcm:definitions";
    public static final String CND_TYPE = "hcm:cnd";
    public static final String BINARY_TYPE = "hcm:binary";

    public static final String YAML_PROPERTY = "hcm:yaml";
    public static final String CND_PROPERTY = "hcm:cnd";

    // This should be one of the required digest algorithms (MD5, SHA-1, or SHA-256)
    public static final String DIGEST_PROPERTY = "hcm:digest";
}
