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
package org.hippoecm.hst.configuration.sitemap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.component.HstComponent;

/**
 * A <code>HstSiteMapItem<code> is used as a representation of a logical path (element) for matching (part of a) external URL
 * to a repository content location. Through its id it can be directly accessed by the {@link HstSiteMap}, to for example
 * create a link to it. Obviously, creating a link to a <code>HstSiteMapItem</code> does only make sense when its path in the
 * <code>HstSiteMap</code> does not contain WILDCARD's ( <code>*</code> or <code>**</code>)
 *
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstSiteMapItem</code> instances but should not be able to modify them,
 * implementations must make sure that through the api a <code>HstSiteMapItem</code> instance cannot be changed. Returned List and Map
 * should be therefor unmodifiable.
 */

public interface HstSiteMapItem {

    /**
     * The id of a <code>HstSiteMapItem</code> is mandatory and must be unique within its containing <code>{@link HstSiteMap}</code> because
     * <code>{@link HstSiteMap#getSiteMapItemById(String)}</code> must uniquely return a <code>HstSiteMapItem</code>.
     * @return the id of this HstSiteMapItem
     */
    String getId();

    /**
    * The refId of a <code>HstSiteMapItem</code> is <b>non</b>-mandatory and must be unique within its containing <code>{@link HstSiteMap}</code> because
    * <code>{@link HstSiteMap#getSiteMapItemByRefId(String)}</code> must uniquely return a <code>HstSiteMapItem</code>. The difference with {@link #getId()} is that
    * that <code>id</code> is in general an auto-generated id, where this {@link #getRefId()} is an optional id to get hold of this {@link HstSiteMapItem}.
    * @return the refId of this HstSiteMapItem or <code>null</code> when no refId is defined on the {@link HstSiteMapItem}
    */
   String getRefId();

    /**
     * The qualified id, which might contain more info then just {@link #getId()} as the getId might return an id which is meaningfull only within it's current {@link HstSiteMap}
     * @return the qualified id of this HstSiteMapItem
     */
    String getQualifiedId();

    /**
     * Returns the logical path element of this <code>SiteMapItem</code>. The constraint to the return value is, that it needs to be
     * unique within the sibbling <code>HstSiteMapItem</code>'s because it is used as a key for <code>{@link #getChild(String)}</code>
     * and <code>{@link HstSiteMap#getSiteMapItem(String)}</code>
     * @return the value of this <code>SiteMapItem</code> which represents the logical path element for this <code>SiteMapItem</code>
     */
    String getValue();

    /**
     * @return the page title for this {@link HstSiteMapItem} or <code>null</code> if not configured
     */
    String getPageTitle();

    /**
     * Returns a boolean indicating whether this <code>HstSiteMapItem</code> represents a path with a <code>wildcard</code> value <code>*</code>
     * @return <code>true</code> if this <code>HstSiteMapItem</code> represents <code>*</code>
     */
    boolean isWildCard();

    /**
     * @return <code>true</code> when this <code>HstSiteMapItem</code> represents a path that contains a <code>*</code> but
     * is not equals to a <code>*</code> (for example *.html)
     */
    boolean containsWildCard();

    /**
     * Returns a boolean indicating whether this <code>HstSiteMapItem</code> represents a path with a <code>any</code> value <code>**</code>
     * @return <code>true</code> if this <code>HstSiteMapItem</code> represents <code>**</code>
     */
    boolean isAny();

    /**
     * @return <code>true</code> when this <code>HstSiteMapItem</code> represents a path that contains <code>**</code> but
     * is not equals to <code>**</code> (for example **.html)
     */
    boolean containsAny();

    /**
     * This method returns a content path, relative to the {@link Mount#getContentPath()}. This value can
     * contain property placeholders, like ${1}/${2}, which should be resolved in the {@link org.hippoecm.hst.core.request.ResolvedSiteMapItem#getRelativeContentPath()}
     * @return the content path relative to the {@link Mount#getContentPath()}
     */
    String getRelativeContentPath();

    /**
     * If a HstSiteMapItem can be used to resolve a url, it must have a componentConfigurationId referencing
     * the component configuration the will handle the request processing. This component configuration can be
     * the root of a component configuration tree, see {@link HstComponentConfiguration}.
     * @return the componentConfigurationId for this <code>SiteMapItem</code> or <code>null</code>
     */
    String getComponentConfigurationId();

    /**
     * @return the {@link Map} of keys to more specific configurationId's and <code>null</code> if there are no mappings defined
     * @see #getComponentConfigurationId()
     */
    Map<String, String> getComponentConfigurationIdMappings();

    /**
     * If this method returns true, then only if the user is explicitly allowed or <code>servletRequest.isUserInRole(role)</code> returns <code>true</code> this
     * sitemap item is accessible for the request.
     *
     * If a sitemap item does not have a configuration for authenticated, the value from the parent item is taken. The root sitemap items
     * return by default <code>false</code> for {@link #isAuthenticated()} when no configuration is set for authenticated.
     *
     * @return <code>true</code> if the sitemap item is authenticated.
     */
    boolean isAuthenticated();

