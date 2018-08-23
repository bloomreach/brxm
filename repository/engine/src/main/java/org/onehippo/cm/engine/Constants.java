/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

    public static final String HIPPO_NAMESPACE = "http://www.onehippo.org/jcr/hippo/nt/2.0.4";
    public static final String HIPPO_PREFIX = "hippo";
    public static final String HCM_NAMESPACE = "http://www.onehippo.org/jcr/hcm/1.0";
    public static final String HCM_PREFIX = "hcm";

    public static final String NT_HCM_ROOT = "hcm:hcm";
    public static final String NT_HCM_BASELINE = "hcm:baseline";
    public static final String NT_HCM_SITES = "hcm:sites";
    public static final String NT_HCM_SITE = "hcm:site";
    public static final String NT_HCM_GROUP = "hcm:group";
    public static final String NT_HCM_PROJECT = "hcm:project";
    public static final String NT_HCM_MODULE = "hcm:module";
    public static final String NT_HCM_DESCRIPTOR = "hcm:descriptor";
    public static final String NT_HCM_ACTIONS = "hcm:actions";
    public static final String NT_HCM_CONTENT = "hcm:content";
    public static final String NT_HCM_BUNDLES = "hcm:webbundles";
    public static final String NT_HCM_CONTENT_SOURCE = "hcm:contentsource";
    public static final String NT_HCM_CONTENT_FOLDER = "hcm:contentfolder";
    public static final String NT_HCM_CONFIG_FOLDER = "hcm:configfolder";
    public static final String NT_HCM_DEFINITIONS = "hcm:definitions";
    public static final String NT_HCM_CND = "hcm:cnd";
    public static final String NT_HCM_BINARY = "hcm:binary";

    public static final String HCM_ROOT = NT_HCM_ROOT;
    public static final String HCM_BASELINE = "hcm:baseline";
    public static final String HCM_SITES = "hcm:sites";
    public static final String HCM_HSTROOT = "hcm:hstroot";
    public static final String HCM_LAST_UPDATED = "hcm:lastupdated";
    public static final String HCM_MODULE_DESCRIPTOR = HCM_MODULE_YAML;
    public static final String HCM_MODULE_SEQUENCE = "hcm:seqnumber";
    public static final String HCM_LAST_EXECUTED_ACTION = "hcm:lastexecutedaction";
    public static final String HCM_ACTIONS = ACTIONS_YAML;
    public static final String HCM_CONTENT_PATHS_APPLIED = "hcm:contentPathsApplied";
    public static final String HCM_BUNDLES_DIGESTS = "hcm:bundlesDigest";
    public static final String HCM_PROCESSED = "hcm:processed";
    public static final String HCM_CONTENT_PATH = "hcm:contentpath";
    public static final String HCM_CONTENT_ORDER_BEFORE = "hcm:contentorderbefore";
    public static final String HCM_CONTENT_ORDER_BEFORE_FIRST = ".:order first:.";
    public static final String HCM_YAML = "hcm:yaml";
    public static final String HCM_CND = "hcm:cnd";
    // This should be one of the required digest algorithms (MD5, SHA-1, or SHA-256)
    public static final String HCM_DIGEST = "hcm:digest";

    public static final String HCM_ROOT_PATH = "/" + HCM_ROOT;
    public static final String HCM_BASELINE_PATH = HCM_ROOT_PATH + "/" + HCM_BASELINE;
    public static final String HCM_CONTENT_NODE_PATH = HCM_ROOT_PATH + "/" + NT_HCM_CONTENT;
    public static final String HCM_BUNDLE_NODE_PATH = HCM_ROOT_PATH + "/" + NT_HCM_BUNDLES;

    public static final String HCM_SITE_DESCRIPTOR = "hcm-site.yaml";
    public static final String HCM_SITE_DESCRIPTOR_LOCATION = "META-INF/" + HCM_SITE_DESCRIPTOR;

    public static final String PRODUCT_GROUP_NAME = "hippo-cms";

    public static final String SYSTEM_PARAMETER_REPO_BOOTSTRAP = "repo.bootstrap";
    public static final String SYSTEM_PARAMETER_AUTOEXPORT_LOOP_PROTECTION = "autoexport.loop.protection";

    // Path within a Maven module where we expect the module descriptor to be
    public static final String MAVEN_MODULE_DESCRIPTOR = "/src/main/resources/" + HCM_MODULE_YAML;

    // Default HST root node -- needed for auto-export to handle locationMapper rules correctly
    public static final String HST_DEFAULT_ROOT_PATH = "/hst:hst";

    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
}
