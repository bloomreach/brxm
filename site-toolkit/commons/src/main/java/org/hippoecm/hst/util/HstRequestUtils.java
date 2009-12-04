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
package org.hippoecm.hst.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;

/**
 * HST Request Utils 
 * 
 * @version $Id$
 */
public class HstRequestUtils {
    
    private HstRequestUtils() {
        
    }
    
    /**
     * Returns <CODE>HstRequest</CODE> object found in the servletRequest.
     * @param servletRequest
     * @return
     */
    public static HstRequest getHstRequest(HttpServletRequest servletRequest) {
        HstRequest hstRequest = (HstRequest) servletRequest.getAttribute(ContainerConstants.HST_REQUEST);
        
        if (hstRequest == null && servletRequest instanceof HstRequest) {
            hstRequest = (HstRequest) servletRequest;
        }
        
        return hstRequest;
    }
    
    /**
     * Returns <CODE>HstResponse</CODE> object found in the servletRequest or servletResponse.
     * @param servletRequest
     * @param servletResponse
     * @return
     */
    public static HstResponse getHstResponse(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        HstResponse hstResponse = (HstResponse) servletRequest.getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstResponse == null && servletResponse instanceof HstResponse) {
            hstResponse = (HstResponse) servletResponse;
        }
        
        return hstResponse;
    }
    
    /**
     * Returns any extra path information associated with the URL the client sent when it made this request.
     * The extra path information follows the servlet path but precedes the query string 
     * and will start with a "/" character.
     * This method extracts and decodes the path information from the request URI returned from
     * <CODE>HttpServletRequest#getRequestURI()</CODE>.
     * @param request
     * @return
     */
    public static String getPathInfo(HttpServletRequest request) {
        return getPathInfo(request, null);
    }
    
    /**
     * Returns any extra path information associated with the URL the client sent when it made this request.
     * The extra path information follows the servlet path but precedes the query string 
     * and will start with a "/" character.
     * This method extracts and decodes the path information by the specified character encoding parameter
     * from the request URI.
     * @param request
     * @param characterEncoding
     * @return
     */
    public static String getPathInfo(HttpServletRequest request, String characterEncoding) {
        String encodePathInfo = request.getRequestURI().substring(request.getContextPath().length() + request.getServletPath().length());
        
        if (characterEncoding == null) {
            characterEncoding = request.getCharacterEncoding();
            
            if (characterEncoding == null) {
                characterEncoding = "ISO-8859-1";
            }
        }
        
        try {
            return URLDecoder.decode(encodePathInfo, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Invalid character encoding: " + characterEncoding, e);
        }
        
    }
    
    /**
     * Returns the original host informations requested by the client or the proxies 
     * in the Host HTTP request headers.
     * @param request
     * @return
     */
    public static String [] getRequestHosts(HttpServletRequest request) {
        String xfh = request.getHeader("X-Forwarded-Host");
        
        if (xfh != null) {
            String [] hosts = xfh.split(",");
            
            for (int i = 0; i < hosts.length; i++) {
                hosts[i] = hosts[i].trim();
            }
            
            return hosts;
        } else {
            return new String [] { request.getHeader("Host") };
        }
    }
    
    /**
     * Returns the original host information requested by the client.
     * @param request
     * @return
     */
    public static String getFarthestRequestHost(HttpServletRequest request) {
        return getRequestHosts(request)[0];
    }
    
    /**
     * Returns the original host's server name requested by the client.
     * @param request
     * @return
     */
    public static String getRequestServerName(HttpServletRequest request) {
        String requestHost = getFarthestRequestHost(request);
        
        if (requestHost == null) {
            return request.getServerName();
        }
        
        int offset = requestHost.indexOf(':');
        
        if (offset != -1) {
            return requestHost.substring(0, offset);
        } else {
            return requestHost;
        }
    }
    
    /**
     * Returns the original host' port number requested by the client.
     * @param request
     * @return
     */
    public static int getRequestServerPort(HttpServletRequest request) {
        String requestHost = getFarthestRequestHost(request);
        
        if (requestHost == null) {
            return request.getServerPort();
        }
        
        int offset = requestHost.indexOf(':');
        
        if (offset != -1) {
            return Integer.parseInt(requestHost.substring(offset + 1));
        } else {
            return ("https".equals(request.getScheme()) ? 443 : 80);
        }
    }
    
    /**
     * Returns the remote host addresses related to this request.
     * If there's any proxy server between the client and the server,
     * then the proxy addresses are contained in the returned array.
     * The lowest indexed element is the farthest downstream client and
     * each successive proxy addresses are the next elements. 
     * @param request
     * @return
     */
    public static String [] getRemoteAddrs(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        
        if (xff != null) {
            String [] addrs = xff.split(",");
            
            for (int i = 0; i < addrs.length; i++) {
                addrs[i] = addrs[i].trim();
            }
            
            return addrs;
        } else {
            return new String [] { request.getRemoteAddr() };
        }
    }
    
    /**
     * Returns the remote client address.
     * @param request
     * @return
     */
    public static String getFarthestRemoteAddr(HttpServletRequest request) {
        return getRemoteAddrs(request)[0];
    }
    
}
