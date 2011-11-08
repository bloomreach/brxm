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
package org.hippoecm.hst.configuration;

public interface HstNodeTypes {
 
    public static final String WILDCARD = "_default_";
    public static final String ANY = "_any_";
    
    public final static String NODETYPE_HST_SITES = "hst:sites";
    public final static String NODETYPE_HST_SITE = "hst:site";
    public final static String NODETYPE_HST_CONFIGURATIONS = "hst:configurations";
    public final static String NODETYPE_HST_CONFIGURATION = "hst:configuration";
    public final static String NODETYPE_HST_SITEMAP = "hst:sitemap";
    public final static String NODETYPE_HST_SITEMAPITEM = "hst:sitemapitem";
    public final static String NODETYPE_HST_COMPONENT = "hst:component";
    public final static String NODETYPE_HST_CONTAINERCOMPONENT = "hst:containercomponent";
    public final static String NODETYPE_HST_CONTAINERITEMCOMPONENT = "hst:containeritemcomponent";
    public final static String NODETYPE_HST_CATALOG = "hst:catalog";
    public final static String NODETYPE_HST_CONTAINERITEM_PACKAGE = "hst:containeritempackage";
    public final static String NODETYPE_HST_SITEMENUS = "hst:sitemenus";
    public final static String NODETYPE_HST_SITEMENU = "hst:sitemenu";
    public final static String NODETYPE_HST_SITEMENUITEM = "hst:sitemenuitem";
    public final static String NODETYPE_HST_VIRTUALHOSTS = "hst:virtualhosts";
    public final static String NODETYPE_HST_VIRTUALHOSTGROUP = "hst:virtualhostgroup";
    public final static String NODETYPE_HST_VIRTUALHOST = "hst:virtualhost";
    public final static String NODETYPE_HST_PORTMOUNT = "hst:portmount";
    public final static String NODETYPE_HST_MOUNT = "hst:mount";
    public final static String NODETYPE_HST_SITEMAPITEMHANDLERS = "hst:sitemapitemhandlers";
    public final static String NODETYPE_HST_SITEMAPITEMHANDLER = "hst:sitemapitemhandler";
    public final static String NODETYPE_HST_CONTENTMOUNT = "hst:contentmount";
    public final static String NODETYPE_HST_TEMPLATE = "hst:template";
    public final static String NODETYPE_HST_CHANNELINFO = "hst:channelinfo";
    public final static String NODETYPE_HST_CHANNEL = "hst:channel";
    public final static String NODETYPE_HST_CHANNELS = "hst:channels";
    public final static String NODETYPE_HST_BLUEPRINT = "hst:blueprint";
    public final static String NODETYPE_HST_BLUEPRINTS = "hst:blueprints";

    public final static String GENERAL_PROPERTY_INHERITS_FROM = "hst:inheritsfrom";
    public final static String GENERAL_PROPERTY_PARAMETER_NAMES = "hst:parameternames";
    public final static String GENERAL_PROPERTY_PARAMETER_VALUES = "hst:parametervalues";
    public final static String GENERAL_PROPERTY_HOMEPAGE = "hst:homepage";
    public final static String GENERAL_PROPERTY_PAGE_NOT_FOUND = "hst:pagenotfound";
    public final static String GENERAL_PROPERTY_LOCALE = "hst:locale";
    public final static String GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER =  "hst:versioninpreviewheader" ;
    
