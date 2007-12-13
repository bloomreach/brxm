/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

/*
 * This file has to be kept in sync with:
 * core/src/main/resources/org/hippoecm/repository/repository.cnd
 */

/**
 * This interface defines the node types and item names that are in use by
 * the Hippo repository build on top of JCR.
 */

public interface HippoNodeType {

    final public static String CONFIGURATION_PATH = "hippo:configuration";
    final public static String INITIALIZE_PATH = "hippo:initialize";
    final public static String WORKFLOWS_PATH = "hippo:workflows";
    final public static String DOCUMENTS_PATH = "hippo:documents";
    final public static String FRONTEND_PATH = "hippo:frontend";
    final public static String PLUGIN_PATH = "hippo:plugins";

    final public static String NT_CONFIGURATION = "hippo:configuration";
    final public static String NT_DOCUMENT = "hippo:document";
    final public static String NT_FACETRESULT = "hippo:facetresult";
    final public static String NT_FACETSUBSEARCH = "hippo:facetsubsearch";
    final public static String NT_FACETSEARCH = "hippo:facetsearch";
    final public static String NT_FACETSELECT = "hippo:facetselect";
    final public static String NT_HANDLE = "hippo:handle";
    final public static String NT_MIRROR = "hippo:mirror";
    final public static String NT_INITIALIZEFOLDER = "hippo:initializefolder";
    final public static String NT_INITIALIZEITEM = "hippo:initializeitem";
    final public static String NT_QUERY = "hippo:query";
    final public static String NT_QUERYFOLDER = "hippo:queryfolder";
    final public static String NT_REFERENCEABLE = "hippo:referenceable";
    final public static String NT_TYPES = "hippo:types";
    final public static String NT_WORKFLOW = "hippo:workflow";
    final public static String NT_WORKFLOWCATEGORY = "hippo:workflowcategory";
    final public static String NT_WORKFLOWFOLDER = "hippo:workflowfolder";
    final public static String NT_PLUGINFOLDER = "hippo:pluginfolder";
    final public static String NT_PLUGIN = "hippo:plugin";

    final public static String HIPPO_CLASSNAME = "hippo:classname";
    final public static String HIPPO_CONTENT = "hippo:content";
    final public static String HIPPO_CONTENTROOT = "hippo:contentroot";
    final public static String HIPPO_COUNT = "hippo:count";
    final public static String HIPPO_DISPLAY = "hippo:display";
    final public static String HIPPO_DOCBASE = "hippo:docbase";
    final public static String HIPPO_DOCUMENTS = "hippo:documents";
    final public static String HIPPO_FACETS = "hippo:facets";
    final public static String HIPPO_INITIALIZE = "hippo:initialize";
    final public static String HIPPO_LANGUAGE = "hippo:language";
    final public static String HIPPO_MODES = "hippo:modes";
    final public static String HIPPO_NAMESPACE = "hippo:namespace";
    final public static String HIPPO_NODETYPE = "hippo:nodetype";
    final public static String HIPPO_NODETYPES = "hippo:nodetypes";
    final public static String HIPPO_PATHS = "hippo:paths";
    final public static String HIPPO_QUERY = "hippo:query";
    final public static String HIPPO_QUERYNAME = "hippo:queryname";
    final public static String HIPPO_RENDERER = "hippo:renderer";
    final public static String HIPPO_RESULTSET = "hippo:resultset";
    final public static String HIPPO_SEARCH = "hippo:search";
    final public static String HIPPO_SERVICE = "hippo:classname";
    final public static String HIPPO_TYPES = "hippo:types";
    final public static String HIPPO_UUID = "hippo:uuid";
    final public static String HIPPO_VALUES = "hippo:values";
    final public static String HIPPO_WORKFLOWS = "hippo:workflows";

    final public static String HIPPO_FRONTENDPLUGIN = "hippo:frontendplugin";
}
