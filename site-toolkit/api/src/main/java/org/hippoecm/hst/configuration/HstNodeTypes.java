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
    public final static String NODETYPE_HST_CONFIGURATION = "hst:configuration";
    public final static String NODETYPE_HST_SITEMAP = "hst:sitemap";
    public final static String NODETYPE_HST_SITEMAPITEM = "hst:sitemapitem";
    public final static String NODETYPE_HST_COMPONENT = "hst:component";
    public final static String NODETYPE_HST_SITEMENUS = "hst:sitemenus";
    public final static String NODETYPE_HST_SITEMENU = "hst:sitemenu";
    public final static String NODETYPE_HST_SITEMENUITEM = "hst:sitemenuitem";
    public final static String NODETYPE_HST_VIRTUALHOSTS = "hst:virtualhosts";
    public final static String NODETYPE_HST_VIRTUALHOST = "hst:virtualhost";
    public final static String NODETYPE_HST_SITEMOUNT = "hst:sitemount";
    public final static String NODETYPE_HST_SCRIPT = "hst:script";

    public final static String COMPONENT_PROPERTY_COMPONENT_CONTEXTNAME = "hst:componentcontextname";
    public final static String COMPONENT_PROPERTY_COMPONENT_CLASSNAME = "hst:componentclassname";
    public final static String COMPONENT_PROPERTY_TEMPLATE_ = "hst:template";
    public final static String COMPONENT_PROPERTY_SERVE_RESOURCE_PATH = "hst:serveresourcepath";
    public final static String COMPONENT_PROPERTY_REFERECENCENAME = "hst:referencename";
    public final static String COMPONENT_PROPERTY_REFERECENCECOMPONENT =  "hst:referencecomponent";
    public final static String COMPONENT_PROPERTY_CONTENTBASEPATH =  "hst:componentcontentbasepath";

    public final static String COMPONENT_PROPERTY_PARAMETER_NAMES = "hst:parameternames";
    public final static String COMPONENT_PROPERTY_PARAMETER_VALUES = "hst:parametervalues";
    

    public final static String TEMPLATE_PROPERTY_RENDERPATH =  "hst:renderpath";
    public final static String SCRIPT_PROPERTY_TEMPLATE =  "hst:template";
    
    public final static String SITEMAPITEM_PROPERTY_VALUE =  "hst:value";
    public final static String SITEMAPITEM_PROPERTY_ROLES =  "hst:roles";
    public final static String SITEMAPITEM_PROPERTY_SECURED =  "hst:secured";
    public final static String SITEMAPITEM_PROPERTY_STATUSCODE =  "hst:statuscode";
    public final static String SITEMAPITEM_PROPERTY_ERRORCODE =  "hst:errorcode";
    public final static String SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH =  "hst:relativecontentpath";
    public final static String SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID =  "hst:componentconfigurationid";
    public final static String SITEMAPITEM_PROPERTY_PORTLETCOMPONENTCONFIGURATIONID =  "hst:portletcomponentconfigurationid";

    public final static String SITEMAPITEM_PROPERTY_PARAMETER_NAMES = "hst:parameternames";
    public final static String SITEMAPITEM_PROPERTY_PARAMETER_VALUES = "hst:parametervalues";
    

    public final static String SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM =  "hst:referencesitemapitem";
    public final static String SITEMENUITEM_PROPERTY_EXTERNALLINK =  "hst:externallink";
    public final static String SITEMENUITEM_PROPERTY_FOLDERSONLY =  "hst:foldersonly";
    public final static String SITEMENUITEM_PROPERTY_REPOBASED =  "hst:repobased";
    public final static String SITEMENUITEM_PROPERTY_DEPTH =  "hst:depth";

    public final static String VIRTUALHOSTS_PROPERTY_SHOWPORT = "hst:showport";
    public final static String VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS = "hst:prefixexclusions";
    public final static String VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS = "hst:suffixexclusions";
    public final static String VIRTUALHOSTS_PROPERTY_PORT = "hst:port";
    public final static String VIRTUALHOSTS_PROPERTY_PROTOCOL = "hst:protocol";
    public final static String VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME = "hst:defaulthostname";
    public final static String VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";

    public final static String VIRTUALHOST_PROPERTY_SHOWPORT = "hst:showport";
    public final static String VIRTUALHOST_PROPERTY_PORT = "hst:port";
    public final static String VIRTUALHOST_PROPERTY_PROTOCOL = "hst:protocol";
    public final static String VIRTUALHOST_PROPERTY_SITENAME = "hst:sitename";
    public final static String VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    
    public final static String SITEMOUNT_HST_ROOTNAME = "hst:root";
    public final static String SITEMOUNT_PROPERTY_SHOWCONTEXTPATH = "hst:showcontextpath";
    public final static String SITEMOUNT_PROPERTY_NAMEDPIPELINE = "hst:namedpipeline";
    public final static String SITEMOUNT_PROPERTY_PORT = "hst:port";
    public final static String SITEMOUNT_PROPERTY_SHOWPORT = "hst:showport";
    public final static String SITEMOUNT_PROPERTY_PROTOCOL = "hst:protocol";
    public final static String SITEMOUNT_PROPERTY_MOUNTPATH = "hst:mountpath";
    public final static String SITEMOUNT_PROPERTY_ISPREVIEW = "hst:ispreview";
     
    public final static String NODEPATH_HST_CONFIGURATION = "hst:configuration/hst:configuration";
    public final static String NODENAME_HST_CONTENTNODE = "hst:content";
    public final static String NODENAME_HST_SITEMAP = "hst:sitemap";
    public final static String NODENAME_HST_SITEMENUS = "hst:sitemenus";
    public final static String NODENAME_HST_COMPONENTS = "hst:components";
    public final static String NODENAME_HST_PAGES = "hst:pages";
    public final static String NODENAME_HST_TEMPLATES = "hst:templates";
    
    
}
