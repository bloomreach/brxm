/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * The URL provider for the {@link HstContainerURL}.
 * This is responsible for creating, parsing and stringifying the {@link HstContainerURL}.
 * 
 * @version $Id$
 */
public interface HstContainerURLProvider {
    
    /**
     * Sets the parameter name component separator.
     * If this is set to ':' and the parameter name is 'p1' with value 'v1', then
     * the parameter pair for the HstComponent having the reference namespace 'a' should be 'a:p1=v1'.
     * 
     * @param parameterNameComponentSeparator
     */
    void setParameterNameComponentSeparator(String parameterNameComponentSeparator);

    /**
     * Returns the parameter name component separator.
     * 
     * @return the parameter name component separator.
     */
    String getParameterNameComponentSeparator();

    /**
     * Parses the current request and creates an {@link HstContainerURL} instance.
     * 
     * @param servletRequest
     * @param servletResponse
     * @param resolvedMount
     */
    HstContainerURL parseURL(HttpServletRequest servletRequest, HttpServletResponse servletResponse, ResolvedMount resolvedMount);
    
    /**
     * Parses a specified requestPath with the current {@link HstRequestContext} for a new {@link ResolvedMount} and requestPath
     * and creates an {@link HstContainerURL} instance.
     * 
     * @param requestContext
     * @param mount
     * @param requestPath
     * @param queryParams
     */
    HstContainerURL parseURL(HstRequestContext requestContext, ResolvedMount mount, String requestPath, Map<String, String []> queryParams);
    
    /**
     * Parses a request for a specific {@link ResolvedMount} and requestPath
     * and creates an {@link HstContainerURL} instance.
     * 
     * @param request
     * @param mount
     * @param requestPath
     * @param queryParams
     */
    HstContainerURL parseURL(HttpServletRequest request, ResolvedMount mount, String requestPath, Map<String, String []> queryParams);

    /**
     * Parses a request for a specific {@link ResolvedMount}, contextPath, requestPath and request character and uri
     * encoding and creates an {@link HstContainerURL} instance. This operation doesn't depend on HttpServletRequest.
     * 
     * @param mount
     * @param contextPath
     * @param requestPath
     * @param queryParams
     * @param requestCharacterEncoding
     * @param requestURIEncoding
     */
    HstContainerURL parseURL(ResolvedMount mount, String contextPath, String requestPath, Map<String,
            String []> queryParams, String requestCharacterEncoding, String requestURIEncoding);

    /**
     * Creates an {@link HstContainerURL} instance for a new pathInfo (without query parameters)
     * based on the base {@link HstContainerURL} instance
     * 
     * @param baseContainerURL
     * @param pathInfo
     */
    HstContainerURL createURL(HstContainerURL baseContainerURL, String pathInfo);
    
    /**
     * Creates an {@link HstContainerURL} instance for a new pathInfo (without query parameters)
     * based on the {@link Mount} and the base {@link HstContainerURL} instance. 
     * @param mount
     * @param baseURL
     * @param path
     */
    HstContainerURL createURL(Mount mount, HstContainerURL baseURL, String path);
     
    /**
     * Creates an {@link HstContainerURL} instance by merging the information of hstUrl 
     * based on the base {@link HstContainerURL} instance
     * 
     * @param baseContainerURL
     * @param hstUrl
     */
    HstContainerURL createURL(HstContainerURL baseContainerURL, HstURL hstUrl);
    
    /**
     * Merges the render parameters into the containerURL.
     * 
     * @param containerURL
     * @param referenceNamespace
     * @param parameters
     */
    void mergeParameters(HstContainerURL containerURL, String referenceNamespace, Map<String, String []> parameters);
    
    /**
     * Stringifying the containerURL as a context relative path.
     * 
     * @param containerURL
     * @param requestContext
     * @throws UnsupportedEncodingException
     * @throws ContainerException
     * @deprecated deprecated since 2.30.00 (CMS 10.0), use
     * {@link #toURLString(HstContainerURL, org.hippoecm.hst.core.request.HstRequestContext)} instead
     */
    @Deprecated
    String toContextRelativeURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException;
    
    /**
     * Stringifying the containerURL as a full URL string.
     * 
     * @param containerURL
     * @param requestContext
     * @throws UnsupportedEncodingException
     * @throws ContainerException
     */
    String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException;
    
    /**
     * Stringifying the containerURL as a full URL string.
     * <P>
     * By this method, you can specify the context path such as '/mycontext'.
     * </P>
     * 
     * @param containerURL
     * @param requestContext
     * @param contextPath
     * @throws UnsupportedEncodingException
     * @throws ContainerException
     */
    String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException;

}
