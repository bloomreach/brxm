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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.util.HttpUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation providing HstContainerURL.
 * This implementation assume the urls are like the following examples:
 * <code><xmp>
 * 1) render url will not be encoded : http://localhost/site/content/news/2008/08
 * 2) action url will be encoded     : http://localhost/site/content/_hn:<encoded_params>/news/2008/08
 *          the <encoded_params>     : <request_type>|<action reference namespace>|<params query string>
 * 3) resource url will be encoded   : http://localhsot/site/content/_hn:<encoded_params>/news/2008/08
 *          the <encoded_params>     : <request_type>|<resource reference namespace>|<resource ID>
 * </xmp></code> 
 * 
 * @version $Id$
 */
public abstract class AbstractHstContainerURLProvider implements HstContainerURLProvider {
    
    protected final static Logger log = LoggerFactory.getLogger(AbstractHstContainerURLProvider.class);
    
    protected static final String REQUEST_INFO_SEPARATOR = "|";

    protected static final String DEFAULT_HST_URL_NAMESPACE_PREFIX = "_hn:";

    protected String urlNamespacePrefix = DEFAULT_HST_URL_NAMESPACE_PREFIX;
    protected String urlNamespacePrefixedPath = "/" + urlNamespacePrefix;
    protected String parameterNameComponentSeparator = ":";
    
    protected HstNavigationalStateCodec navigationalStateCodec;
    
    public void setUrlNamespacePrefix(String urlNamespacePrefix) {
        this.urlNamespacePrefix = urlNamespacePrefix;
        this.urlNamespacePrefixedPath = "/" + urlNamespacePrefix;
    }
    
    public String getUrlNamespacePrefix() {
        return this.urlNamespacePrefix;
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public String getParameterNameComponentSeparator() {
        return this.parameterNameComponentSeparator;
    }
    
    public void setNavigationalStateCodec(HstNavigationalStateCodec navigationalStateCodec) {
        this.navigationalStateCodec = navigationalStateCodec;
    }
    
    public HstNavigationalStateCodec getNavigationalStateCodec() {
        return this.navigationalStateCodec;
    }

    public HstContainerURL parseURL(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext) {
        return parseURL(servletRequest, servletResponse, requestContext, null);
    }
    
    public HstContainerURL parseURL(ServletRequest servletRequest, ServletResponse servletResponse, HstRequestContext requestContext, String pathInfo) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        HstContainerURLImpl url = new HstContainerURLImpl();
        
        // requestContext now is a required parameter...
        HstContainerURL baseURL = requestContext.getBaseURL();

        if (baseURL != null) {
            url.setContextPath(baseURL.getContextPath());
            url.setServletPath(baseURL.getServletPath());
        }
        else {
            url.setContextPath(request.getContextPath());
            url.setServletPath(request.getServletPath());
        }
        
        String characterEncoding = request.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "ISO-8859-1";
        }
        
        url.setCharacterEncoding(characterEncoding);
        
        String namespacedPathPart = null;
        // pathInfo argument is always passed when a navigational url is needed; 
        // navigational urls cannot have namespaced path part.
        if (pathInfo == null) {
            String [] namespacedPartAndPathInfo = splitPathInfo(request, characterEncoding);
            namespacedPathPart = namespacedPartAndPathInfo[0];
            pathInfo = namespacedPartAndPathInfo[1];
        }
        
        url.setPathInfo(pathInfo);
        
