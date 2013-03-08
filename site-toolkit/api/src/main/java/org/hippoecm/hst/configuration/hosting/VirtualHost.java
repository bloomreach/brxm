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
package org.hippoecm.hst.configuration.hosting;

import java.util.List;

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
     * is www.apache.org, then the name of this VirtualHost might be 'www' or 'apache' or 'org'. It is thus one segment of the entire hostName.
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
     * 
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
     * In case the {@link HttpServletRequest#getContextPath()} does not matter, this method returns <code>null</code> or empty. <b>If</b> {@link Mount}s 
     * for this host can be used only for a certain contextPath, this method should return that contextPath. If configured, the contextPath is either an empty string,
     * or it must start with a "/" and cannot contain any other "/".
     * 
     * @return <code>null</code> or empty if the contextPath does not matter, otherwise the value the contextPath must have to match {@link Mount}s for this host.
     */
    String onlyForContextPath();
    
    /**
     * @return <code>true</code> when the created url should have the port in it
     */
    boolean isPortInUrl();
    
    /**
     * @return the scheme to use for creating external urls, for example http / https
     */
    String getScheme();

    /**
     * <p>
     * the response code the HST sets when {@link javax.servlet.http.HttpServletRequest} <code>scheme</code> does not match {@link #getScheme()}.
     * Default response code is {@link javax.servlet.http.HttpServletResponse#SC_MOVED_PERMANENTLY}. Typically the following response
     * code results in:
     * </p>
     * <p>
     *     <ol>
     *         <li>200 : no behavior, ignored</li>
     *         <li>301 : when request has different scheme than {@link #getScheme()}, permanent redirect to the correct scheme is done</li>
     *         <li>307 : when request has different scheme than {@link #getScheme()}, temporal redirect to the correct scheme is done</li>
     *         <li>403 : when request has different scheme than {@link #getScheme()}, a page forbidden is returned</li>
     *         <li>404 : when request has different scheme than {@link #getScheme()}, a page not found is returned</li>
     *     </ol>
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
     * The scheme is 'http' by default, unless {@link # getScheme()} returns something else 
     * The hostname is the HttpServeltRequest request.getServerName() (proxies must have <code>ProxyPreserveHost On</code>)
     * The portnumber is as follows: 
     * <ul>
     *   <li>when {@link #isPortVisible()} is <code>false</code>, there is no portnumber</li>
     *   <li>otherwise: 
     *       <ul>
     *          <li><code>port = {@link #getPortNumber()}</code></li>
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
     * @return default resource bundle for all sites below this virtualhost to use, for example org.example.resources.MyResources. Returns <code>null</code>
     * when not configured on this {@link VirtualHost} and <code>null</code> from ancestor {@link VirtualHost} or when root host from  {@link VirtualHosts#getDefaultResourceBundleId()}
     */
    String getDefaultResourceBundleId();

}
