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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;

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
     * @return
     */
    String getParameterNameComponentSeparator();

    /**
     * Parses the current request and create an {@link HstContainerURL} instance.
     * 
     * @param servletRequest
     * @param servletResponse
     * @return
     */
    HstContainerURL parseURL(ServletRequest servletRequest, ServletResponse servletResponse);
    
    /**
     * Parses the current request with the current {@link HstReqeustContext} and a specified pathInfo
     * and creates an {@link HstContainerURL} instance.
     * 
     * @param servletRequest
     * @param servletResponse
     * @param requestContext
     * @param pathInfo
     * @return
     */
    HstContainerURL parseURL(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, String pathInfo);
    
    /**
     * Creates an {@link HstContainerURL} instance by merging the information of hstUrl 
     * based on the base {@link HstContainerURL} instance
     * 
     * @param baseContainerURL
     * @param hstUrl
     * @return
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
     * @return
     * @throws UnsupportedEncodingException
     * @throws ContainerException
     */
    String toContextRelativeURLString(HstContainerURL containerURL) throws UnsupportedEncodingException, ContainerException;
    
    /**
     * Stringifying the containerURL as a full URL string.
     * 
     * @param containerURL
     * @param requestContext
     * @return
     * @throws UnsupportedEncodingException
     * @throws ContainerException
     */
    String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException;
    
}
