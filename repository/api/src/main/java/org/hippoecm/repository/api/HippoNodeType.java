/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

/**
 * This interface defines the node types and item names that are in use by
 * the Hippo repository build on top of JCR.
 */
public interface HippoNodeType {

    public static final String CONFIGURATION_PATH = "hippo:configuration";

    public static final String DOCUMENTS_PATH = "hippo:documents";

    public static final String DOMAINS_PATH = "hippo:domains";

    public static final String FRONTEND_PATH = "hippo:frontend";

    public static final String GROUPS_PATH = "hippo:groups";

    public static final String INITIALIZE_PATH = "hippo:initialize";

    public static final String MODULES_PATH = "hippo:modules";

    public static final String NAMESPACES_PATH = "hippo:namespaces";

    public static final String PLUGIN_PATH = "hippo:plugins";

    public static final String ROLES_PATH = "hippo:roles";

    public static final String USERS_PATH = "hippo:users";

    public static final String WORKFLOWS_PATH = "hippo:workflows";

    public static final String QUERIES_PATH = "hippo:queries";

    public static final String ACCESSMANAGER_PATH = "hipposys:accessmanager";

    public static final String SECURITY_PATH = "hippo:security";

    public static final String LOG_PATH = "hippo:log";

    public static final String TEMPORARY_PATH = "hippo:temporary";

    public static final String NT_AUTHROLE = "hipposys:authrole";

    public static final String NT_CONFIGURATION = "hipposys:configuration";

    public static final String NT_DERIVED = "hippo:derived";

    public static final String HIPPO_DERIVED = "hipposys:derived";

    public static final String NT_DOCUMENT = "hippo:document";

    public static final String NT_DOMAIN = "hipposys:domain";

    public static final String NT_DOMAINFOLDER = "hipposys:domainfolder";

    public static final String NT_DOMAINRULE = "hipposys:domainrule";

    public static final String NT_EXTERNALGROUP = "hipposys:externalgroup";

    public static final String NT_EXTERNALROLE = "hipposys:externalrole";

    public static final String NT_EXTERNALUSER = "hipposys:externaluser";

    public static final String NT_FACETBASESEARCH = "hippo:facetbasesearch";

    public static final String NT_FACETRESULT = "hippo:facetresult";

    public static final String NT_FACETRULE = "hipposys:facetrule";

    public static final String NT_FACETSEARCH = "hippo:facetsearch";

    public static final String NT_FACETSELECT = "hippo:facetselect";

    public static final String NT_QUERY = "hippo:query";

    public static final String NT_RESOURCE = "hippo:resource";

    public static final String NT_FACETSUBSEARCH = "hipposys:facetsubsearch";

    public static final String NT_FIELD = "hipposysedit:field";

    public static final String NT_GROUP = "hipposys:group";

    public static final String NT_GROUPPROVIDER = "hipposys:groupprovider";

    public static final String NT_GROUPFOLDER = "hipposys:groupfolder";

    public static final String NT_HANDLE = "hippo:handle";

    /**
     * @deprecated since 2.26.00 (7.9.0) Do not use any more. Usages have been replaced with
     * {@link org.onehippo.repository.util.JcrConstants#MIX_VERSIONABLE} or
     * {@link org.onehippo.repository.util.JcrConstants#MIX_REFERENCEABLE}
     */
    @Deprecated
    public static final String NT_HARDDOCUMENT = "hippo:harddocument";

    /**
     * @deprecated since 2.26.00 (7.9.0) Do not use any more. Usages have been replaced with
     * {@link org.onehippo.repository.util.JcrConstants#MIX_VERSIONABLE} or
     * {@link org.onehippo.repository.util.JcrConstants#MIX_REFERENCEABLE}
     */
    @Deprecated
    public static final String NT_HARDHANDLE = "hippo:hardhandle";

    public static final String NT_IMPLEMENTATION = "hipposys:implementation";

    public static final String NT_INITIALIZEFOLDER = "hippo:initializefolder";

    public static final String NT_INITIALIZEITEM = "hipposys:initializeitem";

    public static final String NT_MIRROR = "hippo:mirror";

    public static final String NT_MOUNT = "hippo:mount";

    public static final String NT_NAMESPACE = "hipposysedit:namespace";

