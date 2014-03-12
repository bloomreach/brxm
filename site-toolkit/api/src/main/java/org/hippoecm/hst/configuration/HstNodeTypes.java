/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
 
    static final String WILDCARD = "_default_";
    static final String ANY = "_any_";
    
    static final String NODETYPE_HST_SITES = "hst:sites";
    static final String NODETYPE_HST_SITE = "hst:site";
    static final String NODETYPE_HST_CONFIGURATIONS = "hst:configurations";
    static final String NODETYPE_HST_CONFIGURATION = "hst:configuration";
    static final String NODETYPE_HST_SITEMAP = "hst:sitemap";
    static final String NODETYPE_HST_SITEMAPITEM = "hst:sitemapitem";
    static final String NODETYPE_HST_ABSTRACT_COMPONENT = "hst:abstractcomponent";
    static final String NODETYPE_HST_COMPONENT = "hst:component";
    static final String NODETYPE_HST_CONTAINERCOMPONENTREFERENCE = "hst:containercomponentreference";
    static final String NODETYPE_HST_CONTAINERCOMPONENT = "hst:containercomponent";
    static final String NODETYPE_HST_CONTAINERITEMCOMPONENT = "hst:containeritemcomponent";
    static final String NODETYPE_HST_CATALOG = "hst:catalog";
    static final String NODETYPE_HST_WORKSPACE = "hst:workspace";
    static final String NODETYPE_HST_CONTAINERITEM_PACKAGE = "hst:containeritempackage";
    static final String NODETYPE_HST_SITEMENUS = "hst:sitemenus";
    static final String NODETYPE_HST_SITEMENU = "hst:sitemenu";
    static final String NODETYPE_HST_SITEMENUITEM = "hst:sitemenuitem";
    static final String NODETYPE_HST_VIRTUALHOSTS = "hst:virtualhosts";
    static final String NODETYPE_HST_VIRTUALHOSTGROUP = "hst:virtualhostgroup";
    static final String NODETYPE_HST_VIRTUALHOST = "hst:virtualhost";
    static final String NODETYPE_HST_PORTMOUNT = "hst:portmount";
    static final String NODETYPE_HST_MOUNT = "hst:mount";
    static final String NODETYPE_HST_SITEMAPITEMHANDLERS = "hst:sitemapitemhandlers";
    static final String NODETYPE_HST_SITEMAPITEMHANDLER = "hst:sitemapitemhandler";
    static final String NODETYPE_HST_CONTENTMOUNT = "hst:contentmount";
    static final String NODETYPE_HST_TEMPLATE = "hst:template";
    static final String NODETYPE_HST_CHANNELINFO = "hst:channelinfo";
    static final String NODETYPE_HST_CHANNEL = "hst:channel";
    static final String NODETYPE_HST_CHANNELS = "hst:channels";
    static final String NODETYPE_HST_BLUEPRINT = "hst:blueprint";
    static final String NODETYPE_HST_BLUEPRINTS = "hst:blueprints";
    static final String NODETYPE_HST_CONTAINERCOMPONENTSFOLDER = "hst:containercomponentfolder";

    static final String MIXINTYPE_HST_EDITABLE = "hst:editable";
    static final String EDITABLE_PROPERTY_STATE = "hst:state";

    static final String GENERAL_PROPERTY_INHERITS_FROM = "hst:inheritsfrom";
    static final String GENERAL_PROPERTY_LOCKED_BY = "hst:lockedby";
    static final String GENERAL_PROPERTY_LOCKED_ON = "hst:lockedon";
    static final String GENERAL_PROPERTY_LAST_MODIFIED = "hst:lastmodified";
    static final String GENERAL_PROPERTY_LAST_MODIFIED_BY = "hst:lastmodifiedby";
    static final String GENERAL_PROPERTY_PARAMETER_NAMES = "hst:parameternames";
    static final String GENERAL_PROPERTY_PARAMETER_VALUES = "hst:parametervalues";
    static final String GENERAL_PROPERTY_HOMEPAGE = "hst:homepage";
    static final String GENERAL_PROPERTY_PAGE_NOT_FOUND = "hst:pagenotfound";
    static final String GENERAL_PROPERTY_LOCALE = "hst:locale";
    static final String GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER =  "hst:versioninpreviewheader" ;
    static final String GENERAL_PROPERTY_CACHEABLE = "hst:cacheable";
    static final String GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID = "hst:defaultresourcebundleid";
    static final String GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE = "hst:schemenotmatchresponsecode";
    static final String GENERAL_PROEPRTY_SCHEME_AGNOSTIC = "hst:schemeagnostic";
    static final String VIRTUALHOST_PROPERTY_CUSTOM_HTTPS_SUPPORT= "hst:customhttpssupport";
    
    static final String COMPONENT_PROPERTY_COMPONENT_CONTEXTNAME = "hst:componentcontextname";
    static final String COMPONENT_PROPERTY_COMPONENT_CLASSNAME = "hst:componentclassname";
    static final String COMPONENT_PROPERTY_TEMPLATE = "hst:template";
    static final String COMPONENT_PROPERTY_RESOURCE_TEMPLATE = "hst:resourcetemplate";
    static final String COMPONENT_PROPERTY_XTYPE = "hst:xtype";
    static final String COMPONENT_PROPERTY_REFERECENCENAME = "hst:referencename";
    static final String COMPONENT_PROPERTY_REFERECENCECOMPONENT =  "hst:referencecomponent";
    static final String COMPONENT_PROPERTY_CONTENTBASEPATH =  "hst:componentcontentbasepath";
    static final String COMPONENT_PROPERTY_PAGE_ERROR_HANDLER_CLASSNAME =  "hst:page_errorhandlerclassname";
    static final String COMPONENT_PROPERTY_STANDALONE = "hst:standalone";
    static final String COMPONENT_PROPERTY_ASYNC = "hst:async";
    static final String COMPONENT_PROPERTY_ASYNC_MODE = "hst:asyncmode";
    static final String COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES = "hst:parameternameprefixes";
    static final String COMPONENT_PROPERTY_ICON_PATH = "hst:iconpath";
    static final String COMPONENT_PROPERTY_LABEL = "hst:label";
    static final String COMPONENT_PROPERTY_COMPONENT_FILTER_TAG = "hst:componentfiltertag";

    static final String TEMPLATE_PROPERTY_RENDERPATH =  "hst:renderpath";
    static final String TEMPLATE_PROPERTY_IS_NAMED =  "hst:isnamed";
    static final String TEMPLATE_PROPERTY_SCRIPT = "hst:script";
    
    static final String SITEMAPITEM_PROPERTY_REF_ID = "hst:refId";
    static final String SITEMAPITEM_PROPERTY_VALUE =  "hst:value";
    static final String SITEMAPITEM_PROPERTY_AUTHENTICATED =  "hst:authenticated";
    static final String SITEMAPITEM_PROPERTY_ROLES =  "hst:roles";
    static final String SITEMAPITEM_PROPERTY_USERS =  "hst:users";
    static final String SITEMAPITEM_PROPERTY_STATUSCODE =  "hst:statuscode";
    static final String SITEMAPITEM_PROPERTY_ERRORCODE =  "hst:errorcode";
    static final String SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH =  "hst:relativecontentpath";
    static final String SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID =  "hst:componentconfigurationid";

    static final String SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_NAMES = "hst:componentconfigurationmappingnames";
    static final String SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_VALUES = "hst:componentconfigurationmappingvalues";
    static final String SITEMAPITEM_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    static final String SITEMAPITEM_PROPERTY_SITEMAPITEMHANDLERIDS =  "hst:sitemapitemhandlerids";
    static final String SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING = "hst:excludedforlinkrewriting";
    static final String SITEMAPITEM_PROPERTY_SCHEME = "hst:scheme";
    static final String SITEMAPITEM_PROPERTY_RESOURCE_BUNDLE_ID = "hst:resourcebundleid";
    static final String SITEMAPITEM_PAGE_TITLE = "hst:pagetitle";

    static final String SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM =  "hst:referencesitemapitem";
    static final String SITEMENUITEM_PROPERTY_EXTERNALLINK =  "hst:externallink";
    static final String SITEMENUITEM_PROPERTY_FOLDERSONLY =  "hst:foldersonly";
    static final String SITEMENUITEM_PROPERTY_REPOBASED =  "hst:repobased";
    static final String SITEMENUITEM_PROPERTY_DEPTH =  "hst:depth";
    static final String SITEMENUITEM_PROPERTY_MOUNTALIAS = "hst:mountalias";
    static final String SITEMENUITEM_PROPERTY_ROLES =  "hst:roles";

    static final String VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS = "hst:prefixexclusions";
    static final String VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS = "hst:suffixexclusions";
    static final String VIRTUALHOSTS_PROPERTY_PORT = "hst:port";
    static final String VIRTUALHOSTS_PROPERTY_SCHEME = "hst:scheme";
    static final String VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME = "hst:defaulthostname";
    static final String VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    static final String VIRTUALHOSTS_PROPERTY_SHOWPORT = "hst:showport";
    static final String VIRTUALHOSTS_PROPERTY_DEFAULTCONTEXTPATH = "hst:defaultcontextpath";
    static final String VIRTUALHOSTS_PROPERTY_CMSPREVIEWPREFIX = "hst:cmspreviewprefix";
    static final String VIRTUALHOSTS_PROPERTY_DIAGNOSTISC_ENABLED = "hst:diagnosticsenabled";
    static final String VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_FOR_IPS = "hst:diagnosticsforips";
    static final String VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_HOSTGROUP = "hst:channelmanagerhostgroup";
    static final String VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITES = "hst:channelmanagersites";
    static final String VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITE_AUTHENTICATION_SKIPPED = "hst:channelmanagersiteauthenticationskipped";

    static final String VIRTUALHOSTGROUP_PROPERTY_CMS_LOCATION =  "hst:cmslocation" ;
    static final String VIRTUALHOSTGROUP_PROPERTY_DEFAULT_PORT = "hst:defaultport";


    static final String VIRTUALHOST_PROPERTY_SCHEME = "hst:scheme";
    static final String VIRTUALHOST_PROPERTY_SITENAME = "hst:sitename";
    static final String VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    static final String VIRTUALHOST_PROPERTY_ONLYFORCONTEXTPATH = "hst:onlyforcontextpath";
    static final String VIRTUALHOST_PROPERTY_SHOWPORT = "hst:showport";
    
    static final String MOUNT_HST_ROOTNAME = "hst:root";
    static final String MOUNT_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    static final String MOUNT_PROPERTY_ONLYFORCONTEXTPATH = "hst:onlyforcontextpath";
    static final String MOUNT_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    static final String MOUNT_PROPERTY_SHOWPORT = "hst:showport";
    static final String MOUNT_PROPERTY_SCHEME = "hst:scheme";
    static final String MOUNT_PROPERTY_MOUNTPOINT = "hst:mountpoint";
    static final String MOUNT_PROPERTY_ISMAPPED = "hst:ismapped";
    static final String MOUNT_PROPERTY_IS_SITE = "hst:isSite";
    
    static final String MOUNT_PROPERTY_ALIAS = "hst:alias";
    static final String MOUNT_PROPERTY_TYPE = "hst:type";
    static final String MOUNT_PROPERTY_TYPES = "hst:types";
    static final String MOUNT_PROPERTY_AUTHENTICATED =  "hst:authenticated";
    static final String MOUNT_PROPERTY_ROLES =  "hst:roles";
    static final String MOUNT_PROPERTY_USERS =  "hst:users";
    static final String MOUNT_PROPERTY_SUBJECTBASEDSESSION =  "hst:subjectbasedsession";
    static final String MOUNT_PROPERTY_SESSIONSTATEFUL =  "hst:sessionstateful";
    static final String MOUNT_PROPERTY_FORMLOGINPAGE =  "hst:formloginpage";
    static final String MOUNT_PROPERTY_DEFAULTSITEMAPITEMHANDLERIDS =  "hst:defaultsitemapitemhandlerids";
    static final String MOUNT_PROPERTY_CHANNELPATH =  "hst:channelpath";

    static final String CHANNEL_PROPERTY_NAME = "hst:name";
    static final String CHANNEL_PROPERTY_TYPE = "hst:type";
    static final String CHANNEL_PROPERTY_DEFAULT_DEVICE = "hst:defaultdevice";
    static final String CHANNEL_PROPERTY_DEVICES = "hst:devices";

    static final String CHANNEL_PROPERTY_CHANNELINFO_CLASS = "hst:channelinfoclass";

    static final String BLUEPRINT_PROPERTY_NAME = "hst:name";
    static final String BLUEPRINT_PROPERTY_DESCRIPTION = "hst:description";
    static final String BLUEPRINT_PROPERTY_CONTENT_ROOT = "hst:contentRoot";

    static final String SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME = "hst:sitemapitemhandlerclassname";

    static final String SITE_CONFIGURATIONPATH = "hst:configurationpath";
    static final String SITE_VERSION = "hst:version";
    static final String SITE_CONTENT = "hst:content";
    
    static final String NODENAME_HST_CONTENTNODE = "hst:content";
    static final String NODENAME_HST_SITEMAP = "hst:sitemap";
    static final String NODENAME_HST_SITEMENUS = "hst:sitemenus";
    static final String NODENAME_HST_COMPONENTS = "hst:components";
    static final String NODENAME_HST_CATALOG = "hst:catalog";
    static final String NODENAME_HST_PAGES = "hst:pages";
    static final String NODENAME_HST_ABSTRACTPAGES = "hst:abstractpages";
    static final String NODENAME_HST_PROTOTYPEPAGES = "hst:prototypepages";
    static final String NODENAME_HST_TEMPLATES = "hst:templates";
    static final String NODENAME_HST_SITEMAPITEMHANDLERS = "hst:sitemapitemhandlers";
    static final String NODENAME_HST_HSTDEFAULT = "hst:default";
    static final String NODENAME_HST_BLUEPRINTS = "hst:blueprints";
    static final String NODENAME_HST_CHANNELINFO = "hst:channelinfo";
    static final String NODENAME_HST_SITE = "hst:site";
    static final String NODENAME_HST_CHANNEL = "hst:channel";
    static final String NODENAME_HST_CONFIGURATION = "hst:configuration";
    static final String NODENAME_HST_CONFIGURATIONS = "hst:configurations";
    static final String NODENAME_HST_MOUNT = "hst:mount";
    static final String NODENAME_HST_HOSTS = "hst:hosts";
    static final String NODENAME_HST_CHANNELS = "hst:channels";
    static final String NODENAME_HST_WORKSPACE = "hst:workspace";
    static final String NODENAME_HST_CONTAINERS = "hst:containers";

    static final String RELPATH_HST_WORKSPACE_CONTAINERS = "hst:workspace/hst:containers";
}
