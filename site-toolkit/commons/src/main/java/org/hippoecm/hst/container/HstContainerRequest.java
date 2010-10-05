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
package org.hippoecm.hst.container;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;

/**
 * HstContainerRequest
 * @version $Id$
 */
public class HstContainerRequest extends GenericHttpServletRequestWrapper {
    
    private String pathSuffix;
    
    public HstContainerRequest(HttpServletRequest request, String pathSuffixDelimiter) {
        super(request);
        
        if (pathSuffixDelimiter == null || "".equals(pathSuffixDelimiter)) {
            return;
        }
        
        String requestURI = request.getRequestURI();
        int pathSuffixOffset = requestURI.indexOf(pathSuffixDelimiter);
        
        if (pathSuffixOffset == -1) {
            return;
        }
        
        String matrixParams = null;
        int matrixParamsOffset = requestURI.indexOf(';');
        
        if (matrixParamsOffset != -1) {
            matrixParams = requestURI.substring(matrixParamsOffset);
        }
        
        if (matrixParams != null) {
            setRequestURI(requestURI.substring(0, pathSuffixOffset) + matrixParams);
        } else {
            setRequestURI(requestURI.substring(0, pathSuffixOffset));
        }
        
        pathSuffix = requestURI.substring(pathSuffixOffset + pathSuffixDelimiter.length());
        
        StringBuffer requestURL = request.getRequestURL();
        if (requestURL != null) {
            matrixParamsOffset = requestURL.indexOf(";");
            if (matrixParamsOffset != -1) {
                requestURL.delete(requestURL.indexOf(pathSuffixDelimiter), matrixParamsOffset);
            } else {
                requestURL.delete(requestURL.indexOf(pathSuffixDelimiter), requestURL.length());
            }
            setRequestURL(requestURL);
        }
        
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            setPathInfo(substringBefore(pathInfo, pathSuffixDelimiter));
        }
        
        String pathTranslated = request.getPathTranslated();
        if (pathTranslated != null) {
            setPathTranslated(substringBefore(pathTranslated, pathSuffixDelimiter));
        }
    }
    
    public String getPathSuffix() {
        return pathSuffix;
    }
    
    private static String substringBefore(String source, String delimiter) {
        int offset = source.indexOf(delimiter);
        
        if (offset == -1) {
            return source;
        }
        
        return source.substring(0, offset);
    }
    
}