/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

public interface HstNodeTypes {

    String WILDCARD = "_default_";
    String ANY = "_any_";

    String NODETYPE_HST_SITES = "hst:sites";
    String NODETYPE_HST_SITE = "hst:site";
    String NODETYPE_HST_CONFIGURATIONS = "hst:configurations";
    String NODETYPE_HST_CONFIGURATION = "hst:configuration";
    String NODETYPE_HST_SITEMAP = "hst:sitemap";
    String NODETYPE_HST_SITEMAPITEM = "hst:sitemapitem";
    String NODETYPE_HST_ABSTRACT_COMPONENT = "hst:abstractcomponent";
    String NODETYPE_HST_COMPONENT = "hst:component";
    String NODETYPE_HST_CONTAINERCOMPONENTREFERENCE = "hst:containercomponentreference";
    String NODETYPE_HST_CONTAINERCOMPONENT = "hst:containercomponent";
    String NODETYPE_HST_CONTAINERITEMCOMPONENT = "hst:containeritemcomponent";
    String NODETYPE_HST_CATALOG = "hst:catalog";
    String NODETYPE_HST_WORKSPACE = "hst:workspace";
    String NODETYPE_HST_CONTAINERITEM_PACKAGE = "hst:containeritempackage";
    String NODETYPE_HST_SITEMENUS = "hst:sitemenus";
    String NODETYPE_HST_SITEMENU = "hst:sitemenu";
    String NODETYPE_HST_SITEMENUITEM = "hst:sitemenuitem";
    String NODETYPE_HST_VIRTUALHOSTS = "hst:virtualhosts";
    String NODETYPE_HST_VIRTUALHOSTGROUP = "hst:virtualhostgroup";
    String NODETYPE_HST_VIRTUALHOST = "hst:virtualhost";
    String NODETYPE_HST_PORTMOUNT = "hst:portmount";
    String NODETYPE_HST_MOUNT = "hst:mount";
    String NODETYPE_HST_SITEMAPITEMHANDLERS = "hst:sitemapitemhandlers";
    String NODETYPE_HST_SITEMAPITEMHANDLER = "hst:sitemapitemhandler";
    String NODETYPE_HST_CONTENTMOUNT = "hst:contentmount";
    String NODETYPE_HST_TEMPLATE = "hst:template";
    String NODETYPE_HST_CHANNELINFO = "hst:channelinfo";
    String NODETYPE_HST_CHANNEL = "hst:channel";
    String NODETYPE_HST_CHANNELS = "hst:channels";
    String NODETYPE_HST_BLUEPRINT = "hst:blueprint";
    String NODETYPE_HST_BLUEPRINTS = "hst:blueprints";
    String NODETYPE_HST_CONTAINERCOMPONENTSFOLDER = "hst:containercomponentfolder";

    String MIXINTYPE_HST_EDITABLE = "hst:editable";
    String EDITABLE_PROPERTY_STATE = "hst:state";

    String MIXINTYPE_HST_PROTOTYPE_META = "hst:prototypemeta";
    String PROTOTYPE_META_PROPERTY_PRIMARY_CONTAINER = "hst:primarycontainer";

    String GENERAL_PROPERTY_INHERITS_FROM = "hst:inheritsfrom";
    String GENERAL_PROPERTY_LOCKED_BY = "hst:lockedby";
    String GENERAL_PROPERTY_LOCKED_ON = "hst:lockedon";
    String GENERAL_PROPERTY_LAST_MODIFIED = "hst:lastmodified";
    String GENERAL_PROPERTY_LAST_MODIFIED_BY = "hst:lastmodifiedby";
    String GENERAL_PROPERTY_PARAMETER_NAMES = "hst:parameternames";
    String GENERAL_PROPERTY_PARAMETER_VALUES = "hst:parametervalues";
    String GENERAL_PROPERTY_HOMEPAGE = "hst:homepage";
    String GENERAL_PROPERTY_PAGE_NOT_FOUND = "hst:pagenotfound";
    String GENERAL_PROPERTY_LOCALE = "hst:locale";
    String GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER = "hst:versioninpreviewheader";
    String GENERAL_PROPERTY_CACHEABLE = "hst:cacheable";
    String GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID = "hst:defaultresourcebundleid";
    String GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE = "hst:schemenotmatchresponsecode";
    String GENERAL_PROEPRTY_SCHEME_AGNOSTIC = "hst:schemeagnostic";
    String VIRTUALHOST_PROPERTY_CUSTOM_HTTPS_SUPPORT = "hst:customhttpssupport";

