/*
 *  Copyright 2008 Hippo.
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
    /**
     * 
     */
    final static String SVN_ID = "$Id$";

    /**
     * 
     */
    final public static String CONFIGURATION_PATH = "hippo:configuration";
    /**
     * 
     */
    final public static String DOCUMENTS_PATH = "hippo:documents";
    /**
     * 
     */
    final public static String DOMAINS_PATH = "hippo:domains";
    /**
     * 
     */
    final public static String FRONTEND_PATH = "hippo:frontend";
    /**
     * 
     */
    final public static String GROUPS_PATH = "hippo:groups";
    /**
     * 
     */
    final public static String INITIALIZE_PATH = "hippo:initialize";
    /**
     * 
     */
    final public static String NAMESPACES_PATH = "hippo:namespaces";
    /**
     * 
     */
    final public static String PLUGIN_PATH = "hippo:plugins";
    /**
     * 
     */
    final public static String ROLES_PATH = "hippo:roles";
    /**
     * 
     */
    final public static String USERS_PATH = "hippo:users";
    /**
     * 
     */
    final public static String WORKFLOWS_PATH = "hippo:workflows";
    /**
     * 
     */
    final public static String QUERIES_PATH = "hippo:queries";
    /**
     * 
     */
    final public static String ACCESSMANAGER_PATH = "hipposys:accessmanager";
    /**
     * 
     */
    final public static String SECURITY_PATH = "hippo:security";
    /**
     * 
     */
    final public static String LOG_PATH = "hippo:log";

    //--- Hippo NodeTypes ---//
    /**
     * 
     */
    final public static String NT_AUTHROLE = "hipposys:authrole";
    /**
     * 
     */
    final public static String NT_CONFIGURATION = "hipposys:configuration";
    /**
     * 
     */
    final public static String NT_DERIVED = "hippo:derived";
    /**
     * 
     */
    final public static String HIPPO_DERIVED = "hipposys:derived";
    /**
     * 
     */
    final public static String NT_DOCUMENT = "hippo:document";
    /**
     * 
     */
    final public static String NT_DOMAIN = "hipposys:domain";
    /**
     * 
     */
    final public static String NT_DOMAINFOLDER = "hipposys:domainfolder";
    /**
     * 
     */
    final public static String NT_DOMAINRULE = "hipposys:domainrule";
    /**
     * 
     */
    final public static String NT_EXTERNALGROUP = "hipposys:externalgroup";
    /**
     * 
     */
    final public static String NT_EXTERNALROLE = "hipposys:externalrole";
    /**
     * 
     */
    final public static String NT_EXTERNALUSER = "hipposys:externaluser";
    /**
     * 
     */
    final public static String NT_FACETBASESEARCH = "hippo:facetbasesearch";
    /**
     * 
     */
    final public static String NT_FACETRESULT = "hippo:facetresult";
    /**
     * 
     */
    final public static String NT_FACETRULE = "hipposys:facetrule";
    /**
     * 
     */
    final public static String NT_FACETSEARCH = "hippo:facetsearch";
    /**
     * 
     */
    final public static String NT_FACETSELECT = "hippo:facetselect";
    /**
     * 
     */
    final public static String NT_QUERY = "hippo:query";
    /**
     * 
     */
    final public static String NT_RESOURCE = "hippo:resource";
    /**
     * 
     */
    final public static String NT_FACETSUBSEARCH = "hipposys:facetsubsearch";
    
    /**
     * 
     */
    final public static String NT_FIELD = "hipposysedit:field";
    /**
     * 
     */
    final public static String NT_GROUP = "hipposys:group";
    /**
     * 
     */
    final public static String NT_GROUPPROVIDER = "hipposys:groupprovider";
    /**
     * 
     */
    final public static String NT_GROUPFOLDER = "hipposys:groupfolder";
    /**
     * 
     */
    final public static String NT_HANDLE = "hippo:handle";
    /**
     * 
     */
    final public static String NT_HARDDOCUMENT = "hippo:harddocument";
    /**
     * 
     */
    final public static String NT_HARDHANDLE = "hippo:hardhandle";
    /**
     * 
     */
    final public static String NT_IMPLEMENTATION = "hipposys:implementation";
    /**
     * 
     */
    final public static String NT_INITIALIZEFOLDER = "hippo:initializefolder";
    /**
     * 
     */
    final public static String NT_INITIALIZEITEM = "hipposys:initializeitem";
    /**
     * 
     */
    final public static String NT_MIRROR = "hippo:mirror";
    /**
     * 
     */
    final public static String NT_MOUNT = "hippo:mount";
    /**
     * 
     */
    final public static String NT_NAMESPACE = "hipposysedit:namespace";
    /**
     * 
     */
    final public static String NT_NAMESPACEFOLDER = "hipposysedit:namespacefolder";
    /**
     * 
     */
    final public static String NT_NODETYPE = "hipposysedit:nodetype";
    /**
     * 
     */
    final public static String NT_OCMQUERY = "hipposys:ocmquery";
    /**
     * 
     */
    final public static String NT_PROTOTYPESET = "hipposysedit:prototypeset";
    /**
     * 
     */
    final public static String NT_REMODEL = "hipposysedit:remodel";
    /**
     * 
     */
    final public static String NT_REQUEST = "hippo:request";
    /**
     * 
     */
    final public static String NT_ROLE = "hipposys:role";
    /**
     * 
     */
    final public static String NT_ROLEPROVIDER = "hipposys:roleprovider";
    /**
     * 
     */
    final public static String NT_ROLEFOLDER = "hipposys:rolefolder";
    /**
     * 
     */
    final public static String NT_SECURITYFOLDER = "hipposys:securityfolder";
    /**
     * 
     */
    final public static String NT_SECURITYPROVIDER = "hipposys:securityprovider";
    /**
     * 
     */
    final public static String NT_SOFTDOCUMENT = "hipposys:softdocument";
    /**
     * 
     */
    final public static String NT_TEMPLATETYPE = "hipposysedit:templatetype";
    /**
     * 
     */
    final public static String NT_TRANSLATED = "hippo:translated";
    /**
     * 
     */
    final public static String NT_TRANSLATION = "hippo:translation";
    /**
     * 
     */
    final public static String NT_TYPE = "hipposys:type";
    /**
     * 
     */
    final public static String NT_TYPES = "hipposys:types";
    /**
     * 
     */
    final public static String NT_USER = "hipposys:user";
    /**
     * 
     */
    final public static String NT_USERPROVIDER = "hipposys:userprovider";
    /**
     * 
     */
    final public static String NT_USERFOLDER = "hipposys:userfolder";
    /**
     * 
     */
    final public static String NT_WORKFLOW = "hipposys:workflow";
    /**
     * 
     */
    final public static String NT_WORKFLOWCATEGORY = "hipposys:workflowcategory";
    /**
     * 
     */
    final public static String NT_WORKFLOWFOLDER = "hipposys:workflowfolder";

    //--- Hippo Item Names ---//
    /**
     * 
     */
    final public static String HIPPO_ACTIVE = "hipposys:active";
    /**
     * 
     */
    final public static String HIPPO_AUTOCREATED = "hipposysedit:autocreated";
    /**
     * 
     */
    final public static String HIPPO_CASCADEVALIDATION = "hipposysedit:cascadevalidation";
    /**
     * 
     */
    final public static String HIPPO_CLASS = "hippo:class";
    /**
     * 
     */
    final public static String HIPPO_CLASSNAME = "hipposys:classname";
    /**
     * 
     */
    final public static String HIPPO_PASSWORDMAXAGEDAYS = "hipposys:passwordmaxagedays";
    /**
     * 
     */
    final public static String HIPPO_CONTENT = "hippo:content";
    /**
     * 
     */
    final public static String HIPPO_CONTENTRESOURCE = "hippo:contentresource";
    /**
     * 
     */
    final public static String HIPPO_CONTENTROOT = "hippo:contentroot";
    /**
     * @deprecated
     */
    @Deprecated
    final public static String HIPPO_CONTENTDELETE = "hippo:contentdelete";
    /**
     * 
     */
    final public static String HIPPO_COUNT = "hippo:count";
    /**
     * 
     */
    final public static String HIPPO_DIRLEVELS = "hipposys:dirlevels";
    /**
     * 
     */
    final public static String HIPPO_DISCRIMINATOR = "hippo:discriminator";
    /**
     * 
     */
    final public static String HIPPO_DISPLAY = "hipposys:display";
    /**
     * 
     */
    final public static String HIPPO_DOCBASE = "hippo:docbase";
    /**
     * 
     */
    final public static String HIPPO_DOMAINSPATH = "hipposys:domainspath";
    /**
     * 
     */
    final public static String HIPPO_EQUALS = "hipposys:equals";
    /**
     * 
     */
    final public static String HIPPO_EXTENSIONSOURCE = "hipposys:extensionsource";
    /**
     * 
     */
    final public static String HIPPO_FACET = "hipposys:facet";
    /**
     * 
     */
    final public static String HIPPO_FACETS = "hippo:facets";
    
    
    /**
     * Deprecated name for field descriptor nodes.  Use field name as node name instead.
     */
    @Deprecated
    final public static String HIPPO_FIELD = "hipposysedit:field";
    /**
     * 
     */
    final public static String HIPPO_GROUPS = "hipposys:groups";
    /**
     * 
     */
    final public static String HIPPO_GROUPSPATH = "hipposys:groupspath";
    /**
     * 
     */
    final public static String HIPPO_JCRREAD = "hipposys:jcrread";
    /**
     * 
     */
    final public static String HIPPO_JCRWRITE = "hipposys:jcrwrite";
    /**
     * 
     */
    final public static String HIPPO_JCRREMOVE = "hipposys:jcrremove";
    /**
     * 
     */
    final public static String HIPPO_KEY = "hippo:key";
    /**
     * 
     */
    final public static String HIPPO_LANGUAGE = "hippo:language";
    /**
     * 
     */
    final public static String HIPPO_LASTLOGIN = "hipposys:lastlogin";
    /**
     * 
     */
    final public static String HIPPO_LASTSYNC = "hipposys:lastsync";
    /**
     * 
     */
    final public static String HIPPO_MEMBERS = "hipposys:members";
    /**
     * 
     */
    final public static String HIPPO_MESSAGE = "hippo:message";
    /**
     * 
     */
    final public static String HIPPO_MANDATORY = "hipposysedit:mandatory";
    /**
     * 
     */
    final public static String HIPPO_MIXIN = "hipposysedit:mixin";
    /**
     * 
     */
    final public static String HIPPO_MODES = "hippo:modes";
    /**
     * 
     */
    final public static String HIPPO_MULTIPLE = "hipposysedit:multiple";
    /**
     * Deprecated property for field name.  Use node name instead.
     */
    @Deprecated
    final public static String HIPPO_NAME = "hipposysedit:name";
    /**
     * 
     */
    final public static String HIPPO_NAMESPACE = "hippo:namespace";
    /**
     * 
     */
    final public static String HIPPO_NODE = "hipposysedit:node";
    /**
     * 
     */
    final public static String HIPPOSYS_NODETYPE = "hipposys:nodetype";
    /**
     * 
     */
    final public static String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";
    /**
     * 
     */
    final public static String HIPPO_NODETYPE = "hippo:nodetype";
    /**
     * 
     */
    final public static String HIPPO_NODETYPES = "hippo:nodetypes";
    /**
     * 
     */
    final public static String HIPPO_NODETYPESRESOURCE = "hippo:nodetypesresource";
    /**
     * 
     */
    final public static String HIPPO_ORDERED = "hipposysedit:ordered";
    /**
     * 
     */
    final public static String HIPPO_PASSKEY = "hipposys:passkey";
    /**
     * 
     */
    final public static String HIPPO_PASSWORD = "hipposys:password";
    /**
     * 
     */
    final public static String HIPPO_PREVIOUSPASSWORDS = "hipposys:previouspasswords";
    /**
     * 
     */
    final public static String HIPPO_PASSWORDLASTMODIFIED = "hipposys:passwordlastmodified";
    /**
     * 
     */
    final public static String HIPPO_PATH = "hipposysedit:path";
    /**
     * 
     */
    final public static String HIPPO_PATHS = "hippo:paths";
    /**
     * 
     */
    final public static String HIPPO_TEXT = "hippo:text";
    /**
     * 
     */
    final public static String HIPPO_PERMISSIONCACHESIZE = "hipposys:permissioncachesize";
    /**
     * 
     */
    final public static String HIPPO_PRIMARY = "hipposysedit:primary";
    /**
     * 
     */
    final public static String HIPPO_PRIVILEGES = "hipposys:privileges";
    /**
     * 
     */
    final public static String HIPPO_PROTECTED = "hipposysedit:protected";
    /**
     * 
     */
    final public static String HIPPO_PROTOTYPE = "hipposysedit:prototype";
    /**
     * 
     */
    final public static String HIPPO_PROTOTYPES = "hipposysedit:prototypes";
    /**
     * 
     */
    final public static String HIPPO_PROPERTY = "hippo:property";
    /**
     * 
     */
    final public static String HIPPO_QUERYNAME = "hippo:queryname";
    /**
     * 
     */
    final public static String HIPPO_RELATED = "hippo:related";
    /**
     * 
     */
    final public static String HIPPO_COMPUTE = "hippo:compute";
    /**
     * 
     */
    final public static String HIPPO_RESULTSET = "hippo:resultset";
    /**
     * 
     */
    final public static String HIPPO_ROLE = "hipposys:role";
    /**
     * 
     */
    final public static String HIPPO_ROLES = "hipposys:roles";
    /**
     * 
     */
    final public static String HIPPO_ROLESPATH = "hipposys:rolespath";
    /**
     * 
     */
    final public static String HIPPO_SECURITYPROVIDER = "hipposys:securityprovider";
    /**
     * 
     */
    final public static String HIPPO_SEARCH = "hipposys:search";
    /**
     * 
     */
    final public static String HIPPO_SEQUENCE = "hippo:sequence";
    /**
     * 
     */
    final public static String HIPPO_SUPERTYPE = "hipposysedit:supertype";
    /**
     * 
     */
    final public static String HIPPO_TEMPLATE = "hipposysedit:template";
    /**
     * 
     */
    final public static String HIPPO_TRANSLATION = "hippo:translation";
    /**
     * 
     */
    final public static String HIPPOSYS_TYPE = "hipposys:type";
    /**
     * 
     */
    final public static String HIPPOSYSEDIT_TYPE = "hipposysedit:type";
    /**
     * 
     */
    final public static String HIPPO_TYPES = "hipposys:types";
    /**
     * 
     */
    final public static String HIPPO_URI = "hipposysedit:uri";
    /**
     * 
     */
    final public static String HIPPO_USERS = "hipposys:users";
    /**
     * 
     */
    final public static String HIPPO_USERSPATH = "hipposys:userspath";
    /**
     * 
     */
    final public static String HIPPO_UUID = "hippo:uuid";
    /**
     * 
     */
    final public static String HIPPO_VERSION = "hippo:version";
    /**
     * 
     */
    final public static String HIPPO_VALIDATORS = "hipposysedit:validators";
    /**
     * 
     */
    final public static String HIPPO_VALUE = "hippo:value";
    /**
     * 
     */
    final public static String HIPPOSYS_VALUE = "hipposys:value";
    /**
     * 
     */
    final public static String HIPPO_VALUES = "hippo:values";

    final public static String HIPPO_CONTENTPROPSET = "hippo:contentpropset";
    final public static String HIPPO_CONTENTPROPADD = "hippo:contentpropadd";
}