    public static final String NT_NAMESPACEFOLDER = "hipposysedit:namespacefolder";

    public static final String NT_NODETYPE = "hipposysedit:nodetype";

    @Deprecated
    public static final String NT_OCMQUERY = "hipposys:ocmquery";

    public static final String NT_PROTOTYPESET = "hipposysedit:prototypeset";

    public static final String NT_REMODEL = "hipposysedit:remodel";

    public static final String NT_REQUEST = "hippo:request";

    public static final String NT_ROLE = "hipposys:role";

    public static final String NT_ROLEPROVIDER = "hipposys:roleprovider";

    public static final String NT_ROLEFOLDER = "hipposys:rolefolder";

    public static final String NT_SECURITYFOLDER = "hipposys:securityfolder";

    public static final String NT_SECURITYPROVIDER = "hipposys:securityprovider";

    public static final String NT_SOFTDOCUMENT = "hipposys:softdocument";

    public static final String NT_TEMPLATETYPE = "hipposysedit:templatetype";

    public static final String NT_TRANSLATED = "hippo:translated";

    public static final String NT_TRANSLATION = "hippo:translation";

    @Deprecated
    public static final String NT_TYPE = "hipposys:type";

    @Deprecated
    public static final String NT_TYPES = "hipposys:types";

    public static final String NT_USER = "hipposys:user";

    public static final String NT_USERPROVIDER = "hipposys:userprovider";

    public static final String NT_USERFOLDER = "hipposys:userfolder";

    public static final String NT_WORKFLOW = "hipposys:workflow";

    public static final String NT_WORKFLOWCATEGORY = "hipposys:workflowcategory";

    public static final String NT_WORKFLOWFOLDER = "hipposys:workflowfolder";

    public static final String NT_MODULE = "hipposys:module";

    public static final String NT_DELETED = "hippo:deleted";

    public static final String NT_SKIPINDEX = "hippo:skipindex";

    public static final String NT_LOCKABLE = "hippo:lockable";

    public static final String HIPPO_ACTIVE = "hipposys:active";

    public static final String HIPPO_SYSTEM = "hipposys:system";

    public static final String HIPPO_AUTOCREATED = "hipposysedit:autocreated";

    public static final String HIPPO_AVAILABILITY = "hippo:availability";

    public static final String HIPPO_CASCADEVALIDATION = "hipposysedit:cascadevalidation";

    public static final String HIPPO_CLASS = "hippo:class";

    public static final String HIPPO_CLASSNAME = "hipposys:classname";

    public static final String HIPPO_PASSWORDMAXAGEDAYS = "hipposys:passwordmaxagedays";

    public static final String HIPPO_CONTENT = "hippo:content";

    public static final String HIPPO_CONTENTRESOURCE = "hippo:contentresource";

    public static final String HIPPO_CONTENTROOT = "hippo:contentroot";

    public static final String HIPPO_CONTEXTPATHS = "hippo:contextpaths";

    public static final String HIPPO_CHANGERECORD = "hippo:changerecord";

    public static final String HIPPO_CONFIG = "hipposys:config";

    public static final String HIPPO_CONTENTDELETE = "hippo:contentdelete";

    public static final String HIPPO_CONTENTPROPDELETE = "hippo:contentpropdelete";

    public static final String HIPPO_COUNT = "hippo:count";

    public static final String HIPPO_DELETED_DATE = "hippo:deletedDate";

    public static final String HIPPO_DELETED_BY = "hippo:deletedBy";

    public static final String HIPPO_DIRLEVELS = "hipposys:dirlevels";

    /**
     * @deprecated since 3.0.1 with no substitute
     */
    public static final String HIPPO_DISCRIMINATOR = "hippo:discriminator";

    public static final String HIPPO_DISPLAY = "hipposys:display";

    public static final String HIPPO_DOCBASE = "hippo:docbase";

    public static final String HIPPO_DOMAINSPATH = "hipposys:domainspath";

    public static final String HIPPO_EQUALS = "hipposys:equals";

    public static final String HIPPO_EXECUTED = "hipposys:executed";

    public static final String HIPPO_EXTENSIONSOURCE = "hipposys:extensionsource";

   /**
     * @deprecated
     */
    public static final String HIPPO_EXTENSIONBUILD = "hipposys:extensionbuild";

