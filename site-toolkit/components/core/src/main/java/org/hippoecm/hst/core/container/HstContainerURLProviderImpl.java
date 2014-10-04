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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation providing HstContainerURL.
 * This implementation assume the urls are like the following examples:
 * <P>
 * <code><xmp>
 * 1) render url will not be encoded : http://localhost/site/content/news/2008/08
 * 2) action url will be encoded     : http://localhost/site/content/news/2008/08?_hn:type=action&_hn:ref=r1
 *          the _hn:type parameter, 'action', means the request is for ACTION phase,
 *          and the _hn:ref parameter, 'r1', means the ACTION request is targeting to the component window referenced by 'r1'.
 * 3) resource url will be encoded   : http://localhsot/site/content/news/2008/08?_hn:type=resource&_hn:ref=r1&_hn:rid=resourceId
 *          the _hn:type parameter, 'resource', means the request is for RESOURCE phase,
 *          the _hn:ref parameter, 'r1', means the RESOURCE request is targeting to the component window referenced by 'r1'.
 *          and the _hn:rid parameter, 'resourceId', means the RESOURCE ID of the RESOURCE request.
 * 4) component rendering url will be encoded : http://localhsot/site/content/news/2008/08?_hn:type=component-rendering&_hn:ref=r1
 *          the _hn:type parameter, 'component-rendering', means the request is for COMPONENT RENDERING phase,
 *          and the _hn:ref parameter, 'r1', means the COMPONENT RENDERING request is targeting to the component window referenced by 'r1'.
 * </xmp></code> 
 * </P>
 * 
 * @version $Id$
 */
public class HstContainerURLProviderImpl implements HstContainerURLProvider {

    protected final static Logger log = LoggerFactory.getLogger(HstContainerURLProvider.class);

    protected static final String REQUEST_INFO_SEPARATOR = "|";

    protected static final String DEFAULT_HST_URL_NAMESPACE_PREFIX = "_hn:";

    protected String urlNamespacePrefix = DEFAULT_HST_URL_NAMESPACE_PREFIX;
    protected String urlNamespacedTypeParamName = urlNamespacePrefix + "type";
    protected String urlNamespacedReferenceParamName = urlNamespacePrefix + "ref";
    protected String urlNamespacedResourceIdParamName = urlNamespacePrefix + "rid";

    protected String parameterNameComponentSeparator = ":";

    protected HstNavigationalStateCodec navigationalStateCodec;

    public void setUrlNamespacePrefix(String urlNamespacePrefix) {
        this.urlNamespacePrefix = urlNamespacePrefix;
        urlNamespacedTypeParamName = urlNamespacePrefix + "type";
        urlNamespacedReferenceParamName = urlNamespacePrefix + "ref";
        urlNamespacedResourceIdParamName = urlNamespacePrefix + "rid";
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

    public HstContainerURL parseURL(HttpServletRequest request, ResolvedMount mount, String requestPath, Map<String, String []> queryParams) {
        return parseURL(mount, request.getContextPath(), requestPath, queryParams, request.getCharacterEncoding());
    }

    public HstContainerURL parseURL(ResolvedMount mount, String contextPath, String requestPath, Map<String, String []> queryParams, String requestCharacterEncoding) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        url.setContextPath(contextPath);
        url.setHostName(mount.getMount().getVirtualHost().getHostName());
        url.setPortNumber(mount.getPortNumber());
        url.setResolvedMountPath(mount.getResolvedMountPath());
        url.setRequestPath(requestPath);
        url.setPathInfo(requestPath.substring(mount.getResolvedMountPath().length()));
        String characterEncoding = StringUtils.defaultIfEmpty(requestCharacterEncoding, "ISO-8859-1");
        url.setCharacterEncoding(characterEncoding);
        url.setPathInfo(requestPath.substring(mount.getResolvedMountPath().length()));
        url.setParameters(queryParams);

        parseRequestInfo(url);

        return url;
    }

    public HstContainerURL parseURL(HstRequestContext requestContext, ResolvedMount mount, String requestPath, Map<String, String []> queryParams) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        HstContainerURL baseURL = requestContext.getBaseURL();
        url.setContextPath(baseURL.getContextPath());
        url.setHostName(baseURL.getHostName());
        url.setPortNumber(baseURL.getPortNumber());
        url.setResolvedMountPath(mount.getResolvedMountPath());
        url.setRequestPath(requestPath);
        url.setPathInfo(requestPath.substring(mount.getResolvedMountPath().length()));
        url.setCharacterEncoding(baseURL.getCharacterEncoding());
        url.setParameters(queryParams);