    /**
     * Returns the roles that are allowed to access this sitemap item when {@link #isAuthenticated()} is true. If the sitemap items does not have any roles defined by itself, it
     * inherits them from the parent. If it defines roles, the roles from any ancestor are ignored. An empty set of roles
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     *
     * {@link HstComponent} instances can access <code>HstSiteMapItem</code> but should not be able to modify them, implementations
     * should return an unmodifiable set.
     *
     * @return The set of roles that are allowed to access this sitemap item. When no roles defined, the roles from the parent item are inherited. If none of the
     * parent items have a role defined, an empty set is returned
     */
    Set<String> getRoles();

    /**
     * Returns the users that are allowed to access this sitemap item when {@link #isAuthenticated()} is true. If the sitemap items does not have any users defined by itself, it
     * inherits them from the parent. If it defines users, the users from any ancestor are ignored. An empty set of users
     * in combination with {@link #isAuthenticated()} return <code>true</code> means nobody has access to the item
     *
     * {@link HstComponent} instances can access <code>HstSiteMapItem</code> but should not be able to modify them, implementations
     * should return an unmodifiable set.
     *
     * @return The set of users that are allowed to access this sitemap item. When no users defined, the users from the parent item are inherited. If none of the
     * parent items have a user defined, an empty set is returned
     */
    Set<String> getUsers();

    /**
     * <p>
     * When having more sitemapitem as sibblings, for example foo.xml, foo.html and foo.rss, you might not want all three being used for linkrewriting: Even worse, if
     * they all three have the same {@link #getRelativeContentPath()}, we cannot choose which one is the one you want, so, one of the items is used. When configuring
     * a SiteMapItem to return <code>true</code> for isExcludedForLinkRewriting(), you can exclude the item to be used for linkrewriting. Thus, setting the .rss and .xml
     * version to <code>true</code> will make sure that linkrewriting returns you the .html links.
     * </p>
     * <p>
     * <b>Note</b> that this value is <b>not</b> inherited from ancestor HstSiteMapItem's
     * </p>
     * @return <code>true</code> when this sitemap item should be ignored for linkrewriting
     */
    boolean isExcludedForLinkRewriting();

    /**
     * Returns a <code>List</code> of all child <code>HstSiteMapItem</code>'s of this <code>HstSiteMapItem</code>.
     * Implementations should return an unmodifiable list, for example {@link java.util.Collections#unmodifiableList} to avoid
     * client code changing configuration
     * @return the List of HstSiteMapItem children. If there are no children, an empty list is returned
     */
    List<HstSiteMapItem> getChildren();

    /**
     * Return the child <code>HstSiteMapItem</code> that has the corresponding <code>value</code> ({@link HstSiteMapItem#getValue()} )
     * and <code>null</code> otherwise
     * @param value the value of the child <code>HstSiteMapItem</code> as it would be return by {@link HstSiteMapItem#getValue()}
     * @return Returns the HstSiteMapItem object corresponding to the unique <code>value</code> and <code>null</code> if no <code>HstSiteMapItem</code>
     * exists with this <code>value</code> in this <code>HstSiteMapItem</code> object.
     */
    HstSiteMapItem getChild(String value);

    /**
     * A HstSiteMapItem can contain a Map of parameters. These parameters can be accessed from {@link HstComponent}'s instances through
     * a parameter in the {@link HstComponentConfiguration}. For example, if this <code>SiteMapItem</code> would have a parameter named
     * <code>foo</code> and value <code>bar</code>, the {@link HstComponentConfiguration} linked through the
     * {@link #getComponentConfigurationId()} can access this parameters by having an own parameter,
     * for example named <code>lux</code> and the value <code>${foo}</code>. If the <code>HstSiteMapItem</code> is a WILDCARD or any of its
     * ancestors, you can also set the parameter values to <code>${1}</code>, <code>${2}</code> etc where <code>${1}</code> refers to the
     * first matched wildcard, <code>${2}</code> to the second, etc.
     *
     * Parameters are inherited from ancestor sitemap items. When this sitemap item configures the same parameter as an ancestor, the
     * value from the ancestor is overwritten. Thus, child items have precedence. Note that this is opposite to {@link HstComponentConfiguration#getParameter(String)}
     *
     * @param name the name of the parameter
     * @return the value of the parameter or <code>null</code> when not present
     */
    String getParameter(String name);

    /**
     * see {@link #getParameter(String)}, only this method returns parameters without inheritance
     * @param name  the name of the parameter
     * @return the value of the parameter or <code>null</code> when not present
     */
    String getLocalParameter(String name);

    /**
     * See {@link #getParameter(String)}, only now entire the parameters map is returned.
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections#unmodifiableMap} to avoid
     * client code changing configuration
     *
     * Parameters are inherited from ancestor sitemap items. When this sitemap item configures the same parameter as an ancestor, the
     * value from the ancestor is overwritten. Thus, child items have precedence. Note that this is opposite to {@link HstComponentConfiguration#getParameters()}
     *
     *
     * @return the Map of parameters contained in this <code>HstSiteMapItem</code>. If no parameters present, and empty map is returned
     */
    Map<String, String> getParameters();

