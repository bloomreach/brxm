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
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.util.HttpUtils;
import org.hippoecm.hst.core.util.Path;
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
public class HstContainerURLProviderImpl implements HstContainerURLProvider {
    
    private final static Logger log = LoggerFactory.getLogger(HstContainerURLProvider.class);
    
    private static final String REQUEST_INFO_SEPARATOR = "|";
    private static final char REQUEST_INFO_SEPARATOR_CHAR = '|';

    private static final String DEFAULT_HST_URL_NAMESPACE_PREFIX = "_hn:";

    protected String urlNamespacePrefix = DEFAULT_HST_URL_NAMESPACE_PREFIX;
    private String urlNamespacePrefixedPath = "/" + urlNamespacePrefix;
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

    public HstContainerURL parseURL(ServletRequest servletRequest, ServletResponse servletResponse) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        HstContainerURLImpl url = new HstContainerURLImpl();
        
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        url.setCharacterEncoding(characterEncoding);
        url.setContextPath(request.getContextPath());
        url.setServletPath(request.getServletPath());
        
        String pathInfo = request.getPathInfo();
        url.setPathInfo(pathInfo);
        
        try {
            if (pathInfo.startsWith(this.urlNamespacePrefixedPath)) {
                Path path = new Path(pathInfo);
                String urlInfo = path.getSegment(0);
                String encodedInfo = urlInfo.substring(this.urlNamespacePrefix.length());
                String decodedInfo = this.navigationalStateCodec.decodeParameters(encodedInfo, characterEncoding);
                String [] requestInfos = StringUtils.splitPreserveAllTokens(decodedInfo, REQUEST_INFO_SEPARATOR_CHAR);
                
                String requestType = requestInfos[0];
                
                if (HstRequest.ACTION_TYPE.equals(requestType)) {
                    String actionWindowReferenceNamespace = requestInfos[1];
                    String queryParams = "";
                    
                    if (requestInfos.length > 3) {
                        queryParams = StringUtils.join(requestInfos, REQUEST_INFO_SEPARATOR_CHAR, 2, requestInfos.length);
                    } else {
                        queryParams = requestInfos[2];
                    }
                    
                    if (queryParams != null && !"".equals(queryParams)) {
                        String [] paramPairs = StringUtils.split(queryParams, '&');
                        
                        for (String paramPair : paramPairs) {
                            String [] paramNameAndValue = StringUtils.split(paramPair, '=');
                            String paramValue = URLDecoder.decode(paramNameAndValue[1], characterEncoding);
                            url.setActionParameter(paramNameAndValue[0], paramValue);
                        }
                    }
                    
                    url.setPathInfo(path.getSubPath(1).toString());
                    url.setActionWindowReferenceNamespace(actionWindowReferenceNamespace);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("action window chosen for {}: {}", url.getPathInfo(), actionWindowReferenceNamespace);
                    }
                } else if (HstRequest.RESOURCE_TYPE.equals(requestType)) {
                    String resourceWindowReferenceNamespace = requestInfos[1];
                    String resourceId = requestInfos[2];
                    
                    url.setResourceId(resourceId);
                    url.setPathInfo(path.getSubPath(1).toString());
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
        
        if (HstURL.TYPE_ACTION.equals(type)) {
            containerURL.setActionWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            containerURL.setActionParameters(hstUrl.getParameterMap());
        } else if (HstURL.TYPE_RESOURCE.equals(type)) {
            containerURL.setResourceWindowReferenceNamespace(hstUrl.getReferenceNamespace());
            containerURL.setResourceId(hstUrl.getResourceID());
        } else {
            mergeParameters(containerURL, hstUrl.getReferenceNamespace(), hstUrl.getParameterMap());
        }
        
        return containerURL;
    }
    
    public String toURLString(HstContainerURL containerURL) throws UnsupportedEncodingException {
        String characterEncoding = containerURL.getCharacterEncoding();
        StringBuilder url = new StringBuilder(containerURL.getContextPath());
        url.append(containerURL.getServletPath());
        
        if (containerURL.getActionWindowReferenceNamespace() != null) {
            url.append(this.urlNamespacePrefixedPath);
            
            Map<String, String []> actionParams = containerURL.getActionParameterMap();
            StringBuilder sbRequestInfo = new StringBuilder(80);
            sbRequestInfo.append(HstRequest.ACTION_TYPE).append(REQUEST_INFO_SEPARATOR);
            sbRequestInfo.append(containerURL.getActionWindowReferenceNamespace()).append(REQUEST_INFO_SEPARATOR);
            
            if (actionParams != null) {
                boolean firstDone = false;
                
                for (Map.Entry<String, String []> entry : actionParams.entrySet()) {
                    String paramName = entry.getKey();
                    
                    for (String value : entry.getValue()) {
                        if (firstDone) {
                            sbRequestInfo.append('&');
                        }
                        
                        sbRequestInfo.append(paramName).append('=');
                        
                        if (value != null) {
                            sbRequestInfo.append(URLEncoder.encode(value, characterEncoding));
                        }
                        
                        firstDone = true;
                    }
                }
            }
            
            url.append(this.navigationalStateCodec.encodeParameters(sbRequestInfo.toString(), characterEncoding));
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            url.append(this.urlNamespacePrefixedPath);
            
            String requestInfo = 
                HstRequest.RESOURCE_TYPE + REQUEST_INFO_SEPARATOR + 
                containerURL.getResourceWindowReferenceNamespace() + REQUEST_INFO_SEPARATOR + 
                (containerURL.getResourceId() != null ? containerURL.getResourceId() : "");
            
            url.append(this.navigationalStateCodec.encodeParameters(requestInfo, characterEncoding));
        }
        
        url.append(containerURL.getPathInfo());
        
        boolean firstParamDone = (url.indexOf("?") >= 0);
        
        Map<String, String []> parameters = containerURL.getParameterMap();
        
        if (parameters != null) {
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String name = entry.getKey();
                
                for (String value : entry.getValue()) {
                    url.append(firstParamDone ? "&" : "?")
                    .append(name)
                    .append("=")
                    .append(urlEncode(value, characterEncoding));
                    
                    firstParamDone = true;
                }
            }
        }
        
        return url.toString();
    }
    
    public void mergeParameters(HstContainerURL containerURL, String referenceNamespace, Map<String, String []> parameterMap) {
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = referenceNamespace + this.parameterNameComponentSeparator + entry.getKey();
            String [] values = entry.getValue();
            
            if (values == null || values.length == 0) {
                containerURL.setParameter(name, (String) null);
            } else {
                containerURL.setParameter(name, values);
            }
        }
    }
    
    protected String urlEncode(String value, String characterEncoding) {
        String encodedValue = value;
        
        try {
            encodedValue = URLEncoder.encode(value, characterEncoding);
        } catch (Exception e) {
        }
        
        return encodedValue;
    }
    
    protected String urlDecode(String value, String characterEncoding) {
        String decodedValue = value;
        
        try {
            decodedValue = URLDecoder.decode(value, characterEncoding);
        } catch (Exception e) {
        }
        
        return decodedValue;
    }

}