    public final static String COMPONENT_PROPERTY_COMPONENT_CONTEXTNAME = "hst:componentcontextname";
    public final static String COMPONENT_PROPERTY_COMPONENT_CLASSNAME = "hst:componentclassname";
    public final static String COMPONENT_PROPERTY_TEMPLATE = "hst:template";
    public final static String COMPONENT_PROPERTY_RESOURCE_TEMPLATE = "hst:resourcetemplate";
    public final static String COMPONENT_PROPERTY_XTYPE = "hst:xtype";
    public final static String COMPONENT_PROPERTY_REFERECENCENAME = "hst:referencename";
    public final static String COMPONENT_PROPERTY_REFERECENCECOMPONENT =  "hst:referencecomponent";
    public final static String COMPONENT_PROPERTY_CONTENTBASEPATH =  "hst:componentcontentbasepath";
    public final static String COMPONENT_PROPERTY_PAGE_ERROR_HANDLER_CLASSNAME =  "hst:page_errorhandlerclassname";
    public final static String COMPONENT_PROPERTY_STANDALONE = "hst:standalone";
    public final static String COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES = "hst:parameternameprefixes";
    /**
     * @deprecated unused
     */
    @Deprecated
    public final static String COMPONENT_PROPERTY_DUMMY_CONTENT = "hst:dummycontent";
    public final static String COMPONENT_PROPERTY_COMPONENT_FILTER_TAG = "hst:componentfiltertag";


    public final static String TEMPLATE_PROPERTY_RENDERPATH =  "hst:renderpath";
    public final static String TEMPLATE_PROPERTY_IS_NAMED =  "hst:isnamed";
    public final static String TEMPLATE_PROPERTY_SCRIPT = "hst:script";
    
    public final static String SITEMAPITEM_PROPERTY_REF_ID = "hst:refId";
    public final static String SITEMAPITEM_PROPERTY_VALUE =  "hst:value";
    public final static String SITEMAPITEM_PROPERTY_AUTHENTICATED =  "hst:authenticated";
    public final static String SITEMAPITEM_PROPERTY_ROLES =  "hst:roles";
    public final static String SITEMAPITEM_PROPERTY_USERS =  "hst:users";
    public final static String SITEMAPITEM_PROPERTY_STATUSCODE =  "hst:statuscode";
    public final static String SITEMAPITEM_PROPERTY_ERRORCODE =  "hst:errorcode";
    public final static String SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH =  "hst:relativecontentpath";
    public final static String SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID =  "hst:componentconfigurationid";
    public final static String SITEMAPITEM_PROPERTY_PORTLETCOMPONENTCONFIGURATIONID =  "hst:portletcomponentconfigurationid";
    public final static String SITEMAPITEM_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    public final static String SITEMAPITEM_PROPERTY_SITEMAPITEMHANDLERIDS =  "hst:sitemapitemhandlerids";
    public final static String SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING = "hst:excludedforlinkrewriting";

    public final static String SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM =  "hst:referencesitemapitem";
    public final static String SITEMENUITEM_PROPERTY_EXTERNALLINK =  "hst:externallink";
    public final static String SITEMENUITEM_PROPERTY_FOLDERSONLY =  "hst:foldersonly";
    public final static String SITEMENUITEM_PROPERTY_REPOBASED =  "hst:repobased";
    public final static String SITEMENUITEM_PROPERTY_DEPTH =  "hst:depth";

    public final static String VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS = "hst:prefixexclusions";
    public final static String VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS = "hst:suffixexclusions";
    public final static String VIRTUALHOSTS_PROPERTY_PORT = "hst:port";
    public final static String VIRTUALHOSTS_PROPERTY_SCHEME = "hst:scheme";
    public final static String VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME = "hst:defaulthostname";
    public final static String VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    public final static String VIRTUALHOSTS_PROPERTY_DEFAULTCONTEXTPATH = "hst:defaultcontextpath";
    public final static String VIRTUALHOSTS_PROPERTY_SHOWPORT = "hst:showport";
    
    public final static String VIRTUALHOSTGROUP_PROPERTY_CMS_LOCATION =  "hst:cmslocation" ;
    

    public final static String VIRTUALHOST_PROPERTY_SCHEME = "hst:scheme";
    public final static String VIRTUALHOST_PROPERTY_SITENAME = "hst:sitename";
    public final static String VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    public final static String VIRTUALHOST_PROPERTY_ONLYFORCONTEXTPATH = "hst:onlyforcontextpath";
    public final static String VIRTUALHOST_PROPERTY_SHOWPORT = "hst:showport";
    