    /**
     * see {@link #getParameters }, only this method returns parameters (unmodifiable map) without inheritance
     * @return the Map of parameters contained in this <code>HstSiteMapItem</code>. If no parameters present, and empty map is returned
     */
    Map<String, String> getLocalParameters();

    /**
     * Returns parent <code>HstSiteMapItem</code> and <code>null</code> when the item does not have a parent (in other words, it is a
     * root <code>HstSiteMapItem</code>)
     * @return the parent <code>HstSiteMapItem</code> and <code>null</code> when the item does not have a parent
     */
    HstSiteMapItem getParentItem();

    /**
     * Return the <code>HstSiteMap</code> that is the container of this <code>HstSiteMapItem</code>
     * @return the <code>HstSiteMap</code> that is the container of this <code>HstSiteMapItem</code>
     */
    HstSiteMap getHstSiteMap();

    /**
     * A <code>HstSiteMapItem</code> can specify the status-code that needs to be set on the webpage header. If a status-code
     * is specified, it can be retrieved through this method. <code>0</code> is return is non is specified.
     * @return if a status code is set, return this, otherwise <code>0</code>
     */
    int getStatusCode();

    /**
     * A <code>HstSiteMapItem</code> can specify an error-code. If an error-code
     * is specified, the framework will invoke a sendError(int code) where the code is the value returned
     * by this method . <code>0</code> is return is non is specified.
     * @return if an error code is set, return this, otherwise <code>0</code>
     */
    int getErrorCode();

    /**
     * the namedPipeline for this sitemapItem or <code>null</code> when it does not contain one. Note that if an ancestor sitemapItem contains a
     * namedPipeline, this value is inherited unless this sitemapItem explicitly defines its own
     * @return the namedPipeline for this sitemapItem or <code>null</code> when it does not contain one.
     */
    String getNamedPipeline();

    /**
     * the locale for this sitemapItem or <code>null</code> when it does not contain one. Note that if an ancestor sitemapItem contains a
     * locale, this value is inherited unless this sitemapItem explicitly defines its own.
     * @return the locale for this sitemapItem or <code>null</code> when it does not contain one.
     */
    String getLocale();

    /**
     * @param handlerId
     * @return the {@link HstSiteMapItemHandlerConfiguration} for <code>handlerId</code> or <code>null</code> if no handler present for <code>handlerId</code>
     */
    HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String handlerId);

    /**
     * <p>
     * The List of {@link HstSiteMapItemHandlerConfiguration}s and an empty list if this SiteMapItem does not contain {@link HstSiteMapItemHandlerConfiguration}s.
     * <br/>
     * <b>Note</b> that {@link HstSiteMapItemHandlerConfiguration}s are  <b>NOT</b> inherited from  parent/ancestor HstSiteMapItem's.
     * </p>
     * <p>Implementations should return an unmodifiable list, for example {@link java.util.Collections#unmodifiableList} to avoid
     * client code changing configuration</p>
     * @return The List of {@link HstSiteMapItemHandlerConfiguration}s and an empty list if this SiteMapItem does not contain {@link HstSiteMapItemHandlerConfiguration}s
     */
    List<HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations();

    /**
     * @return <code>true</code> if rendering / resource requests can have their entire page http responses cached.
     */
    boolean isCacheable();

    /**
     * The scheme of a site map item specifies which scheme is to be used for serving this site map item. The value of
     * this property is derived in the following order:
     * <ol>
     *     <li>If the hst:scheme property is set for this site map item, then that is used</li>
     *     <li>If this site map item has a parent, it will use the parent's scheme</li>
     *     <li>If the mount that this site map item belongs to has a scheme set, then that is used</li>
     * </ol>
     *
     * If a site map item has an hst:scheme property, but it is left blank then the scheme defaults to
     * {@link org.hippoecm.hst.configuration.hosting.VirtualHosts#DEFAULT_SCHEME}
     * @return the scheme of this site map item
     */
    String getScheme();

    /**
     * If a {@link HstSiteMapItem} is scheme agnostic, the request gets served regardless whether it is <code>http</code> or
     * <code>https</code>
     * @return <code>true</code> when this {@link HstSiteMapItem} is scheme agnostic
     */
    boolean isSchemeAgnostic();

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
     *     Any other response code than above will result in inheriting the response code from parent {@link HstSiteMapItem} or {@link Mount}
     * </p>
     */
    int getSchemeNotMatchingResponseCode();

    /**
     * @deprecated Use {@link #getResourceBundleIds()} instead.
     * @return the first item of default resource bundle IDs or null if not configured or empty.
     */
    String getResourceBundleId();

    /**
     * @return resource bundle IDs for this sitemapitem and descendants to use, for example { "org.example.resources.MyResources" }, or an empty array
     * when not configured on this {@link HstSiteMapItem} and empty from ancestor {@link HstSiteMapItem} or when root
     * sitemapitem from {@link org.hippoecm.hst.configuration.hosting.Mount#getDefaultResourceBundleIds()}
     */
    String [] getResourceBundleIds();
}