    String COMPONENT_PROPERTY_COMPONENT_CONTEXTNAME = "hst:componentcontextname";
    String COMPONENT_PROPERTY_COMPONENT_CLASSNAME = "hst:componentclassname";
    String COMPONENT_PROPERTY_TEMPLATE = "hst:template";
    String COMPONENT_PROPERTY_RESOURCE_TEMPLATE = "hst:resourcetemplate";
    String COMPONENT_PROPERTY_XTYPE = "hst:xtype";
    String COMPONENT_PROPERTY_REFERECENCENAME = "hst:referencename";
    String COMPONENT_PROPERTY_REFERECENCECOMPONENT = "hst:referencecomponent";
    String COMPONENT_PROPERTY_CONTENTBASEPATH = "hst:componentcontentbasepath";
    String COMPONENT_PROPERTY_PAGE_ERROR_HANDLER_CLASSNAME = "hst:page_errorhandlerclassname";
    String COMPONENT_PROPERTY_STANDALONE = "hst:standalone";
    String COMPONENT_PROPERTY_ASYNC = "hst:async";
    String COMPONENT_PROPERTY_ASYNC_MODE = "hst:asyncmode";
    String COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES = "hst:parameternameprefixes";
    String COMPONENT_PROPERTY_ICON_PATH = "hst:iconpath";
    String COMPONENT_PROPERTY_LABEL = "hst:label";
    String COMPONENT_PROPERTY_COMPONENT_FILTER_TAG = "hst:componentfiltertag";

    String TEMPLATE_PROPERTY_RENDERPATH = "hst:renderpath";
    String TEMPLATE_PROPERTY_IS_NAMED = "hst:isnamed";
    String TEMPLATE_PROPERTY_SCRIPT = "hst:script";

    String SITEMAPITEM_PROPERTY_REF_ID = "hst:refId";
    String SITEMAPITEM_PROPERTY_VALUE = "hst:value";
    String SITEMAPITEM_PROPERTY_AUTHENTICATED = "hst:authenticated";
    String SITEMAPITEM_PROPERTY_ROLES = "hst:roles";
    String SITEMAPITEM_PROPERTY_USERS = "hst:users";
    String SITEMAPITEM_PROPERTY_STATUSCODE = "hst:statuscode";
    String SITEMAPITEM_PROPERTY_ERRORCODE = "hst:errorcode";
    String SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH = "hst:relativecontentpath";
    String SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID = "hst:componentconfigurationid";

    String SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_NAMES = "hst:componentconfigurationmappingnames";
    String SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_VALUES = "hst:componentconfigurationmappingvalues";
    String SITEMAPITEM_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    String SITEMAPITEM_PROPERTY_SITEMAPITEMHANDLERIDS = "hst:sitemapitemhandlerids";
    String SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING = "hst:excludedforlinkrewriting";
    String SITEMAPITEM_PROPERTY_SCHEME = "hst:scheme";
    String SITEMAPITEM_PROPERTY_RESOURCE_BUNDLE_ID = "hst:resourcebundleid";
    String SITEMAPITEM_PAGE_TITLE = "hst:pagetitle";

    String SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM = "hst:referencesitemapitem";
    String SITEMENUITEM_PROPERTY_EXTERNALLINK = "hst:externallink";
    String SITEMENUITEM_PROPERTY_FOLDERSONLY = "hst:foldersonly";
    String SITEMENUITEM_PROPERTY_REPOBASED = "hst:repobased";
    String SITEMENUITEM_PROPERTY_DEPTH = "hst:depth";
    String SITEMENUITEM_PROPERTY_MOUNTALIAS = "hst:mountalias";
    String SITEMENUITEM_PROPERTY_ROLES = "hst:roles";

    String VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS = "hst:prefixexclusions";
    String VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS = "hst:suffixexclusions";
    String VIRTUALHOSTS_PROPERTY_PORT = "hst:port";
    String VIRTUALHOSTS_PROPERTY_SCHEME = "hst:scheme";
    String VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME = "hst:defaulthostname";
    String VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    String VIRTUALHOSTS_PROPERTY_SHOWPORT = "hst:showport";
    String VIRTUALHOSTS_PROPERTY_DEFAULTCONTEXTPATH = "hst:defaultcontextpath";
    String VIRTUALHOSTS_PROPERTY_CMSPREVIEWPREFIX = "hst:cmspreviewprefix";
    String VIRTUALHOSTS_PROPERTY_DIAGNOSTISC_ENABLED = "hst:diagnosticsenabled";
    String VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_FOR_IPS = "hst:diagnosticsforips";
    String VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_HOSTGROUP = "hst:channelmanagerhostgroup";
    String VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITES = "hst:channelmanagersites";
    String VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITE_AUTHENTICATION_SKIPPED = "hst:channelmanagersiteauthenticationskipped";

