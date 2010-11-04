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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
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
     * the predefined property name prefix to indicates mount aliases
     */
    static final String PROPERTY_NAME_MOUNT_PREFIX = "hst:mount";
    
    /**
     * @return The name of this SiteMount item
     */
    String getName();
    
    /**
     * Returns the alias of this SiteMount item. If there is no specific alias defined, then the {@link #getName()} is used. However,
     * the alias of a SiteMount <b>must</b> be unique in combination with every type {@link #getTypes()} within a single host group, also 
     * see {@link VirtualHosts#get}
     * @return The alias of this SiteMount item. 
     */
    String getAlias();
    
    /**
     * When this SiteMount is not using a {@link HstSite} for the request processing, this method returns <code>true</code>. When it returns <code>true</code>, then 
     * {@link #getNamedPipeline()} should also be configured, and a pipeline should be invoked that is independent of the {@link ResolvedSiteMapItem} as their won't be one.
     */
    boolean isSiteMount();
    
    /**
     * @return the parent SiteMount of this SiteMount and <code>null</code> if we are at the root SiteMount
     */
    SiteMount getParent();
    
    /**
     * <p>
     * Returns the mount point for this SiteMount object. The mount point can be the absolute jcr path to the root site node, for example 
     * something like '/hst:hst/hst:sites/mysite-live', but it can also be some jcr path to some virtual or canonical node in the repository. For example
     * it can be '/content/gallery' , which might be the case for a Mount suited for REST gallery calls. 
     * </p>
     * 
     * @see ResolvedSiteMount#getResolvedMountPath()
     * @return the mountPoint for this siteMount
     */
    String getMountPoint();
    
    /**
     * <p>
     * Returns the content path for this SiteMount object. The content path is the absolute jcr path to the root site node content, for example 
     * something like '/hst:hst/hst:sites/mysite-live/hst:content'. The {@link #getContentPath()} can be the same as {@link #getMountPoint()}, but
     * this is in general only for {@link SiteMount}'s that have {@link #isSiteMount()} returning false. When the {@link SiteMount} does have
     * {@link #isSiteMount()} equal to true, the {@link #getContentPath()} can return a different path than {@link #getMountPoint()}. In general, it will be
     * then {@link #getMountPoint()} + "/hst:content". 
     * </p>
     * 
     * @return the content path for this <code>SiteMount</code>. It cannot be <code>null</code>
     */
    String getContentPath();
    
    /**
     * Returns the absolute canonical content path for the content of this {@link SiteMount}. Note that it can return the same
     * value as {@link #getContentPath()}, but this is in general not the case: When the {@link #getContentPath()} points
     * to a virtual node, this method returns the location of the canonical version. When the {@link #getContentPath()} points to 
     * a node which behaves like a <b>mirror</b>, then this method returns the location where the mirror points to. If 
     * {@link #getContentPath()} does not point to a virtual node, nor to a mirror, this method returns the same value. 
     * 
     * @return The absolute absolute content path for this <code>SiteMount</code>. It can be <code>null</code> in case {@link #getContentPath()} points to a virtual node
     * that does not have a canonical version.
     */
    String getCanonicalContentPath();
    

    /**
     * <p>
     * Returns the mount path for this SiteMount object. The root SiteMount has an empty mount path. A mountPath for a 
     * SiteMount is its own {@link #getName()} plus all ancestors up to the root and always starts with a "/" (unless for the root, this one is empty).
     * It can contain wildcards, for example /preview/*. Typically, these wildcards are replaced by their request specific values in the {@link ResolvedSiteMount}.
     * </p>
     * 
     * <p>
     * Note the difference with {@link #getMountPoint()}: the {@link #getMountPoint()} returns the jcr location of the (sub)site or of the content
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
     * @return <code>true</code> when the created url should have contain the port number
     */
    boolean isPortInUrl();
    
    /**
     * When this method returns <code>true</code>, then {@link HstLink} will always have the {@link HstLink#PATH_SUBPATH_DELIMITER} included, even if the {@link HstLink}
     * does have an empty or <code>null</code> {@link HstLink#getSubPath()}
     * @return true when subPath is supported
     */
    boolean supportsSubPath();
    

    /**
     * @return the portnumber for this siteMount
     */
    int getPort();
    
    /**
     * In case the {@link HttpServletRequest#getContextPath()} does not matter, this method must return <code>null</code> or empty. <b>If</b> only this SiteMount 
     * can be used for a certain contextPath, this method should return that contextPath. A contextPath has to start with a "/" and is not allowed to have any other "/". 
     * 
     * @return <code>null</code> or empty if the contextPath does not matter, otherwise it returns the value the contextPath must have a possible to match to this SiteMount
     */
    String onlyForContextPath();
    
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
     * This method returns the same as {@link SiteMount#isOfType(String type)} with <code>type="preview"</code>
     * 
     * @return <code>true</code> when this SiteMount is configured to be a preview site mount. 
     */
    boolean isPreview();
    
    
    /**
     * When a this SiteMount is of type <code>type</code> this returns <code>true</code>. A SiteMount can be of multiple types at once.
     * @param type the type to test
     * @return <code>true</code> when this SiteMount is of type <code>type</code>
     */
    boolean isOfType(String type);
    
    /**
     * @return the primary type of this SiteMount
     */
    String getType();
    
    /**
     * @return the list of all types this SiteMount belongs to, including the primary type {@link #getType()}. The primary type is the first item in the List
     */
    List<String> getTypes();
    
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
     * the locale for this siteMount or <code>null</code> when it does not contain one. Note that if an ancestor siteMount contains a 
     * locale, this value is inherited unless this siteMount explicitly defines its own. The root siteMount inherits the value from 
     * the {@link VirtualHost} if the virtual host contains a locale
     * @return the locale for this siteMount or <code>null</code> when it does not contain one. 
     */
    String getLocale();

    /**
     * This is a shortcut method fetching the HstSiteMapMatcher from the backing {@link HstManager}
     * @return the HstSiteMapMatcher implementation
     */
    HstSiteMapMatcher getHstSiteMapMatcher();
    
    /**
     * for embedded delegation of sites a sitemountpath needs to point to the delegated sitemount. This is only relevant for portal environment
     * @return the embedded sitemount path and <code>null</code> if not present
     */
    String getEmbeddedSiteMountPath();
    
    /**
     * If this method returns true, then only if the user is explicitly allowed or <code>servletRequest.isUserInRole(role)</code> returns <code>true</code> this
     * site mount is accessible for the request. 
     * 
     * If a site mount does not have a configuration for isSecure, the value from the parent item is taken.
     * 
     * @return <code>true</code> if the site mount is secured. 
     */
    boolean isSecured();
    
    /**
     * Returns the roles that are allowed to access this site mount when {@link isSecure()} is true. If the site mount does not have any roles defined by itself, it
     * inherits them from the parent. If it defines roles, the roles from any ancestor are ignored. An empty set of roles
     * in combination with {@link #isSecured()} return <code>true</code> means nobody has access to the item
     * 
     * @return The set of roles that are allowed to access this site mount. When no roles defined, the roles from the parent item are inherited. If none of the 
     * parent items have a role defined, an empty set is returned
     */
    Set<String> getRoles();  
    
    /**
     * Returns the users that are allowed to access this site mount when {@link isSecure()} is true. If the site mount does not have any users defined by itself, it
     * inherits them from the parent. If it defines users, the users from any ancestor are ignored. An empty set of users
     * in combination with {@link #isSecured()} return <code>true</code> means nobody has access to the item
     * 
     * @return The set of users that are allowed to access this site mount. When no users defined, the users from the parent item are inherited. If none of the 
     * parent items have a user defined, an empty set is returned
     */
    Set<String> getUsers();  
    
    /**
     * Returns true if subject based jcr session should be used for this site mount 
     * @return
     */
    boolean isSubjectBasedSession();
    
    /**
     * Returns true if subject based jcr session should be statefully managed. 
     * @return
     */
    boolean isSessionStateful();
    
    /**
     * the string value of the property or <code>null</code> when the property is not present. When the property value is not of
     * type {@link String}, we'll return the {@link Object#toString()} value
     * @param name the name of the property
     * @return the value of the property or <code>null</code> when the property is not present
     */
    String getProperty(String name);
    
    /**
     * Returns all the properties that start with {@value #PROPERTY_NAME_MOUNT_PREFIX} and have value of type String. This map has as key the 
     * propertyname after {@value #PROPERTY_NAME_MOUNT_PREFIX}
     * @return all the mount properties
     */
    Map<String, String> getMountProperties();
}