    public static final String HIPPO_EXTENSIONVERSION = "hipposys:extensionversion";

    public static final String HIPPO_RELOADONSTARTUP = "hippo:reloadonstartup";

    public static final String HIPPO_FACET = "hipposys:facet";

    public static final String HIPPO_FACETS = "hippo:facets";

    public static final String HIPPO_FILENAME = "hippo:filename";

    public static final String HIPPO_GROUPS = "hipposys:groups";

    public static final String HIPPO_GROUPSPATH = "hipposys:groupspath";

    public static final String HIPPO_JCRREAD = "hipposys:jcrread";

    public static final String HIPPO_JCRWRITE = "hipposys:jcrwrite";

    public static final String HIPPO_JCRREMOVE = "hipposys:jcrremove";

    public static final String HIPPO_KEY = "hippo:key";

    public static final String HIPPO_LANGUAGE = "hippo:language";

    public static final String HIPPO_LASTLOGIN = "hipposys:lastlogin";

    public static final String HIPPO_LASTSYNC = "hipposys:lastsync";

    public static final String HIPPO_MEMBERS = "hipposys:members";

    public static final String HIPPO_MESSAGE = "hippo:message";

    public static final String HIPPO_MANDATORY = "hipposysedit:mandatory";

    public static final String HIPPO_MIXIN = "hipposysedit:mixin";

    public static final String HIPPO_MODES = "hippo:modes";

    public static final String HIPPO_MODULECONFIG = "hippo:moduleconfig";

    public static final String HIPPO_MULTIPLE = "hipposysedit:multiple";

    public static final String HIPPO_NAMESPACE = "hippo:namespace";

    public static final String HIPPO_NODE = "hipposysedit:node";

    public static final String HIPPOSYS_NODETYPE = "hipposys:nodetype";

    public static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype";

    public static final String HIPPO_NODETYPE = "hippo:nodetype";

    public static final String HIPPO_NODETYPES = "hippo:nodetypes";

    public static final String HIPPO_NODETYPESRESOURCE = "hippo:nodetypesresource";

    public static final String HIPPO_ORDERED = "hipposysedit:ordered";

    public static final String HIPPO_PASSKEY = "hipposys:passkey";

    public static final String HIPPO_PASSWORD = "hipposys:password";

    public static final String HIPPO_PREVIOUSPASSWORDS = "hipposys:previouspasswords";

    public static final String HIPPO_PASSWORDLASTMODIFIED = "hipposys:passwordlastmodified";

    public static final String HIPPO_PATH = "hipposysedit:path";

    public static final String HIPPO_PATHS = "hippo:paths";

    public static final String HIPPO_REQUEST = "hippo:request";

    public static final String HIPPO_TEXT = "hippo:text";

    public static final String HIPPO_PERMISSIONCACHESIZE = "hipposys:permissioncachesize";

    public static final String HIPPO_PRIMARY = "hipposysedit:primary";

    public static final String HIPPO_PRIVILEGES = "hipposys:privileges";

    public static final String HIPPO_PROTECTED = "hipposysedit:protected";

    public static final String HIPPO_PROTOTYPE = "hipposysedit:prototype";

    public static final String HIPPO_PROTOTYPES = "hipposysedit:prototypes";

    public static final String HIPPO_PROPERTY = "hippo:property";

    public static final String HIPPO_QUERYNAME = "hippo:queryname";

    public static final String HIPPO_RELATED = "hippo:related";

    public static final String HIPPO_COMPUTE = "hippo:compute";

    public static final String HIPPO_RESULTSET = "hippo:resultset";

    public static final String HIPPO_ROLE = "hipposys:role";

    public static final String HIPPO_ROLES = "hipposys:roles";

    public static final String HIPPO_ROLESPATH = "hipposys:rolespath";

    public static final String HIPPO_SECURITYPROVIDER = "hipposys:securityprovider";

    public static final String HIPPO_SEARCH = "hipposys:search";

    public static final String HIPPO_SEQUENCE = "hippo:sequence";

    public static final String HIPPOSYS_SUBTYPE = "hipposys:subtype";

    public static final String HIPPO_SUPERTYPE = "hipposysedit:supertype";

    public static final String HIPPO_TEMPLATE = "hipposysedit:template";