    String VIRTUALHOSTGROUP_PROPERTY_CMS_LOCATION = "hst:cmslocation";
    String VIRTUALHOSTGROUP_PROPERTY_DEFAULT_PORT = "hst:defaultport";

    String VIRTUALHOST_PROPERTY_SCHEME = "hst:scheme";
    String VIRTUALHOST_PROPERTY_SITENAME = "hst:sitename";
    String VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    String VIRTUALHOST_PROPERTY_ONLYFORCONTEXTPATH = "hst:onlyforcontextpath";
    String VIRTUALHOST_PROPERTY_SHOWPORT = "hst:showport";

    String MOUNT_HST_ROOTNAME = "hst:root";
    String MOUNT_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    String MOUNT_PROPERTY_ONLYFORCONTEXTPATH = "hst:onlyforcontextpath";
    String MOUNT_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    String MOUNT_PROPERTY_SHOWPORT = "hst:showport";
    String MOUNT_PROPERTY_SCHEME = "hst:scheme";
    String MOUNT_PROPERTY_MOUNTPOINT = "hst:mountpoint";
    String MOUNT_PROPERTY_ISMAPPED = "hst:ismapped";
    String MOUNT_PROPERTY_IS_SITE = "hst:isSite";

    String MOUNT_PROPERTY_ALIAS = "hst:alias";
    String MOUNT_PROPERTY_TYPE = "hst:type";
    String MOUNT_PROPERTY_TYPES = "hst:types";
    String MOUNT_PROPERTY_AUTHENTICATED = "hst:authenticated";
    String MOUNT_PROPERTY_ROLES = "hst:roles";
    String MOUNT_PROPERTY_USERS = "hst:users";
    String MOUNT_PROPERTY_SUBJECTBASEDSESSION = "hst:subjectbasedsession";
    String MOUNT_PROPERTY_SESSIONSTATEFUL = "hst:sessionstateful";
    String MOUNT_PROPERTY_FORMLOGINPAGE = "hst:formloginpage";
    String MOUNT_PROPERTY_DEFAULTSITEMAPITEMHANDLERIDS = "hst:defaultsitemapitemhandlerids";
    String MOUNT_PROPERTY_CHANNELPATH = "hst:channelpath";

    String CHANNEL_PROPERTY_NAME = "hst:name";
    String CHANNEL_PROPERTY_TYPE = "hst:type";
    String CHANNEL_PROPERTY_DEFAULT_DEVICE = "hst:defaultdevice";
    String CHANNEL_PROPERTY_DEVICES = "hst:devices";

    String CHANNEL_PROPERTY_CHANNELINFO_CLASS = "hst:channelinfoclass";

    String BLUEPRINT_PROPERTY_NAME = "hst:name";
    String BLUEPRINT_PROPERTY_DESCRIPTION = "hst:description";
    String BLUEPRINT_PROPERTY_CONTENT_ROOT = "hst:contentRoot";

    String SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME = "hst:sitemapitemhandlerclassname";

    String SITE_CONFIGURATIONPATH = "hst:configurationpath";
    String SITE_VERSION = "hst:version";
    String SITE_CONTENT = "hst:content";

    String NODENAME_HST_CONTENTNODE = "hst:content";
    String NODENAME_HST_SITEMAP = "hst:sitemap";
    String NODENAME_HST_SITEMENUS = "hst:sitemenus";
    String NODENAME_HST_COMPONENTS = "hst:components";
    String NODENAME_HST_CATALOG = "hst:catalog";
    String NODENAME_HST_PAGES = "hst:pages";
    String NODENAME_HST_ABSTRACTPAGES = "hst:abstractpages";
    String NODENAME_HST_PROTOTYPEPAGES = "hst:prototypepages";
    String NODENAME_HST_TEMPLATES = "hst:templates";
    String NODENAME_HST_SITEMAPITEMHANDLERS = "hst:sitemapitemhandlers";
    String NODENAME_HST_HSTDEFAULT = "hst:default";
    String NODENAME_HST_BLUEPRINTS = "hst:blueprints";
    String NODENAME_HST_CHANNELINFO = "hst:channelinfo";
    String NODENAME_HST_SITE = "hst:site";
    String NODENAME_HST_CHANNEL = "hst:channel";
    String NODENAME_HST_CONFIGURATION = "hst:configuration";
    String NODENAME_HST_CONFIGURATIONS = "hst:configurations";
    String NODENAME_HST_MOUNT = "hst:mount";
    String NODENAME_HST_HOSTS = "hst:hosts";
    String NODENAME_HST_CHANNELS = "hst:channels";
    String NODENAME_HST_WORKSPACE = "hst:workspace";
    String NODENAME_HST_CONTAINERS = "hst:containers";

    String RELPATH_HST_WORKSPACE_CONTAINERS = "hst:workspace/hst:containers";

}
