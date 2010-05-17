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
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMount;

/**
 * <p>A SiteMount object is the mount from a prefix to some (sub)site *or* content location: when the <code>issitemount</code> property is <code>true</code> or missing,
 * the SiteMount is linked to a {@link HstSite}. When <code>issitemount</code> property is <code>false</code>, the SiteMount should have it's own namedPipeline and the
 * <code>hst:mountpoint</code> property is used a content path: for example a simple jcr browser unaware of HstSite at all could be easily built with this. 
 * </p>
 * <p>
 * SiteMount is a Composite pattern: Each SiteMount can contain any descendant
 * child SiteMount tree. A SiteMount 'lives' below a {@link VirtualHost}. The SiteMount directly below a {@link VirtualHost} <b>must</b> be called <b>hst:root</b> by definition. 
 * The <code>hst:root</code> SiteMount is where the SiteMount matching starts. Once a virtual host is matched for the incoming {@link HttpServletRequest},
 * we inspect the request path (the path after the context path and before the query string) and try to match the request path as deep as possible to the SiteMount tree: note that 
 * we try to map to the best matching SiteMount: This means, that 'exact' matching names have precedence over wildcard names
 * </p>
 * 
 * Thus, suppose we have the following SiteMount tree configuration:
 * 
 * <pre>
 *    127.0.0.1
 *      `- hst:root  (hst:mountpoint = /live/myproject, hst:issitemount = true)
 *            `- preview (hst:mountpoint = /preview/myproject, hst:issitemount = true)
 * </pre>
 * <p>
 * The configuration above means, that below the host 127.0.0.1 we have of course the mandatory SiteMount <code>hst:root</code>, and below it, we have 
 * a SiteMount <code>preview</code>. Every request path that starts with <code>/preview</code> will be mapped to 'preview' SiteMount, all other request path's that do not start with '/preview' 
 * resolve to the <code>hst:root<code> item. While the request path does match the next SiteMount descendant in the tree, the matching is continued to descendant SiteMount items. Thus, in the current example,
 * the request path <code>/preview/home</code> will return the SiteMount with name 'preview'. 
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
 *        ` hst:root  (hst:mountpoint = /live/myproject)
 *    preview.mycompany.com
 *        ` hst:root  (hst:mountpoint = /preview/myproject)
 * </pre>
 * 
 * <p>As can be seen, instead of using the request path prefix to distuinguish between live and preview, we now do so by hostname.</p>
 * 
 * An example with more SiteMount items is for example:
 * 
 * <pre>
 *    127.0.0.1
 *      `- hst:root  (hst:mountpoint = /live/myproject)
 *            |-myrestservice (hst:mountpoint = /live/myproject, hst:namedpipeline=JaxrsPipeline)
 *            `- preview (hst:mountpoint = /preview/myproject)
 *                  `-myrestservice (hst:mountpoint = /preview/myproject, hst:namedpipeline=JaxrsPipeline)
 * </pre>
 * 
 * <p>Now, all request path's that start with <code>/myrestservice</code> or  <code>/preview/myrestservice</code> resolve to the <code>myrestservice</code>
 * SiteMount item. This one has a custom <code>hst:namedpipeline</code>, where you can configure a complete custom hst request processing, in this example
 * some pipeline exposing some rest interface.</p>
 * 
 * <p>
 * Optionally, wildcard matching support can be implemented, where even the wilcards can be used within the property values. For example:
 * <pre>
 *    127.0.0.1
 *      `- hst:root  (hst:mountpoint = /live/myproject)
 *            |- * (hst:mountpoint = /live/${1})
 *            `- preview (hst:mountpoint = /preview/myproject)
 *                  ` - * (hst:mountpoint = /preview/${1})
 * </pre>
 * 
 * The above means, that when there is a (sub)site complying to the wildcard, this one is used, and otherwise, the default one pointing
 * to <code>myproject</code> is used. Thus, a request path like <code>/preview/mysubsite/home</code> will map to the SiteMount <code>/preview/*</code>
 * if there is a (sub)site at the mountpoint <code>/preview/${1}</code>, where obviously ${1} is substituted by 'mysubsite'. Assuming that there
 * is no subsite called 'news', a request path like /preview/news/2010/05/my-news-item will thus be handled by the site 'myproject'
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
     * When this returns <code>true</code>, this SiteMount is referring to a {@link HstSite}. When it is <code>false</code>, it is referring
     * to a content location when {@link #getMountPoint()} is not <code>null</code> or empty. When having the value <code>false</code>, you most
     * like invoke some specific pipeline through {@link #getNamedPipeline()}
     * @return <code>true</code> when this SiteMount is referring to a {@link HstSite}
     */
    boolean isSiteMount();
    
    /**
     * @return the parent SiteMount of this SiteMount and <code>null</code> if we are at the root SiteMount
     */
    SiteMount getParent();
    
    /**
     * <p>
     * Returns the mount point for this SiteMount object. The root SiteMount has an empty mount point. A mountPoint for a 
     * SiteMount is its own {@link #getName()} plus all ancestors up to the root and always starts with a "/" (unless for the root, this one is empty).
     * It can contain wildcards, for example /preview/*. Typically, these wildcards are replaced by their request specific values in the {@link ResolvedSiteMount}.
     * </p>
     * 
     * @see ResolvedSiteMount#getResolvedMountPath()
     * @return the mountPoint for this siteMount
     */
    String getMountPoint();
    
    /**
     * <p>
     * Returns the mount path for this SiteMount object. The root SiteMount has an empty mount path. A mountPath for a 
     * SiteMount is its own {@link #getName()} plus all ancestors up to the root and always starts with a "/" (unless for the root, this one is empty).
     * It can contain wildcards, for example /preview/*. Typically, these wildcards are replaced by their request specific values in the {@link ResolvedSiteMount}.
     * </p>
     * 
     * <p>
     * Note the difference with {@link #getMountPoint()}: this returns the jcr location of the (sub)site or of the content
     * </p>
     * 
     * @see ResolvedSiteMount#getResolvedMountPath()
     * @return the mountPath for this siteMount
     */
    String getMountPath();
    
    /**
     * 
     * @param name of the child SiteMount
     * @return a SiteMount with {@link #getName()} equal to <code>name</code> or <code>null</code> when there is no such item
     */
    SiteMount getChildMount(String name);
    
    /**
     * @return the virtualHost where this SiteMount belongs to
     */
    VirtualHost getVirtualHost();
    
    /**
     * @return the {@link HstSite} this <code>SiteMount</code> is pointing to or <code>null</code> when none found
     */
    HstSite getHstSite();
  
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
     * @return the homepage for this SiteMount or <code>null</code> when not present
     */
    String getHomePage();
    
    /**
     * 
     * @return the pagenotfound for this SiteMount or <code>null</code> when not present
     */
    String getPageNotFound();
    
    /**
     * @return the scheme to use for creating external urls, for example http / https
     */
    String getScheme();
    
    /**
     * @return <code>true</code> when this SiteMount is configured to be a preview site mount
     */
    boolean isPreview();
    
    /**
     * When this SiteMount has {@link #isPreview()} return <code>false</code>, this method always returns false. When the SiteMount is preview,
     * and the SiteMount is configured to have the hst version number in preview, then this method returns <code>true</code> 
     * @return <code>true</code> when for this SiteMount the current hst version should be added as a response header
     */
    boolean isVersionInPreviewHeader();
    
    /**
     * Note that if an ancestor siteMount contains a namedPipeline, this value is inherited unless this siteMount explicitly defines its own
     * @return the named pipeline to be used for this SiteMount or <code>null</code> when the default pipeline is to be used
     */
    String getNamedPipeline();

    /**
     * This is a shortcut method fetching the HstSiteMapMatcher from the backing {@link VirtualHostsManager}
     * @return the HstSiteMapMatcher implementation
     */
    HstSiteMapMatcher getHstSiteMapMatcher();
    
}