        try {
            if (namespacedPathPart != null) {
                String encodedInfo = namespacedPathPart.substring(urlNamespacePrefixedPath.length());
                String [] requestInfos = StringUtils.splitByWholeSeparatorPreserveAllTokens(encodedInfo, REQUEST_INFO_SEPARATOR, 3);
                
                String requestType = requestInfos[0];
                
                if (HstURL.ACTION_TYPE.equals(requestType)) {
                    String actionWindowReferenceNamespace = requestInfos[1];
                    url.setActionWindowReferenceNamespace(actionWindowReferenceNamespace);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("action window chosen for {}: {}", url.getPathInfo(), actionWindowReferenceNamespace);
                    }
                } else if (HstURL.RESOURCE_TYPE.equals(requestType)) {
                    String resourceWindowReferenceNamespace = requestInfos[1];
                    String resourceId = requestInfos.length > 2 ? requestInfos[2] : null;
                    
                    if (resourceId != null) {
                        // resource id is double-encoded because it can have slashes,
                        // which can make a problem in Tomcat 6.
                        resourceId = URLDecoder.decode(resourceId, characterEncoding);
                    }
                    
                    url.setResourceId(resourceId);
                    url.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("resource window chosen for {}: {}", url.getPathInfo(), resourceWindowReferenceNamespace + ", " + resourceId);
                    }
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid container URL path: {}", pathInfo, e);
            } else if (log.isWarnEnabled()) {
                log.warn("Invalid container URL path: {}", pathInfo);
            }
        }
        
        Map<String, String []> paramMap = HttpUtils.parseQueryString(request);
        url.setParameters(paramMap);
        
        return url;
    }
    
    public HstContainerURL createURL(HstContainerURL baseContainerURL, HstURL hstUrl) {
        HstContainerURLImpl containerURL = (HstContainerURLImpl) ((HstContainerURLImpl) baseContainerURL).clone();
        containerURL.setActionWindowReferenceNamespace(null);
        containerURL.setResourceWindowReferenceNamespace(null);
        
        String type = hstUrl.getType();
        
        if (HstURL.ACTION_TYPE.equals(type)) {
            containerURL.setActionWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        } else if (HstURL.RESOURCE_TYPE.equals(type)) {
            containerURL.setResourceWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            containerURL.setResourceId(hstUrl.getResourceID());
        } else {
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        }
        
        return containerURL;
    }
    
    public void mergeParameters(HstContainerURL containerURL, String referenceNamespace, Map<String, String []> parameterMap) {
        if (referenceNamespace != null ) {
            String prefix = "";
            if (!referenceNamespace.equals("")) {
                prefix = referenceNamespace + parameterNameComponentSeparator;
                int prefixLen = prefix.length();
                String name;
                if (containerURL.getParameterMap() != null) {
                    // first drop existing referenceNamespace parameters
                    for (Iterator<Map.Entry<String,String[]>> iter = containerURL.getParameterMap().entrySet().iterator(); iter.hasNext(); ) {
                        name = iter.next().getKey();
                        if (name.startsWith(prefix) && name.length() > prefixLen) {
                            iter.remove();
                        }
                    }
                }
            }
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String [] values = entry.getValue();
                if (values == null || values.length == 0) {
                    containerURL.setParameter(prefix+entry.getKey(), (String) null);
                } else {
                    containerURL.setParameter(prefix+entry.getKey(), values);
                }
            }
        }
    }
    
    public String toContextRelativeURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        StringBuilder url = new StringBuilder(100);
        url.append(containerURL.getServletPath());
        String pathInfo = buildHstURLPath(containerURL);
        url.append(pathInfo);
        return url.toString();
    }
    
    public abstract String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException;

    public abstract String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException;
    
    protected String buildHstURLPath(HstContainerURL containerURL) throws UnsupportedEncodingException {
        String characterEncoding = containerURL.getCharacterEncoding();
        StringBuilder url = new StringBuilder(100);
        
        if (containerURL.getActionWindowReferenceNamespace() != null) {
            url.append(this.urlNamespacePrefixedPath);
            
            StringBuilder sbRequestInfo = new StringBuilder(80);
            sbRequestInfo.append(HstURL.ACTION_TYPE).append(REQUEST_INFO_SEPARATOR);
            sbRequestInfo.append(containerURL.getActionWindowReferenceNamespace()).append(REQUEST_INFO_SEPARATOR);
            
            url.append(URLEncoder.encode(sbRequestInfo.toString(), characterEncoding));
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            url.append(this.urlNamespacePrefixedPath);
            
            String resourceId = containerURL.getResourceId();
            // resource id should be double-encoded because it can have slashes,
            // which can make a problem in Tomcat 6.
            if (resourceId != null) {
                resourceId = URLEncoder.encode(resourceId, characterEncoding);
            }
            
            String requestInfo = 
                HstURL.RESOURCE_TYPE + REQUEST_INFO_SEPARATOR + 
                containerURL.getResourceWindowReferenceNamespace() + REQUEST_INFO_SEPARATOR + 
                (resourceId != null ? resourceId : "");
            
            url.append(URLEncoder.encode(requestInfo, characterEncoding));
        }
        
        String[] unEncodedPaths = containerURL.getPathInfo().split("/");
        for(String path : unEncodedPaths) {
            if(!"".equals(path)) {
                url.append("/").append(URLEncoder.encode(path, characterEncoding));
            }
        }
        
        boolean firstParamDone = (url.indexOf("?") >= 0);
        
        Map<String, String []> parameters = containerURL.getParameterMap();
        
        if (parameters != null) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String name = entry.getKey();
                
                for (String value : entry.getValue()) {
                    url.append(firstParamDone ? "&" : "?")
                    .append(name)
                    .append("=")
                    .append(URLEncoder.encode(value, characterEncoding));
                    
                    firstParamDone = true;
                }
            }
        }
        
        return url.toString();
    }
    
    protected String getVirtualizedContextPath(HstContainerURL containerURL, HstRequestContext requestContext, String path) {
        String virtualizedContextPath = containerURL.getContextPath();
        
        HstContainerURL baseURL = requestContext.getBaseURL();
        if (baseURL != null) {
            
            VirtualHost virtualHost = requestContext.getVirtualHost();
            
            if (virtualHost != null && path != null) {
                if (virtualHost.isContextPathInUrl()) {
                    virtualizedContextPath = baseURL.getContextPath();
                } else {
                    virtualizedContextPath = "";
                }
            }
        }
        
        return virtualizedContextPath;
    }
    
    protected String getVirtualizedServletPath(HstContainerURL containerURL, HstRequestContext requestContext, String path) {
        String virtualizedServletPath = containerURL.getServletPath();
        
        HstContainerURL baseURL = requestContext.getBaseURL();

        if (baseURL != null) {
            VirtualHost virtualHost = requestContext.getVirtualHost();
            
            if (virtualHost != null && path != null) {
                if (virtualHost.getVirtualHosts().isExcluded(path)) {
                    // if the path is an excluded path defined in virtual hosting (for example /binaries), we do not include
                    // a servletpath in the url
                    virtualizedServletPath = "";
                } else {
                    // as the external url is mapped, get the external 'fake' servletpath
                    virtualizedServletPath = requestContext.getMatchedMapping().getMapping().getUriPrefix();
                    if (virtualizedServletPath == null) {
                        virtualizedServletPath = "";
                    }
                }
                if (virtualizedServletPath.endsWith("/")) {
                    virtualizedServletPath = virtualizedServletPath.substring(0, virtualizedServletPath.length() - 1);
                }
            }
        }
        
        return virtualizedServletPath;
    }
    
    /*
     * Splits path info to an array of namespaced path part and remainder. 
     */
    protected String [] splitPathInfo(HttpServletRequest request, String characterEncoding) {
        /*
         * Do not use request.getPathInfo() as this path is already decoded by the 
         * the web container making it impossible for us to distuinguish between a space and 
         * a plus as both are represented by a '+' in the getPathInfo
         */ 
        
        String pathInfo = HstRequestUtils.getPathInfo(request, characterEncoding);
        
        if (!pathInfo.startsWith(urlNamespacePrefixedPath)) {
            return new String [] { null, pathInfo };
        }
        
        String requestURI = (String) request.getAttribute("javax.servlet.include.request_uri");
        
        if (requestURI == null) {
            requestURI = request.getRequestURI();
        }
        
        String temp = requestURI.substring(requestURI.indexOf(urlNamespacePrefixedPath));
        int offset = temp.indexOf('/', 1);
        String namespacedPathPart = temp.substring(0, offset);
        String pathInfoFromURI = "";

        try {
            namespacedPathPart = URLDecoder.decode(namespacedPathPart, characterEncoding);
            pathInfoFromURI = URLDecoder.decode(temp.substring(offset), characterEncoding);
        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid request uri: {}", requestURI, e);
            } else if (log.isWarnEnabled()) {
                log.warn("Invalid request uri: {}", requestURI);
            }
        }
        
        return new String [] { namespacedPathPart, pathInfoFromURI };
    }
    
}
