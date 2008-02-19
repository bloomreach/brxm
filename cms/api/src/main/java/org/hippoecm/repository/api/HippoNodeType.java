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
    final public static String USERS_PATH = "hippo:users";
    final public static String GROUPS_PATH = "hippo:groups";
    final public static String ROLES_PATH = "hippo:roles";
    final public static String FACETAUTH_PATH = "hippo:facetauths";
    final public static String PLUGIN_PATH = "hippo:plugins";
    final public static String PROTOTYPES_PATH = "hippo:prototypes";
    final public static String NAMESPACES_PATH = "hippo:namespaces";

    //--- Hippo NodeTypes ---//
    final public static String NT_APPLICATION = "hippo:application";
    final public static String NT_CONFIGURATION = "hippo:configuration";
    final public static String NT_DOCUMENT = "hippo:document";
    final public static String NT_FACETAUTH = "hippo:facetauth";
    final public static String NT_FACETAUTHFOLDER = "hippo:facetauthfolder";
    final public static String NT_FACETRESULT = "hippo:facetresult";
    final public static String NT_FACETSEARCH = "hippo:facetsearch";
    final public static String NT_FACETSELECT = "hippo:facetselect";
    final public static String NT_FACETSUBSEARCH = "hippo:facetsubsearch";
    final public static String NT_FIELD = "hippo:field";
    final public static String NT_FOLDER = "hippo:folder";
    final public static String NT_FRONTEND = "hippo:frontend";
    final public static String NT_FRONTENDPLUGIN = "hippo:frontendplugin";
    final public static String NT_GROUP = "hippo:group";
    final public static String NT_GROUPFOLDER = "hippo:groupfolder";
    final public static String NT_HANDLE = "hippo:handle";
    final public static String NT_INITIALIZEFOLDER = "hippo:initializefolder";
    final public static String NT_INITIALIZEITEM = "hippo:initializeitem";
    final public static String NT_MIRROR = "hippo:mirror";
    final public static String NT_MOUNT = "hippo:mount";
    final public static String NT_NAMESPACE = "hippo:namespace";
    final public static String NT_OCMQUERY = "hippo:ocmquery";
    final public static String NT_OCMQUERYFOLDER = "hippo:ocmqueryfolder";
    final public static String NT_PAGE = "hippo:page";
    final public static String NT_PARAMETERS = "hippo:parameters";
    final public static String NT_PLUGIN = "hippo:plugin";
    final public static String NT_PLUGINFOLDER = "hippo:pluginfolder";
    final public static String NT_PROTOTYPED = "hippo:prototyped";
    final public static String NT_REFERENCEABLE = "hippo:referenceable";
    final public static String NT_REQUEST = "hippo:request";
    final public static String NT_ROLE = "hippo:role";
    final public static String NT_ROLEFOLDER = "hippo:rolefolder";
    final public static String NT_TEMPLATE = "hippo:template";
    final public static String NT_TYPE = "hippo:type";
    final public static String NT_TYPES = "hippo:types";
    final public static String NT_USER = "hippo:user";
    final public static String NT_USERFOLDER = "hippo:userfolder";
    final public static String NT_WORKFLOW = "hippo:workflow";
    final public static String NT_WORKFLOWCATEGORY = "hippo:workflowcategory";
    final public static String NT_WORKFLOWFOLDER = "hippo:workflowfolder";

    //--- Hippo Item Names ---//
    final public static String HIPPO_CLASSNAME = "hippo:classname";
    final public static String HIPPO_CONTENT = "hippo:content";
    final public static String HIPPO_CONTENTRESOURCE = "hippo:contentresource";
    final public static String HIPPO_CONTENTROOT = "hippo:contentroot";
    final public static String HIPPO_COUNT = "hippo:count";
    final public static String HIPPO_DISCRIMINATOR = "hippo:discriminator";
    final public static String HIPPO_DISPLAY = "hippo:display";
    final public static String HIPPO_DOCBASE = "hippo:docbase";
    final public static String HIPPO_DOCUMENTS = "hippo:documents";
    final public static String HIPPO_FACET = "hippo:facet";
    final public static String HIPPO_FACETS = "hippo:facets";
    final public static String HIPPO_FIELD = "hippo:field";
    final public static String HIPPO_INITIALIZE = "hippo:initialize";
    final public static String HIPPO_MEMBERS = "hippo:members";
    final public static String HIPPO_MANDATORY = "hippo:mandatory";
    final public static String HIPPO_MIXINTYPES = "hippo:mixintypes";
    final public static String HIPPO_MODES = "hippo:modes";
    final public static String HIPPO_MULTIPLE = "hippo:multiple";
    final public static String HIPPO_NAME = "hippo:name";
    final public static String HIPPO_NAMESPACE = "hippo:namespace";
    final public static String HIPPO_NODE = "hippo:node";
    final public static String HIPPO_NODETYPE = "hippo:nodetype";
    final public static String HIPPO_NODETYPES = "hippo:nodetypes";
    final public static String HIPPO_NODETYPESRESOURCE = "hippo:nodetypesresource";
    final public static String HIPPO_ORDERED = "hippo:ordered";
    final public static String HIPPO_PASSWORD = "hippo:password";
    final public static String HIPPO_PARAMETERS = "hippo:parameters";
    final public static String HIPPO_PATH = "hippo:path";
    final public static String HIPPO_PATHS = "hippo:paths";
    final public static String HIPPO_PERMISSIONS = "hippo:permissions";
    final public static String HIPPO_PROTOTYPE = "hippo:prototype";
    final public static String HIPPO_QUERYNAME = "hippo:queryname";
    final public static String HIPPO_RENDERER = "hippo:renderer";
    final public static String HIPPO_RESULTSET = "hippo:resultset";
    final public static String HIPPO_ROLES = "hippo:roles";
    final public static String HIPPO_SEARCH = "hippo:search";
    final public static String HIPPO_SEQUENCE = "hippo:sequence";
    final public static String HIPPO_SERVICE = "hippo:classname";
    final public static String HIPPO_SUPERTYPE = "hippo:supertype";
    final public static String HIPPO_TEMPLATE = "hippo:template";
    final public static String HIPPO_TEMPLATES = "hippo:templates";
    final public static String HIPPO_TYPE = "hippo:type";
    final public static String HIPPO_TYPES = "hippo:types";
    final public static String HIPPO_UUID = "hippo:uuid";
    final public static String HIPPO_VALUES = "hippo:values";
    final public static String HIPPO_WORKFLOWS = "hippo:workflows";
}
