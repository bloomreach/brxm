/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * VirtualHost which holds the mapping between host (server name) and site name.
 *
 */
public interface VirtualHost {

    /**
     * The hostName of this VirtualHost. <b>Note</b> that this hostName might contain wildcards, for example www.onehippo.*
     * @return The composite hostName of this VirtualHost, thus including parent VirtualHosts if present
     */
    String getHostName();

    /**
     * Returns the <code>name</code> of this VirtualHost. Note, this is not the hostName, but only part of it. If the hostName
     * is www.apache.org, then the name of this VirtualHost might be 'www' or 'apache' or 'org'. It can thus be one segment
     * of the entire hostName or is get be www.apache.org which is also possible
     *
     * @see #getHostName()
     * @return The <code>name</code> of this VirtualHost. Note, this is only part of the entire hostName
     */
    String getName();

    /**
     * Returns the name of host group this virtual host belongs to, for example 'prod', 'acct' or 'dev'
     * @return the <code>name</code> of the host group this VirtualHost belongs to.
     */
    String getHostGroupName();

    /**
     * the locale for this VirtualHost or <code>null</code> when it does not contain one. Note that if an ancestor VirtualHost contains a
     * locale, this value is inherited unless this VirtualHost explicitly defines its own. The VirtualHost directly below the  {@link VirtualHosts} inherits the value from
     * the {@link VirtualHosts}
     * @return the locale for this VirtualHost or <code>null</code> when it does not contain one.
     */
    String getLocale();

    /**
     * @param name the name segment of the hostname
     * @return the child <code>VirtualHost</code> or <code>null</code> if none found
     */
    VirtualHost getChildHost(String name);

    /**
     * @return the list of all VirtualHost childs of this VirtualHost
     */
    List<VirtualHost> getChildHosts();

    /**
     * @param portNumber
     * @return the
     */
    PortMount getPortMount(int portNumber);

    /**
     * @return the <code>VirtualHosts</code> container of this <code>VirtualHost</code>
     */
    VirtualHosts getVirtualHosts();

    /**
     * @return <code>true</code> when the created url should have the contextpath in it
     */
    boolean isContextPathInUrl();

    /**
     * <p>
     *    Returns the contextpath (webapp) for all the {@link Mount}s below this {@link VirtualHost}.
     * </p>
     * @return Returns the contextpath (webapp) for all the {@link Mount}s below this {@link VirtualHost}.
     * The contextpath for the ROOT application must be an empty String. If non-empty, a path starting with a "/" character
     * but that does not end with a "/" character must be returned. It is not allowed to return <code>null</code>
     * @see org.hippoecm.hst.configuration.hosting.Mount#getContextPath()
     */
    String getContextPath();

    /**
     * @return <code>true</code> when the created url should have the port in it
     */
    boolean isPortInUrl();

    /**
     * @return the scheme to use for creating external urls, for example http / https
     */
    String getScheme();

    /**
     * If a {@link VirtualHost} is scheme agnostic, the request gets served regardless whether it is <code>http</code> or
     * <code>https</code> (assuming {@link Mount} and {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} do not override the value)
     * @return <code>true</code> when this {@link VirtualHost} is scheme agnostic
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
     *     Any other response code than above will result in inheriting the response code from parent {@link VirtualHost} or {@link VirtualHosts}
     * </p>
     */
    int getSchemeNotMatchingResponseCode();

    /**
     * @return the homepage for this virtual host or <code>null</code> when not present
     */
    String getHomePage();

    /**
     * Returns the base of the <code>URL</code> as seen by for example a browser. The base URL is consists of <code>scheme + hostname + portnumber</code>
     * for example 'http://www.hippoecm.org:8081'
     *
     * The scheme is 'http' by default, unless {@link #getScheme()} returns something else
     * The hostname is the HttpServeltRequest request.getServerName() (proxies must have <code>ProxyPreserveHost On</code>)
     * The portnumber is as follows:
     * <ul>
     *   <li>when {@code #isPortVisible()} is <code>false</code>, there is no portnumber</li>
     *   <li>otherwise:
     *       <ul>
     *          <li><code>port = {@code #getPortNumber()}</code></li>
     *          <li><code>if (port == 0) {port = request.getServerPort()}</code></li>
     *          <li>if(port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme)): no portnumber will be in baseUrl
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param request the HttpServletRequest
     * @return the <code>URL</code> until the context path, thus <code>scheme + hostname + portnumber</code>, for example 'http://www.hippoecm.org:8081'
     */
    String getBaseURL(HttpServletRequest request);

    /**
     * @return the pagenotfound for this {@link Mount} or <code>null</code> when not present
     */
    String getPageNotFound();

    /**
     * @return whether the version of the HST is in the header of the preview
     */
    boolean isVersionInPreviewHeader();

    /**
     * @return <code>true</code> if rendering / resource requests can have their entire page http responses cached.
     */
    boolean isCacheable();

    /**
     * @return default resource bundle IDs for all sites below this virtualhost to use, for example { "org.example.resources.MyResources" }. Returns empty array
     * when not configured on this {@link VirtualHost} and empty from ancestor {@link VirtualHost} or when root host from  {@link VirtualHosts#getDefaultResourceBundleIds()}
     */
    String [] getDefaultResourceBundleIds();

    /**
     * @return <code>null</code> or empty String in case no cdn host is used and otherwise the cdn host to be used for URLs to sitemap items that have
     * {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem#isContainerResource() HstSiteMapItem#isContainerResource() = true}.
     * Preferred format for a cdn host is //hostname so it piggybacks on the scheme of the current request. Starting with
     * http/https is allowed but is not preferred
     */
    String getCdnHost();

    /**
     * <p>
     *     Returns the 'HST Link URL prefix' in case one is configured
     * </p>
     * @return
     */
    String getHstLinkUrlPrefix();

    /**
     * @return <code>true</code> if this {@link VirtualHost} allows a {@link HttpServletRequest} over <code>https</code> to
     * be rendered while the matched {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem} or {@link Mount} indicates
     * through <code>getScheme()</code> the request should be <code>http</code>
     */
    boolean isCustomHttpsSupported();

    /**
     * <p>
     *     Return a non-null unmodifiable map of the configuration values of HTTP Response headers which should be set
     *     in any responses by the requests on this. They keys from the returned map are the header names.
     * </p>
     * <p>
     *     Note that the header names returned by this method overwrites any already set headers during request processing
     *     with the same name
     * </p>
     * @return a non-null unmodifiable map of the configuration values of HTTP Response headers which should be set
     * in any responses by the requests on this.
     */
    Map<String, String> getResponseHeaders();


    /**
     * <p>
     *     Returns all the allowed origins for this {@link VirtualHost}
     * </p>
     * @return unmdifiable collection of the allowed origins for this virtualhost used in case of a CORS OPTIONS request,
     * if no allowed origins specified, the parent virtual host its allowed origins will be used and if there is no
     * parent an empty list will be returned.
     */
    Collection<String> getAllowedOrigins();
}
