/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.util.PathEncoder;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.util.QueryStringBuilder;
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

    public HstContainerURL parseURL(HttpServletRequest request, ResolvedMount mount, String requestPath, Map<String, String []> queryParams) {
        return parseURL(mount, request.getContextPath(), requestPath, queryParams,
                HstRequestUtils.getCharacterEncoding(request), HstRequestUtils.getURIEncoding(request));
    }

    public HstContainerURL parseURL(ResolvedMount mount, String contextPath, String requestPath,
                                    Map<String, String []> queryParams, String requestCharacterEncoding,
                                    String requestURIEncoding) {
        HstContainerURLImpl url = new HstContainerURLImpl();
        url.setContextPath(contextPath);
        url.setHostName(mount.getMount().getVirtualHost().getHostName());
        url.setPortNumber(mount.getPortNumber());
        url.setResolvedMountPath(mount.getResolvedMountPath());
        url.setRequestPath(requestPath);
        url.setPathInfo(requestPath.substring(mount.getResolvedMountPath().length()));
        url.setCharacterEncoding(requestCharacterEncoding);
        url.setURIEncoding(requestURIEncoding);
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
        url.setURIEncoding(baseURL.getURIEncoding());
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
        url.setCharacterEncoding(HstRequestUtils.getCharacterEncoding(request));
        String uriEncoding = HstRequestUtils.getURIEncoding(request);
        url.setURIEncoding(uriEncoding);

        try {
            Map<String, String[]> paramMap = HstRequestUtils.parseQueryString(request);
            url.setParameters(paramMap);
        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.warn("Unsupported encoding in request, using empty query parameters:", e);
            } else {
                log.warn("Unsupported encoding in request, using empty query parameters: " + e.toString());
            }
        }

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
        url.setURIEncoding(baseContainerURL.getURIEncoding());
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
        url.setURIEncoding(baseContainerURL.getURIEncoding());

        // if the Mount is port agnostic, in other words, has port = 0, we take the port from the baseContainerURL
        if (mount.getPort() == 0) {
            url.setPortNumber(baseContainerURL.getPortNumber());
        } else {
            url.setPortNumber(mount.getPort());
        }

        boolean includeTrailingSlash = false;

        HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class);
        if (pathInfo != null && pathInfo.endsWith(hstManager.getPathSuffixDelimiter())) {
            // we now must not strip the trailing slash because it is part of the rest call ./
            includeTrailingSlash = true; 
        }

        // if pathInfo starts with or is equal to the rest call subPath prefix (default ./), then, we must not include a 
        // leading / to the pathInfo, because then for the homepage, which has a empty pathInfo before the subPath, we would
        // get a wrong URL like /mountPath/./subPath : It must be /mountPath./subPath instead. Thus, hence this check
        boolean includeLeadingSlash = true;

        if (pathInfo != null && pathInfo.startsWith(hstManager.getPathSuffixDelimiter())) {
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

    protected String buildHstURLPath(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException {
        String encoding = containerURL.getURIEncoding();
        StringBuilder url = new StringBuilder(100);

        String containerUrlPathInfo = containerURL.getPathInfo();

        if ("/".equals(containerUrlPathInfo) || StringUtils.isEmpty(containerUrlPathInfo)) {
            if (StringUtils.isEmpty(containerURL.getResolvedMountPath())) {
                // make sure the created url has at least a '/' after the contextpath because
                // containers typically send a 302 for urls the match only the contextpath (for example /site)
                url.append('/');
            }
        } else {
            final String encoded = PathEncoder.encode(containerUrlPathInfo, encoding);
            if (!encoded.startsWith("/")) {
                url.append("/");
            }
            url.append(encoded);
        }

        final QueryStringBuilder queryStringBuilder = new QueryStringBuilder(encoding);

        if (containerURL.getActionWindowReferenceNamespace() != null) {
            queryStringBuilder.append(urlNamespacedTypeParamName, HstURL.ACTION_TYPE);
            queryStringBuilder.append(urlNamespacedReferenceParamName, containerURL.getActionWindowReferenceNamespace());
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            queryStringBuilder.append(urlNamespacedTypeParamName, HstURL.RESOURCE_TYPE);
            queryStringBuilder.append(urlNamespacedReferenceParamName, containerURL.getResourceWindowReferenceNamespace());

            if (containerURL.getResourceId() != null) {
                queryStringBuilder.append(urlNamespacedResourceIdParamName, containerURL.getResourceId());
            }
        } else if (containerURL.getComponentRenderingWindowReferenceNamespace() != null) {
            queryStringBuilder.append(urlNamespacedTypeParamName, HstURL.COMPONENT_RENDERING_TYPE);
            queryStringBuilder.append(urlNamespacedReferenceParamName, containerURL.getComponentRenderingWindowReferenceNamespace());
        }

        Map<String, String []> parameters = containerURL.getParameterMap();

        if (parameters != null) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String name = entry.getKey();

                for (String value : entry.getValue()) {
                    queryStringBuilder.append(name, value);
                }
            }
        }

        url.append(queryStringBuilder.toString());

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
                if (componentRenderingReferenceNamespace == null) {
                    // typically this situation easily leads to recursion if we do not throw an exception resulting in 404:
                    // if we continue, the normal page would be rendered, typically including again the wrong
                    // component-rendering url again. Therefore we short-circuit and return a 404
                    throw new MatchException("Component rendering namespace now allowed to be missing " +
                            "for component-rendering URLs");
                }
                url.setComponentRenderingWindowReferenceNamespace(componentRenderingReferenceNamespace);

                log.debug("partial render window chosen for {}: {}", url.getPathInfo(), componentRenderingReferenceNamespace);
            }
        } catch (MatchException e) {
            throw e;
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
        } else if (requestContext.isChannelManagerPreviewRequest() ||  requestContext.getResolvedMount().getMount().isContextPathInUrl()) {
            urlBuilder.append(containerURL.getContextPath());
        }
        // if there is a matchingIgnoredPrefix on the ResolvedMount, we include it here again after the contextpath
        if(!StringUtils.isEmpty(requestContext.getResolvedMount().getMatchingIgnoredPrefix())) {
            urlBuilder.append('/').append(requestContext.getResolvedMount().getMatchingIgnoredPrefix());
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
            urlBuilder.append(containerURL.getResolvedMountPath());
            path = buildHstURLPath(containerURL, requestContext);
        }

        urlBuilder.append(path);

        return urlBuilder.toString();
    }
}
