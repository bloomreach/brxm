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
    
    public HstRequest getHstRequest(HttpServletRequest servletRequest) {
        HstRequest hstRequest = (HstRequest) servletRequest.getAttribute(ContainerConstants.HST_REQUEST);
        
        if (hstRequest == null && servletRequest instanceof HstRequest) {
            hstRequest = (HstRequest) servletRequest;
        }
        
        return hstRequest;
    }
    
    public HstResponse getHstResponse(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        HstResponse hstResponse = (HstResponse) servletRequest.getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstResponse == null && servletResponse instanceof HstResponse) {
            hstResponse = (HstResponse) servletResponse;
        }
        
        return hstResponse;
    }
    
    public static String getRequestServerName(HttpServletRequest request) {
        // TODO: Possibly we can read some header information in a complicated proxied environment like the following example code:
        //String xForwardedHost = request.getHeader("X-Forwarded-Host");
        //if (xForwardedHost != null) {
        //    String [] xForwardedHostNames = xForwardedHost.split(",");
        //    if (xForwardedHostNames.length > 0) {
        //        return xForwardedHostNames[0].trim();
        //    }
        //}
        return request.getServerName();
    }
    
    public static int getRequestServerPort(HttpServletRequest request) {
        // TODO: Possibly we can read some header information in a complicated proxied environment like the following example code:
        //String xForwardedPort = request.getHeader("X-Forwarded-Port");
        //if (xForwardedPort != null) {
        //    String [] xForwardedPortNumbers = xForwardedPort.split(",");
        //    if (xForwardedPortNumbers.length > 0) {
        //        try {
        //            return Integer.parseInt(xForwardedPortNumbers[0].trim());
        //        } catch (NumberFormatException ignore) {
        //        }
        //    }
        //}
        return request.getServerPort();
    }
    
}
