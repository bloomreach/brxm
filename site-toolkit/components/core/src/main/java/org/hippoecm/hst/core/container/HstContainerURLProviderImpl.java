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
import org.hippoecm.hst.core.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation providing HstContainerURL.
 * This implementation assume the urls are like the following examples:
 * <code><xmp>
 * 1) render url will not be encoded : http://localhost/site/content/news/2008/08
 * 2) action url will be encoded     : http://localhost/site/content/_hn:<encoded_params1>/<encoded_param_name>/<encoded_param_value>/news/2008/08
 *          the <encoded_params1>     : <request_type>|<action reference namespace>|<param count>
 * 3) resource url will be encoded   : http://localhsot/site/content/_hn:<encoded_params1>/news/2008/08
 *          the <encoded_params1>     : <request_type>|<resource reference namespace>|<resource ID>
 * </xmp></code> 
 * 
 * @version $Id$
 */
public class HstContainerURLProviderImpl implements HstContainerURLProvider {
    
    private final static Logger log = LoggerFactory.getLogger(HstContainerURLProvider.class);
    
    private static final String REQUEST_INFO_SEPARATOR = "|";

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
                String encodedInfo = urlInfo.substring(urlInfo.indexOf(':'));
                String decodedInfo = this.navigationalStateCodec.decodeParameters(encodedInfo, characterEncoding);
                String [] infos = StringUtils.split(decodedInfo, '|');
                
                String requestType = infos[0];
                
                if (HstRequest.ACTION_TYPE.equals(requestType)) {
                    String actionWindowReferenceNamespace = infos[1];
                    int paramCount = Integer.parseInt(infos[2]);
                    
                    int pathPartIndex = 1;
                    
                    for (int i = 0; i < paramCount; i++) {
                        String paramName = this.navigationalStateCodec.decodeParameters(path.getSegment(pathPartIndex), characterEncoding);
                        String paramValue = this.navigationalStateCodec.decodeParameters(path.getSegment(pathPartIndex + 1), characterEncoding);
                        url.setActionParameter(paramName, paramValue);
                        pathPartIndex += 2;
                    }
                    
                    url.setPathInfo(path.getSubPath(pathPartIndex).toString());
                    url.setActionWindowReferenceNamespace(actionWindowReferenceNamespace);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("action window chosen for {}: {}", url.getPathInfo(), actionWindowReferenceNamespace);
                    }
                } else if (HstRequest.RESOURCE_TYPE.equals(requestType)) {
                    String resourceWindowReferenceNamespace = infos[1];
                    String resourceId = infos[2];
                    
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
        
        Map<String, String []> paramMap = (Map<String, String []>) request.getParameterMap();
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
            String requestInfo = 
                HstRequest.ACTION_TYPE + REQUEST_INFO_SEPARATOR + 
                containerURL.getActionWindowReferenceNamespace() + REQUEST_INFO_SEPARATOR + 
                (actionParams == null ? 0 : actionParams.size());
            url.append(this.navigationalStateCodec.encodeParameters(requestInfo, characterEncoding));
            
            if (actionParams != null) {
                for (Map.Entry<String, String []> entry : actionParams.entrySet()) {
                    for (String value : entry.getValue()) {
                        url.append("/").append(this.navigationalStateCodec.encodeParameters(entry.getKey(), characterEncoding));
                        url.append("/").append(this.navigationalStateCodec.encodeParameters(value, characterEncoding));
                    }
                }
            }
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