        parseRequestInfo(url);

        return url;
    }

    public HstContainerURL parseURL(HttpServletRequest request, HttpServletResponse response, ResolvedMount resolvedMount) {

        HstContainerURLImpl url = new HstContainerURLImpl();

        url.setContextPath(request.getContextPath());
        url.setHostName(HstRequestUtils.getFarthestRequestHost(request));
        url.setPortNumber(HstRequestUtils.getRequestServerPort(request));
        url.setRequestPath(HstRequestUtils.getRequestPath(request));
        String characterEncoding = HstRequestUtils.getCharacterEncoding(request);
        url.setCharacterEncoding(characterEncoding);

        Map<String, String []> paramMap = HstRequestUtils.parseQueryString(request);
        url.setParameters(paramMap);
        url.setResolvedMountPath(resolvedMount.getResolvedMountPath());
        url.setPathInfo(request.getPathInfo());

        parseRequestInfo(url);

        return url;
    }

    public HstContainerURL createURL(HstContainerURL baseContainerURL, String pathInfo) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        url.setContextPath(baseContainerURL.getContextPath());
        url.setHostName(baseContainerURL.getHostName());
        url.setPortNumber(baseContainerURL.getPortNumber());
        pathInfo = PathUtils.normalizePath(pathInfo);

        if (pathInfo != null) {
            pathInfo = '/' + pathInfo;
        }

        url.setRequestPath(baseContainerURL.getResolvedMountPath()+pathInfo);
        url.setCharacterEncoding(baseContainerURL.getCharacterEncoding());
        url.setResolvedMountPath(baseContainerURL.getResolvedMountPath());
        url.setPathInfo(pathInfo);

        return url;
    }

    public HstContainerURL createURL(Mount mount, HstContainerURL baseContainerURL, String pathInfo) {
        HstContainerURLImpl url = new HstContainerURLImpl();

        if (mount.isContextPathInUrl()) {
            url.setContextPath(baseContainerURL.getContextPath());
        } else {
            url.setContextPath("");
        }

        url.setCharacterEncoding(baseContainerURL.getCharacterEncoding());

        // if the Mount is port agnostic, in other words, has port = 0, we take the port from the baseContainerURL
        if (mount.getPort() == 0) {
            url.setPortNumber(baseContainerURL.getPortNumber());
        } else {
            url.setPortNumber(mount.getPort());
        }

        boolean includeTrailingSlash = false;

        if (pathInfo != null && pathInfo.endsWith(mount.getVirtualHost().getVirtualHosts().getHstManager().getPathSuffixDelimiter())) {
            // we now must not strip the trailing slash because it is part of the rest call ./
            includeTrailingSlash = true; 
        }

        // if pathInfo starts with or is equal to the rest call subPath prefix (default ./), then, we must not include a 
        // leading / to the pathInfo, because then for the homepage, which has a empty pathInfo before the subPath, we would
        // get a wrong URL like /mountPath/./subPath : It must be /mountPath./subPath instead. Thus, hence this check
        boolean includeLeadingSlash = true;

        if (pathInfo != null && pathInfo.startsWith(mount.getVirtualHost().getVirtualHosts().getHstManager().getPathSuffixDelimiter())) {
            includeLeadingSlash = false;
        }

        pathInfo = PathUtils.normalizePath(pathInfo);

        if (pathInfo != null) {
            if(includeLeadingSlash) {
                pathInfo = '/' + pathInfo;
            }

            if(includeTrailingSlash) {
                pathInfo = pathInfo + '/';
            }
        }

        url.setHostName(mount.getVirtualHost().getHostName());
        url.setRequestPath(mount.getMountPath() + pathInfo);
        url.setResolvedMountPath(mount.getMountPath());
        url.setPathInfo(pathInfo);

        return url;
    }

    public HstContainerURL createURL(HstContainerURL baseContainerURL, HstURL hstUrl) {
        HstContainerURLImpl containerURL;

        try {
            containerURL = (HstContainerURLImpl) ((HstContainerURLImpl) baseContainerURL).clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone not supported on the container url. " + e);
        }

        containerURL.setActionWindowReferenceNamespace(null);
        containerURL.setResourceWindowReferenceNamespace(null);
        containerURL.setComponentRenderingWindowReferenceNamespace(null);

        String type = hstUrl.getType();

        if (HstURL.ACTION_TYPE.equals(type)) {
            containerURL.setActionWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        } else if (HstURL.RESOURCE_TYPE.equals(type)) {
            containerURL.setResourceWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            containerURL.setResourceId(hstUrl.getResourceID());
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        } else if (HstURL.COMPONENT_RENDERING_TYPE.equals(type)) {
            containerURL.setComponentRenderingWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        } else {
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        }

        return containerURL;
    }

    public void mergeParameters(HstContainerURL containerURL, String referenceNamespace, Map<String, String []> parameterMap) {
        String prefix = (StringUtils.isEmpty(referenceNamespace) ? "" : referenceNamespace + parameterNameComponentSeparator);

        // first drop existing referenceNamespace parameters
        if (!"".equals(prefix)) {
            Map<String, String []> containerParamsMap = containerURL.getParameterMap();

            if (containerParamsMap != null && !containerParamsMap.isEmpty()) {
                int prefixLen = prefix.length();
                String paramName = null;

                for (Iterator<Map.Entry<String, String[]>> iter = containerParamsMap.entrySet().iterator(); iter.hasNext(); ) {
                    paramName = iter.next().getKey();

                    if (paramName.startsWith(prefix) && paramName.length() > prefixLen) {
                        iter.remove();
                    }
                }
            }
        }

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = prefix + entry.getKey();
            String [] paramValues = entry.getValue();

            if (paramValues == null || paramValues.length == 0) {
                containerURL.setParameter(paramName, (String) null);
            } else {
                containerURL.setParameter(paramName, paramValues);
            }
        }
    }

    public String toContextRelativeURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        StringBuilder url = new StringBuilder(100);
        String mountPrefix = containerURL.getResolvedMountPath();

        if(mountPrefix != null) {
            url.append(mountPrefix);
        }

        String pathInfo = buildHstURLPath(containerURL, requestContext);
        url.append(pathInfo);
        return url.toString();
    }

    protected String buildHstURLPath(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException {
        String characterEncoding = containerURL.getCharacterEncoding();
        StringBuilder url = new StringBuilder(100);
        String pathSuffixDelimiter = requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().getHstManager().getPathSuffixDelimiter();

        boolean includeSlash = true;

        if (containerURL.getPathInfo().startsWith(pathSuffixDelimiter)) {
            includeSlash = false;
        }

        String[] unencodedPaths = StringUtils.splitPreserveAllTokens(containerURL.getPathInfo(), '/');

        for (String path : unencodedPaths) {
            if (StringUtils.isNotEmpty(path)) {
                if (includeSlash) {
                    url.append('/');
                } else {
                    // apparently due to pathSuffixDelimiter the first / was skipped. From now include it
                    includeSlash = true;
                }

                // check if we have an anchor link and encode everything behind it, but leave first part as it is:
                if (path.indexOf('#') != -1) {
                    String[] hashParts = StringUtils.splitPreserveAllTokens(path, '#');

                    // check if preceded with query
                    if (hashParts[0].indexOf('?') != -1) {
                        String[] parameterParts = StringUtils.splitPreserveAllTokens(hashParts[0], '?');
                        url.append(URLEncoder.encode(parameterParts[0], characterEncoding))
                                .append('?').append(parameterParts[1])
                                .append('#').append(URLEncoder.encode(hashParts[1], characterEncoding));
                    }
                    else{
                        url.append(URLEncoder.encode(hashParts[0], characterEncoding)).append('#').append(URLEncoder.encode(hashParts[1], characterEncoding));
                    }
                }
                // check query parameters:
                else if (path.indexOf('?') != -1) {
                    String[] parameterParts = StringUtils.splitPreserveAllTokens(path, '?');
                    url.append(URLEncoder.encode(parameterParts[0], characterEncoding)).append('?').append(parameterParts[1]);
                } else {
                    url.append(URLEncoder.encode(path, characterEncoding));
                }
            }
        }

        if (pathSuffixDelimiter != null && containerURL.getPathInfo().endsWith(pathSuffixDelimiter) && pathSuffixDelimiter.endsWith("/")) {
            // the trailing slash is removed above, but for ./ we need to append the slash again
            url.append('/');
        }

        boolean firstParamDone = (url.indexOf("?") >= 0);

        if (containerURL.getActionWindowReferenceNamespace() != null) {
            url.append(firstParamDone ? "&" : "?").append(urlNamespacedTypeParamName).append('=').append(HstURL.ACTION_TYPE);
            url.append('&').append(urlNamespacedReferenceParamName).append('=').append(URLEncoder.encode(containerURL.getActionWindowReferenceNamespace(), characterEncoding));
            firstParamDone = true;
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            url.append(firstParamDone ? "&" : "?").append(urlNamespacedTypeParamName).append('=').append(HstURL.RESOURCE_TYPE);
            url.append('&').append(urlNamespacedReferenceParamName).append('=').append(URLEncoder.encode(containerURL.getResourceWindowReferenceNamespace(), characterEncoding));

            if (containerURL.getResourceId() != null) {
                url.append('&').append(urlNamespacedResourceIdParamName).append('=').append(URLEncoder.encode(containerURL.getResourceId(), characterEncoding));
            }

            firstParamDone = true;
        } else if (containerURL.getComponentRenderingWindowReferenceNamespace() != null) {
            url.append(firstParamDone ? "&" : "?").append(urlNamespacedTypeParamName).append('=').append(HstURL.COMPONENT_RENDERING_TYPE);
            url.append('&').append(urlNamespacedReferenceParamName).append('=').append(URLEncoder.encode(containerURL.getComponentRenderingWindowReferenceNamespace(), characterEncoding));
            firstParamDone = true;
        }

        Map<String, String []> parameters = containerURL.getParameterMap();

        if (parameters != null) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String name = entry.getKey();

                for (String value : entry.getValue()) {
                    url.append(firstParamDone ? "&" : "?")
                    .append(name)
                    .append('=')
                    .append(URLEncoder.encode(value, characterEncoding));

                    firstParamDone = true;
                }
            }
        }

        return url.toString();
    }

    protected void parseRequestInfo(HstContainerURL url) {
        try {
            String requestType = url.getParameter(urlNamespacedTypeParamName);

            if (HstURL.ACTION_TYPE.equals(requestType)) {
                String actionWindowReferenceNamespace = url.getParameter(urlNamespacedReferenceParamName);
                url.setActionWindowReferenceNamespace(actionWindowReferenceNamespace);

                log.debug("action window chosen for {}: {}", url.getPathInfo(), actionWindowReferenceNamespace);
            } else if (HstURL.RESOURCE_TYPE.equals(requestType)) {
                String resourceWindowReferenceNamespace = url.getParameter(urlNamespacedReferenceParamName);
                String resourceId = url.getParameter(urlNamespacedResourceIdParamName);
                url.setResourceId(resourceId);
                url.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);

                log.debug("resource window chosen for {}: {}", url.getPathInfo(), resourceWindowReferenceNamespace + ", " + resourceId);
            } else if (HstURL.COMPONENT_RENDERING_TYPE.equals(requestType)) {
                String componentRenderingReferenceNamespace = url.getParameter(urlNamespacedReferenceParamName);
                url.setComponentRenderingWindowReferenceNamespace(componentRenderingReferenceNamespace);

                log.debug("partial render window chosen for {}: {}", url.getPathInfo(), componentRenderingReferenceNamespace);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid container URL path: {}", url.getRequestPath(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Invalid container URL path: {}", url.getRequestPath());
            }
        } finally {
            // clean up all the HST-2 namespaced parameters
            url.setParameter(urlNamespacedTypeParamName, (String) null);
            url.setParameter(urlNamespacedReferenceParamName, (String) null);
            url.setParameter(urlNamespacedResourceIdParamName, (String) null);
        }
    }

    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        return toURLString(containerURL, requestContext, null);
    }

    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException {
        StringBuilder urlBuilder = new StringBuilder(100);

        if (contextPath != null) {
            urlBuilder.append(contextPath);
        } else if (requestContext.isCmsRequest() ||  requestContext.getResolvedMount().getMount().isContextPathInUrl()) {
            urlBuilder.append(containerURL.getContextPath());
        }

        String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
        String path = null;

        if (ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace)) {
            String oldPathInfo = containerURL.getPathInfo();
            String resourcePath = containerURL.getResourceId();
            Map<String, String[]> oldParamMap = containerURL.getParameterMap();

            try {
                containerURL.setResourceWindowReferenceNamespace(null);
                ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                containerURL.setParameters(null);
                path = buildHstURLPath(containerURL, requestContext);
            } finally {
                containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                containerURL.setParameters(oldParamMap);
            }
        } else {
            // if there is a matchingIgnoredPrefix on the ResolvedMount, we include it here again after the contextpath
            if(!StringUtils.isEmpty(requestContext.getResolvedMount().getMatchingIgnoredPrefix())) {
                urlBuilder.append('/').append(requestContext.getResolvedMount().getMatchingIgnoredPrefix());
            }

            urlBuilder.append(containerURL.getResolvedMountPath());
            path = buildHstURLPath(containerURL, requestContext);
        }

        urlBuilder.append(path);

        return urlBuilder.toString();
    }
}
