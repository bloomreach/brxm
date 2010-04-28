/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.configuration.hosting;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * <p>A SiteMount object is the mount from a prefix to some (sub)site. SiteMount is a Composite pattern: Each SiteMount can contain any descendant
 * child SiteMount tree. A SiteMount 'lives' below a {@link VirtualHost}. The SiteMount directly below a {@link VirtualHost} <b>must</b> be called <b>root</b> by definition and
 * must exist. The <code>root</code> SiteMount is where the SiteMount matching starts. Once a virtual host is matched for the incoming {@link HttpServletRequest},
 * we inspect the {@link HttpServletRequest#getPathInfo()} and try to match the path info as deep as possible to the SiteMount tree: note that 
 * we try to map to the best matching SiteMount: This means, that 'exact' matching names have precedence over wildcard names
 * </p>
 * 
 * Thus, suppose we have the following SiteMount tree configuration:
 * 
 * <pre>
 *    127.0.0.1
 *      `- root  (hst:mountpath = /live/myproject)
 *            `- preview (hst:mountpath = /preview/myproject)
 * </pre>
 * <p>
 * The configuration above means, that below the host 127.0.0.1 we have of course the mandatory SiteMount <code>root</code>, and below it, we have 
 * a SiteMount <code>preview</code>. Every pathInfo that starts with <code>/preview</code> will be mapped to 'preview' SiteMount, all other pathInfo's that do not start with '/preview' 
 * resolve to the <code>root<code> item. While the pathInfo does match the next SiteMount descendant in the tree, the matching is continued to descendant SiteMount items, also see {@link #match(HttpServletRequest)}. Thus, in the current example,
 * the pathInfo <code>/preview/home</code> will return the SiteMount with name 'preview'. 
 * </p>
 * 
 * Also, you can configure some of the same properties the {@link VirtualHost} also has:
 *  <ul>
 *  <li>hst:port (long)</li>
 *  <li>hst:showcontextpath (boolean)</li>
 *  <li>hst:showport(boolean)</li>
 *  <li>hst:scheme (string)</li>
 *  </ul>
 * 
 * <p>One extra is possible, <code>hst:namedpipeline</code>, see below for an example.</p> 
 * 
 * <p>Just as with the virtual hosts, properties are <b>inherited</b> by child SiteMount items as long as they are not defined by themselves
 * </p>
 * 
 * Obviously, the above configuration might not be desirable in some circumstances, for example, when on a production host, you do not want 
 * a preview available at all. Configuration then for example could be:
 * 
 * <pre>
 *    www.mycompany.com
 *        ` root  (hst:mountpath = /live/myproject)
 *    preview.mycompany.com
 *        ` root  (hst:mountpath = /preview/myproject)
 * </pre>
 * 
 * <p>As can be seen, instead of using the pathInfo prefix to distuinguish between live and preview, we now do so by hostname.</p>
 * 
 * An example with more SiteMount items is for example:
 * 
 * <pre>
 *    127.0.0.1
 *      `- root  (hst:mountpath = /live/myproject)
 *            |-myrestservice (hst:mountpath = /live/myproject, hst:namedpipeline=JaxrsPipeline)
 *            `- preview (hst:mountpath = /preview/myproject)
 *                  `-myrestservice (hst:mountpath = /preview/myproject, hst:namedpipeline=JaxrsPipeline)
 * </pre>
 * 
 * <p>Now, all pathInfo's that start with <code>/myrestservice</code> or  <code>/preview/myrestservice</code> resolve to the <code>myrestservice</code>
 * SiteMount item. This one has a custom <code>hst:namedpipeline</code>, where you can configure a complete custom hst request processing, in this example
 * some pipeline exposing some rest interface.</p>
 * 
 * <p>
 * Optionally, wildcard matching support can be implemented, where even the wilcards can be used within the property values. For example:
 * <pre>
 *    127.0.0.1
 *      `- root  (hst:mountpath = /live/myproject)
 *            |- * (hst:mountpath = /live/${1})
 *            `- preview (hst:mountpath = /preview/myproject)
 *                  ` - * (hst:mountpath = /preview/${1})
 * </pre>
 * 
 * The above means, that when there is a (sub)site complying to the wildcard, this one is used, and otherwise, the the default one pointing
 * to <code>myproject</code> is used. Thus, a pathInfo like <code>/preview/mysubsite/home</code> will map to the SiteMount <code>/preview/*</code>
 * if there is a (sub)site at the mountpath <code>/preview/${1}</code>, where obviously ${1} is substituted by 'mysubsite'. Assuming that there
 * is no subsite called 'news', a pathInfo like /preview/news/2010/05/my-news-item will thus be handled by the site 'myproject'
 * 
 * </p>
 * 
 */
public interface SiteMount {

    
    /**
     * @return The name of this SiteMount item
     */
    String getName();
    
    /**
     * @return the parent SiteMount of this SiteMount and <code>null</code> if we are at the root SiteMount
     */
    SiteMount getParent();
    
    /**
     * 
     * @param name of the child SiteMount
     * @return a SiteMount with {@link #getName()} equal to <code>name</code> or <code>null</code> when there is no such item
     */
    SiteMount getChildMount(String name);
    
    /**
     * @return the List of child SiteMount's. If there are no child SiteMount's, and empty List must be returned
     */
    // TODO Do we need this one?
    //List<SiteMount> getSiteMountChilds();
    
    /**
     * @return the virtualHost where this SiteMount belongs to
     */
    VirtualHost getVirtualHost();
    
    /**
     * @return the <code>HstSite</code> this <code>SiteMount</code> is pointing to or <code>null</code> when none found
     */
    HstSite getHstSite();
    
    /**
     * This method handles multiple matching's: First, the pathInfo is used, to match the best deepest <code>SiteMount</code>. Once the best deepest 
     * <code>SiteMount</code> is found, the remainder of the pathInfo will be delegated to the {@link HstSiteMapMatcher}, which returns a flyweight 'request' time 
     * instance of a {@link HstSiteMapItem}  
     * @param request
     * @return the resolvedSiteMap item for the current request pathInfo or <code>null</code> when the request can not be matched.
     * @throws MatchException when the matching cannot be done, for example because no valid virtual hosts are configured
     */
    ResolvedSiteMapItem match(HttpServletRequest request) throws MatchException;
    
    /**
     * @return <code>true</code> when the created url should have the contextpath in it
     */
    boolean isContextPathInUrl();
    
    /**
     * 
     * @return <code>true</code> when an externalized (starting with http/https) should contain a port number
     */
    boolean isPortVisible();

    /**
     * 
     * @return when {@link #isPortVisible()} returns <code>true</code> this method returns the port number of the externalized url
     */
    int getPortNumber();
    
    /**
     * @return the scheme to use for creating external urls, for example http / https
     */
    String getScheme();
    
    /**
     * 
     * @return <code>true</code> when this SiteMount is configured to be a preview site mount
     */
    boolean isPreview();
    
    /**
     * @return the named pipeline to be used for this SiteMount or <code>null</code> when the default pipeline is to be used
     */
    String getNamedPipeline();
    
}