    public static final String HIPPO_TRANSLATION = "hippo:translation";

    public static final String HIPPOSYS_TYPE = "hipposys:type";

    public static final String HIPPOSYS_FILTER = "hipposys:filter";

    public static final String HIPPOSYSEDIT_TYPE = "hipposysedit:type";

    @Deprecated
    public static final String HIPPO_TYPES = "hipposys:types";

    public static final String HIPPO_URI = "hipposysedit:uri";

    public static final String HIPPO_USERS = "hipposys:users";

    public static final String HIPPO_USERSPATH = "hipposys:userspath";

    public static final String HIPPO_UUID = "hippo:uuid";

    public static final String HIPPO_VERSION = "hippo:version";

    public static final String HIPPO_VALIDATORS = "hipposysedit:validators";

    public static final String HIPPO_VALUE = "hippo:value";

    public static final String HIPPOSYS_VALUE = "hipposys:value";

    public static final String HIPPO_VALUES = "hippo:values";

    public static final String HIPPO_WEB_FILE_BUNDLE = "hippo:webfilebundle";

    public static final String HIPPO_CONTENTPROPSET = "hippo:contentpropset";

    public static final String HIPPO_CONTENTPROPADD = "hippo:contentpropadd";

    public static final String HIPPO_STATUS = "hippo:status";

    public static final String HIPPO_ERRORMESSAGE = "hippo:errormessage";

    public static final String HIPPO_UPSTREAMITEMS = "hippo:upstreamitems";

    /** @deprecated replaced by {@link #HIPPO_LASTPROCESSEDTIME} */
    public static final String HIPPO_TIMESTAMP = "hippo:timestamp";

    public static final String HIPPO_LASTPROCESSEDTIME = "hippo:lastprocessedtime";

    /** @deprecated replaced by {@link #HIPPO_CONTEXTPATHS} */
    public static final String HIPPO_CONTEXTNODENAME = "hippo:contextnodename";

    public static final String HIPPOSYS_DESCRIPTION = "hipposys:description";

    public static final String HIPPOSYS_PATH = "hipposys:path";

    public static final String HIPPOSYS_QUERY = "hipposys:query";

    public static final String HIPPOSYS_LANGUAGE = "hipposys:language";

    public static final String HIPPOSYS_PARAMETERS = "hipposys:parameters";

    public static final String HIPPOSYS_SCRIPT = "hipposys:script";

    public static final String HIPPOSYS_CLASS = "hipposys:class";

    public static final String HIPPOSYS_REVERT = "hipposys:revert";

    public static final String HIPPOSYS_THROTTLE = "hipposys:throttle";

    public static final String HIPPOSYS_BATCHSIZE = "hipposys:batchsize";

    public static final String HIPPOSYS_DRYRUN = "hipposys:dryrun";

    public static final String HIPPOSYS_CANCELLED = "hipposys:cancelled";

    public static final String HIPPOSYS_STARTEDBY = "hipposys:startedby";

    public static final String HIPPOSYS_CANCELLEDBY = "hipposys:cancelledby";

    public static final String HIPPOSYS_STARTTIME = "hipposys:starttime";

    public static final String HIPPOSYS_FINISHTIME = "hipposys:finishtime";

    public static final String HIPPOSYS_UPDATED = "hipposys:updated";

    public static final String HIPPOSYS_FAILED = "hipposys:failed";

    public static final String HIPPOSYS_SKIPPED = "hipposys:skipped";

    public static final String HIPPOSYS_UPDATEDCOUNT = "hipposys:updatedcount";

    public static final String HIPPOSYS_FAILEDCOUNT = "hipposys:failedcount";

    public static final String HIPPOSYS_SKIPPEDCOUNT = "hipposys:skippedcount";

    public static final String HIPPOSYS_LOG = "hipposys:log";

    public static final String HIPPOSYS_LOGTAIL = "hipposys:logtail";

    public static final String HIPPOSYS_DELTADIRECTIVE = "hipposys:deltadirective";

    public static final String HIPPO_IGNORABLE = "hippo:ignorable";

    public static final String HIPPO_LOCKEXPIRATIONTIME = "hippo:lockExpirationTime";

    public static final String HIPPO_LOCK = "hippo:lock";

}