    public final static String MOUNT_HST_ROOTNAME = "hst:root";
    public final static String MOUNT_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    public final static String MOUNT_PROPERTY_ONLYFORCONTEXTPATH = "hst:onlyforcontextpath";
    public final static String MOUNT_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    public final static String MOUNT_PROPERTY_SHOWPORT = "hst:showport";
    public final static String MOUNT_PROPERTY_SCHEME = "hst:scheme";
    public final static String MOUNT_PROPERTY_MOUNTPOINT = "hst:mountpoint";
    public final static String MOUNT_PROPERTY_ISMAPPED = "hst:ismapped";
    public final static String MOUNT_PROPERTY_IS_SITE = "hst:isSite";
    
    public final static String MOUNT_PROPERTY_ALIAS = "hst:alias";
    public final static String MOUNT_PROPERTY_TYPE = "hst:type";
    public final static String MOUNT_PROPERTY_TYPES = "hst:types";
    public final static String MOUNT_PROPERTY_EMBEDDEDMOUNTPATH = "hst:embeddedmountpath";
    public final static String MOUNT_PROPERTY_AUTHENTICATED =  "hst:authenticated";
    public final static String MOUNT_PROPERTY_ROLES =  "hst:roles";
    public final static String MOUNT_PROPERTY_USERS =  "hst:users";
    public final static String MOUNT_PROPERTY_SUBJECTBASEDSESSION =  "hst:subjectbasedsession";
    public final static String MOUNT_PROPERTY_SESSIONSTATEFUL =  "hst:sessionstateful";
    public final static String MOUNT_PROPERTY_FORMLOGINPAGE =  "hst:formloginpage";
    public final static String MOUNT_PROPERTY_DEFAULTSITEMAPITEMHANDLERIDS =  "hst:defaultsitemapitemhandlerids";
    public final static String MOUNT_PROPERTY_CHANNELPATH =  "hst:channelpath";

    public final static String CHANNEL_PROPERTY_NAME = "hst:name";
    public final static String CHANNEL_PROPERTY_CHANNELINFO_CLASS = "hst:channelinfoclass";

    public final static String BLUEPRINT_PROPERTY_NAME = "hst:name";
    public final static String BLUEPRINT_PROPERTY_DESCRIPTION = "hst:description";
    public final static String BLUEPRINT_PROPERTY_CONTENT_ROOT = "hst:contentRoot";

    public final static String SITEMAPITEMHANDLDER_PROPERTY_CLASSNAME = "hst:sitemapitemhandlerclassname";
    
    public final static String SITE_CONFIGURATIONPATH = "hst:configurationpath";
    
    public final static String NODENAME_HST_CONTENTNODE = "hst:content";
    public final static String NODENAME_HST_SITEMAP = "hst:sitemap";
    public final static String NODENAME_HST_SITEMENUS = "hst:sitemenus";
    public final static String NODENAME_HST_COMPONENTS = "hst:components";
    public final static String NODENAME_HST_CATALOG = "hst:catalog";
    public final static String NODENAME_HST_PAGES = "hst:pages";
    public final static String NODENAME_HST_TEMPLATES = "hst:templates";
    public final static String NODENAME_HST_SITEMAPITEMHANDLERS = "hst:sitemapitemhandlers";
    public final static String NODENAME_HST_HSTDEFAULT = "hst:default";
    public final static String NODENAME_HST_BLUEPRINTS = "hst:blueprints";
    public final static String NODENAME_HST_CHANNELINFO = "hst:channelinfo";
    public final static String NODENAME_HST_SITE = "hst:site";
    public final static String NODENAME_HST_CHANNEL = "hst:channel";
    public final static String NODENAME_HST_CONFIGURATION = "hst:configuration";
    public final static String NODENAME_HST_CONFIGURATIONS = "hst:configurations";
    public final static String NODENAME_HST_MOUNT = "hst:mount";
    public final static String NODENAME_HST_HOSTS = "hst:hosts";
    public final static String NODENAME_HST_CHANNELS = "hst:channels";
}
