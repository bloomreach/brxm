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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.util.Path;

/**
 * The default implementation providing HstContainerURL.
 * This implementation assume the urls are like the following examples:
 * <pre><code>
 * 1) render url   : http://localhost/site/content/news/2008/08
 * 2) action url   : http://localhost/site/content/_hstact_:<action ref>/<param count>/<action param name>/<action param value>/news/2008/08
 * 3) resource url : http://localhsot/site/content/_hstres_:<resource ref>:<resource ID>/news/2008/08
 * </code></pre> 
 * 
 * @version $Id$
 */
public class HstContainerURLProviderImpl implements HstContainerURLProvider {
    
    static Log log = LogFactory.getLog(HstContainerURLProviderImpl.class);
    
    private static final String ACTION_URL_PATH_PREFIX = "/_hstact_:";
    private static final String RESOURCE_URL_PATH_PREFIX = "/_hstres_:";
    
    protected String parameterNameComponentSeparator = ":";
    
    public HstContainerURLProviderImpl() {
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public String getParameterNameComponentSeparator() {
        return this.parameterNameComponentSeparator;
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
            if (pathInfo.startsWith(ACTION_URL_PATH_PREFIX)) {
                Path path = new Path(pathInfo);
                String [] actionInfo = path.getSegment(0).split(":");
                String actionWindowReferenceNamespace = (actionInfo.length > 1 ? actionInfo[1] : null);
                
                int paramCount = Integer.parseInt(path.getSegment(1));
                int pathPartIndex = 2;
                
                for (int i = 0; i < paramCount; i++) {
                    String paramName = path.getSegment(pathPartIndex);
                    String paramValue = urlDecode(path.getSegment(pathPartIndex + 1), characterEncoding);
                    url.setActionParameter(paramName, paramValue);
                    pathPartIndex += 2;
                }
                
                url.setPathInfo(path.getSubPath(pathPartIndex).toString());
                url.setActionWindowReferenceNamespace(actionWindowReferenceNamespace);
                
                if (log.isDebugEnabled()) {
                    log.debug("action window chosen for " + url.getPathInfo() + ": " + actionWindowReferenceNamespace);
                }
            } else if (pathInfo.startsWith(RESOURCE_URL_PATH_PREFIX)) {
                Path path = new Path(pathInfo);
                String [] resourceInfo = path.getSegment(0).split(":");
                String resourceWindowReferenceNamespace = (resourceInfo.length > 1 ? resourceInfo[1] : null);
                String resourceId = (resourceInfo.length > 2 ? urlDecode(resourceInfo[2], characterEncoding) : null);
                url.setResourceId(resourceId);
                url.setPathInfo(path.getSubPath(1).toString());
                url.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                
                if (log.isDebugEnabled()) {
                    log.debug("resource window chosen for " + url.getPathInfo() + ": " + resourceWindowReferenceNamespace + ", " + resourceId);
                }
            }
        } catch (Exception e) {
            log.warn("Invalid container URL path: " + pathInfo, e);
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
            mergeParameters(containerURL, hstUrl);
        }
        
        return containerURL;
    }
    
    public String toURLString(HstContainerURL containerURL) {
        String characterEncoding = containerURL.getCharacterEncoding();
        StringBuilder url = new StringBuilder(containerURL.getContextPath());
        url.append(containerURL.getServletPath());
        
        if (containerURL.getActionWindowReferenceNamespace() != null) {
            url.append(ACTION_URL_PATH_PREFIX).append(containerURL.getActionWindowReferenceNamespace());
            
            Map<String, String []> actionParams = containerURL.getActionParameterMap();
            url.append("/").append(actionParams == null ? 0 : actionParams.size());
            
            if (actionParams != null) {
                for (Map.Entry<String, String []> entry : actionParams.entrySet()) {
                    for (String value : entry.getValue()) {
                        url.append("/").append(entry.getKey());
                        url.append("/").append(urlEncode(urlEncode(value, characterEncoding), characterEncoding));
                    }
                }
            }
        } else if (containerURL.getResourceWindowReferenceNamespace() != null) {
            url.append(RESOURCE_URL_PATH_PREFIX)
            .append(containerURL.getResourceWindowReferenceNamespace())
            .append(":")
            .append(containerURL.getResourceId() == null ? "" : urlEncode(urlEncode(containerURL.getResourceId(), characterEncoding), characterEncoding));
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
    
    protected void mergeParameters(HstContainerURL containerURL, HstURL hstUrl) {
        String referenceNamespace = hstUrl.getReferenceNamespace();
        Map<String, String []> parameterMap = hstUrl.getParameterMap();
        
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
