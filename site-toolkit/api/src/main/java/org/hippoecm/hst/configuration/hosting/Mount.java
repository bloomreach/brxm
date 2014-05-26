/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * <p>A {@link Mount} object is the mount from a prefix to some (sub)site *or* content location: when the {@link Mount#isMapped()} property returns <code>true</code> or missing,
 * the {@link Mount} is linked to a {@link HstSite} that uses a {@link HstSiteMap}. When {@link Mount#isMapped()} property returns <code>false</code>, the {@link Mount} won't use
 * a URL mapping through the {@link HstSiteMap}
 * </p>
 * <p>
 * {@link Mount} is a Composite pattern: Each {@link Mount} can contain any descendant
 * child {@link Mount} tree. A {@link Mount} 'lives' below a {@link VirtualHost}. The {@link Mount} directly below a {@link VirtualHost} <b>must</b> be called <b>hst:root</b> by definition.
 * The <code>hst:root</code> {@link Mount} is where the {@link Mount} matching starts. Once a virtual host is matched for the incoming {@link HttpServletRequest},
 * we inspect the request path (the path after the context path and before the query string) and try to match the request path as deep as possible to the {@link Mount} tree: note that
 * we try to map to the best matching {@link Mount}: This means, that 'exact' matching names have precedence over wildcard names
 * </p>
 *
 * Thus, suppose we have the following {@link Mount} tree configuration:
 *
 * <pre>
 *    +127.0.0.1
 *       +hst:root
 *           - hst:mountpoint = /content/documents/myproject
 *           - hst:ismapped = true
 *           + subsite
 *              - hst:mountpoint = /content/documents/subsite
 *              - hst:ismapped = true
 * </pre>
 * <p>
 * The configuration above means, that below the host 127.0.0.1 we have of course the mandatory {@link Mount} <code>hst:root</code>, and below it, we have
 * a {@link Mount} <code>subsite</code>. Every request path that starts with <code>/subsite</code> will be mapped to 'subsite' {@link Mount}, all other request path's that do not start with '/subsite'
 * resolve to the <code>hst:root<code> item. While the request path does match the next {@link Mount} descendant in the tree, the matching is continued to descendant {@link Mount} items. Thus, in the current example,
 * the request path <code>/subsite/home</code> will return the {@link Mount} with name 'subsite'.
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
 * <p>Just as with the virtual hosts, properties are <b>inherited</b> by child {@link Mount} items as long as they are not defined by themselves
 * </p>
 *
 * Obviously, the above configuration might not be desirable in some circumstances, for example, when on a production host, you do not want
 * a preview available at all. Configuration then for example could be:
 *
 * <pre>
 *    +www.mycompany.com
 *        +hst:root
 *           - hst:mountpoint = /content/documents/myproject
 *    +sub.mycompany.com
 *        +hst:root
 *           - hst:mountpoint = /content/documents/subsite
 * </pre>
 *
 * Optionally, wildcard matching support can be implemented, where the wilcards can be used within the property values. For example:
 * </p>
 *
 */
public interface Mount {

    /**
     * the predefined property name prefix to indicates mount aliases
     */
    static final String PROPERTY_NAME_MOUNT_PREFIX = "hst:mount";

    /**
     * the string value that indicates 'live'
     */
    static final String LIVE_NAME = "live";

    /**
     * the string value that indicates 'preview'
     */
    static final String PREVIEW_NAME = "preview";

    /**
     * @return The name of this {@link Mount} item
     */
    String getName();

    /**
     * Returns the alias of this {@link Mount} item. The alias of a {@link Mount} <b>must</b> be unique in combination with every type {@link #getTypes()} within a single host group, also
     * see ({@link VirtualHost#getHostGroupName()}). When there is no alias defined on the {@link Mount}, <code>null</code> is returned. The {@link Mount} can then not be used
     * to lookup by alias
     * @return The alias of this {@link Mount} item or <code>null</code> when it does not have one
     */
    String getAlias();

    /**
     * When this {@link Mount} is not using a {@link HstSiteMap} for the request processing, this method returns <code>false</code>. When it returns <code>false</code>, then
     * {@link #getNamedPipeline()} should also be configured, and a pipeline should be invoked that is independent of the {@link ResolvedSiteMapItem} as their won't be one.
     */
    boolean isMapped();

    /**
     * @return the parent {@link Mount} of this {@link Mount} and <code>null</code> if we are at the root {@link Mount}
     */
    Mount getParent();

    /**
     * <p>
     * Returns the mount point for this {@link Mount} object. The mount point can be the absolute jcr path to the root site node, for example
     * something like '/hst:hst/hst:sites/mysite', but it can also be some jcr path to some virtual or canonical node in the repository. For example
     * it can be '/content/gallery' , which might be the case for a Mount suited for REST gallery calls.
     * </p>
     *
     * @see ResolvedMount#getResolvedMountPath()
     * @return the mountPoint for this {@link Mount} and <code>null</code> if there is no mountPoint configured (nor inherited)
     */
    String getMountPoint();

    /**
     * <p>
     * Returns the content path for this {@link Mount} object. The content path is the absolute jcr path to the root site node content, for example
     * something like '/content/documents/myproject'. The {@link #getContentPath()} can be the same as {@link #getMountPoint()}, but
     * this is in general only for {@link Mount}'s that have {@link #isMapped()} returning false. When the {@link Mount} does have
     * {@link #isMapped()} equal to true, the {@link #getContentPath()} can return a different path than {@link #getMountPoint()}.
     * </p>
     *
     * @return the content path for this {@link Mount}. It cannot be <code>null</code>
     */
    String getContentPath();

    /**
     * Returns the absolute canonical content path for the content of this {@link Mount}. Note that it returns in general the same
     * value as {@link #getContentPath()}
     *
     * @return The absolute absolute content path for this {@link Mount}.
     * @deprecated since 7.9.0 : returns same as {@link #getContentPath()} : Use {@link #getContentPath()} instead
     */
    @Deprecated
    String getCanonicalContentPath();


    /**
     * <p>
     * Returns the mount path for this {@link Mount} object. The root {@link Mount} (mount with no parent mount) has an empty {@link String} ("") as mount path. A mountPath for a
     * {@link Mount} that is not a root {@link Mount} is its own {@link #getName()} plus all ancestors until the root and always starts with a "/" (except for the root, this one is empty).
     * It can contain wildcards, for example /preview/*. Typically, these wildcards are replaced by their request specific values in the {@link ResolvedMount}.
     * </p>
     *
     * <p>
     * Note the difference with {@link #getMountPoint()}: the {@link #getMountPoint()} returns the jcr location of the (sub)site or of the content
     * </p>
     *
     * @see ResolvedMount#getResolvedMountPath()
     * @return the mountPath for this {@link Mount}
     */
    String getMountPath();

    /**
     * @return the unmodifiable {@link List} of all child {@link Mount}s and an empty {@link List} if there are no child {@link Mount}s
     */
    List<Mount> getChildMounts();

    /**
     *
     * @param name of the child {@link Mount}
     * @return a {@link Mount} with {@link #getName()} equal to <code>name</code> or <code>null</code> when there is no such item
     */
    Mount getChildMount(String name);

    /**
     * @return the virtualHost where this {@link Mount} belongs to
     */
    VirtualHost getVirtualHost();

    /**
     * @return the {@link HstSite} this <code>{@link Mount}</code> is pointing to or <code>null</code> when none found
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
     * When this method returns <code>false</code>, then {@link HstLink} will have the {@link HstManager#getPathSuffixDelimiter()}  included even for empty or <code>null</code> {@link HstLink#getSubPath()}, if and only if
     * the {@link VirtualHosts#isExcluded(String)} for the path to create a link for returns <code>true</code>. This is to avoid that for example a path that ends with .pdf will be skipped by the
     * {@link HstRequestProcessor} due to {@link VirtualHosts#isExcluded(String)} : This is undesirable in case of a REST link for a binary for example
     * @return true when the {@link Mount} is meant to be a site (false in case of for example being used for REST calls)
     */
    boolean isSite();


    /**
     * @return the portnumber for this {@link Mount}
     */
    int getPort();

    /**
     * @deprecated since CMS 7.9.1 : Use {@link #getContextPath()} instead
     */
    @Deprecated
    String onlyForContextPath();

    /**
     * <p>
     *    Returns the contextpath (webapp) for this {@link Mount}. A request can only be matched to this
     *    {@link Mount} if the request its {@link javax.servlet.http.HttpServletRequest#getContextPath()} is equal
     *    to {@link #getContextPath()}, or when the {@link Mount} returns <code>null</code> for {@link #getContextPath()} : Namely
     *    <code>null</code> means the {@link Mount} is contextpath (webapp) agnostic and can be matched regardless the
     *   {@link javax.servlet.http.HttpServletRequest#getContextPath()}
     * </p>
     * @return the contextpath (webapp) for this {@link Mount}. The contextpath for the ROOT application must be an empty String.
     * If non-empty, a path starting with a "/" character but that does not end with a "/" character must be returned. If
     * <code>null</code> is returned, the {@link Mount} is contextpath (webapp) agnostic and can be matched regardless the
     * {@link javax.servlet.http.HttpServletRequest#getContextPath()}
     */
    String getContextPath();

    /**
     * @return the homepage for this {@link Mount} or <code>null</code> when not present
     */
    String getHomePage();

    /**
     *
     * @return the pagenotfound for this {@link Mount} or <code>null</code> when not present
     */
    String getPageNotFound();

    /**
     * @return the scheme to use for creating external urls, for example http / https
     */
    String getScheme();

    /**
     * If a {@link Mount} is scheme agnostic, the request gets served regardless whether it is <code>http</code> or
     * <code>https</code> (assuming {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} does not override the value)
     * @return <code>true</code> when this {@link Mount} is scheme agnostic
     */
    boolean isSchemeAgnostic();

    /**
     * When this {@link Mount} has a {@link HstSite} attached to it that contains a {@link HstSiteMap} with {@link HstSiteMapItem}'s that
     * have multiple schemes (http/https) or that have a different scheme than the {@link Mount#getScheme()}.
     * @return <code>true</code> when this {@link Mount} can contain links with different schemes
     */
    boolean containsMultipleSchemes();

    /**
     * <p>
     * the response code the HST sets when {@link javax.servlet.http.HttpServletRequest} <code>scheme</code> does not match {@link #getScheme()}.
     * Default response code is {@link javax.servlet.http.HttpServletResponse#SC_MOVED_PERMANENTLY}. The following response
     * codes are supported and result in:
     * </p>
     * <p>
     *     <ol>
     *         <li>200 : no behavior, ignored</li>
     *         <li>301 : when request has different scheme than {@link #getScheme()}, permanent redirect to the correct scheme is done</li>
     *         <li>302 | 303 | 307 : when request has different scheme than {@link #getScheme()}, temporal redirect to the correct scheme is done</li>
     *         <li>403 : when request has different scheme than {@link #getScheme()}, a page forbidden is returned</li>
     *         <li>404 : when request has different scheme than {@link #getScheme()}, a page not found is returned</li>
     *     </ol>
     * </p>
     * <p>
     *     Any other response code than above will result in inheriting the response code from parent {@link Mount} or {@link VirtualHost}
     * </p>
     */
    int getSchemeNotMatchingResponseCode();

    /**
     * This method returns the same as {@link Mount#isOfType(String type)} with <code>type="preview"</code>
     *
     * @return <code>true</code> when this {@link Mount} is configured to be a preview Mount.
     */
    boolean isPreview();


    /**
     * When a this {@link Mount} is of type <code>type</code> this returns <code>true</code>. A {@link Mount} can be of multiple types at once.
     * @param type the type to test
     * @return <code>true</code> when this {@link Mount} is of type <code>type</code>
     */
    boolean isOfType(String type);

    /**
     * @return the primary type of this {@link Mount}
     */
    String getType();

    /**
     * @return the list of all types this {@link Mount} belongs to, including the primary type {@link #getType()}. The primary type is the first item in the List
     */
    List<String> getTypes();

    /**
     * When this {@link Mount} has {@link #isPreview()} return <code>false</code>, this method always returns false. When the {@link Mount} is preview,
     * and the {@link Mount} is configured to have the hst version number in preview, then this method returns <code>true</code>
     * @return <code>true</code> when for this {@link Mount} the current hst version should be added as a response header
     */
    boolean isVersionInPreviewHeader();

    /**
     * Note that if an ancestor {@link Mount} contains a namedPipeline, this value is inherited unless this {@link Mount} explicitly defines its own
     * @return the named pipeline to be used for this {@link Mount} or <code>null</code> when the default pipeline is to be used
     */
    String getNamedPipeline();

    /**
     * the locale for this {@link Mount} or <code>null</code> when it does not contain one. Note that if an ancestor {@link Mount} contains a
     * locale, this value is inherited unless this {@link Mount} explicitly defines its own. The root {@link Mount} inherits the value from
     * the {@link VirtualHost} if the virtual host contains a locale
     * @return the locale for this {@link Mount} or <code>null</code> when it does not contain one.
     */
    String getLocale();

    /**
     * This is a shortcut method fetching the HstSiteMapMatcher from the backing {@link HstManager}
     * @return the HstSiteMapMatcher implementation
     */
    HstSiteMapMatcher getHstSiteMapMatcher();

    /**
     * If this method returns true, then only if the user is explicitly allowed or <code>servletRequest.isUserInRole(role)</code> returns <code>true</code> this
     * Mount is accessible for the request.
     *
     * If a Mount does not have a configuration for authenticated, the value from the parent item is taken.
     *
     * @return <code>true</code> if the Mount is authenticated.
     */
    boolean isAuthenticated();

    /**
     * Returns the roles that are allowed to access this Mount when {@link #isAuthenticated()} is true. If the Mount does not have any roles defined by itself, it
     * inherits them from the parent. If it defines roles, the roles from any ancestor are ignored. An empty set of roles
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     *
     * @return The set of roles that are allowed to access this Mount. When no roles defined, the roles from the parent item are inherited. If none of the
     * parent items have a role defined, an empty set is returned
     */
    Set<String> getRoles();

    /**
     * Returns the users that are allowed to access this Mount when {@link #isAuthenticated()} is true. If the Mount does not have any users defined by itself, it
     * inherits them from the parent. If it defines users, the users from any ancestor are ignored. An empty set of users
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     *
     * @return The set of users that are allowed to access this Mount. When no users defined, the users from the parent item are inherited. If none of the
     * parent items have a user defined, an empty set is returned
     */
    Set<String> getUsers();

    /**
     * Returns true if subject based jcr session should be used for this Mount
     * @return
     */
    boolean isSubjectBasedSession();

    /**
     * Returns true if subject based jcr session should be statefully managed.
     * @return
     */
    boolean isSessionStateful();

    /**
     * Returns FORM Login Page
     * @return the FORM Login Page and <code>null</code> if not configured
     */
    String getFormLoginPage();

    /**
     * the string value of the property or <code>null</code> when the property is not present. When the property value is not of
     * type {@link String}, we'll return the {@link Object#toString()} value
     * @param name the name of the property
     * @return the value of the property or <code>null</code> when the property is not present
     */
    String getProperty(String name);

    /**
     * <p>
     * Returns all the properties that start with {@value #PROPERTY_NAME_MOUNT_PREFIX} and have value of type {@link String}. This map has as key the
     * propertyname after {@value #PROPERTY_NAME_MOUNT_PREFIX}.
     * </p>
     * <p>
     * <b>Note</b> The property called <code>hst:mountpoint</code> is excluded from this map, as it has a complete different purpose
     * </p>
     * @return all the mount properties and an empty map if there where no mount properties
     */
    Map<String, String> getMountProperties();

    /**
     * The String value of the mount parameter.
     *
     * @param name the parameter name
     * @return the parameter value, <code>null</code> if not present
     */
    String getParameter(String name);

    /**
     * Returns an unmodifiable map of all mount parameters, empty map if none.
     *
     * @return map of all mount parameters
     */
    Map<String, String> getParameters();

    /**
     * @return the identifier of this {@link Mount}
     */
    String getIdentifier();

    /**
     * @return the repository path to the channel configuration node and <code>null</code> if not configured
     */
    String getChannelPath();

    /**
     * @param <T> Type of the channel info.  Only checked at runtime on assignment.
     * @return A channel properties instance or <code>null</code> in case {@link #getChannel()} returns <code>null</code> or
     * when the {@link ChannelInfo} interface cannot be loaded by the current classLoader
     */
    <T extends ChannelInfo> T getChannelInfo();

    /**
     * @return The {@link Channel} object instance to which this {@link Mount} belongs, or <code>null</code> if this
     * {@link Mount} does not contain a  {@link Channel} <strong>or</strong> if this {@link Mount} belongs to a different
     * webapp (contextPath) than the current webapp (contextPath)
     */
    Channel getChannel();

    /**
     * @return the String[] of defaultSiteMapItemHandlerIds which all {@link HstSiteMapItem}'s get or <code>null</code> if non configured
     */
    String[] getDefaultSiteMapItemHandlerIds();

    /**
     * @return <code>true</code> if rendering / resource requests can have their entire page http responses cached.
     */
    boolean isCacheable();

    /**
     * @deprecated Use {@link #getDefaultResourceBundleIds()} instead.
     * @return the first item of default resource bundle IDs or null if not configured or empty.
     * @deprecated since 2.28.00. Use {@link #getDefaultResourceBundleIds()} instead
     */
    @Deprecated
    String getDefaultResourceBundleId();

    /**
     * @returnM default resource bundle IDs for all sites below this mount to use, for example, { "org.example.resources.MyResources" }. Returns an empty array
     * when not configured on this {@link Mount} and empty from ancestor {@link Mount} or when root host from  {@link VirtualHost#getDefaultResourceBundleIds()}
     */
    String [] getDefaultResourceBundleIds();

    /**
     * @return the cms location (fully qualified URL) or <code>null</code> if not configured
     * @deprecated since 7.9.1 : Use {@link #getCmsLocations()} instead
     */
    @Deprecated
    String getCmsLocation();

    /**
     * @return the cms location(s) (fully qualified URL) or empty list if not configured
     */
    List<String> getCmsLocations();

}
