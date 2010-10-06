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
 * HstContainerRequestImpl
 * @version $Id$
 */
public class HstContainerRequestImpl extends GenericHttpServletRequestWrapper implements HstContainerRequest {
    
    private static final String MATRIX_PARAMETERS_DELIMITER = ";";
    
    private String pathSuffix;
    private String pathSuffixDelimiter;
    
    public HstContainerRequestImpl(HttpServletRequest request, String pathSuffixDelimiter) {
        super(request);
        
        this.pathSuffixDelimiter = pathSuffixDelimiter;
        
        if (pathSuffixDelimiter == null || "".equals(pathSuffixDelimiter)) {
            return;
        }
        
        String tempRequestURI = request.getRequestURI();
        int pathSuffixOffset = tempRequestURI.indexOf(pathSuffixDelimiter);
        
        if (pathSuffixOffset != -1) {
            requestURI = tempRequestURI.substring(0, pathSuffixOffset);
            pathSuffix = tempRequestURI.substring(pathSuffixOffset + pathSuffixDelimiter.length());
        }
    }
    
    public String getPathSuffix() {
        return pathSuffix;
    }
    
    @Override
    public StringBuffer getRequestURL() {
        if (pathSuffix == null) {
            return super.getRequestURL();
        }
        
        StringBuffer tempRequestURL = requestURL;
        
        if (tempRequestURL == null) {
            tempRequestURL = super.getRequestURL();
        
            if (tempRequestURL != null) {
                tempRequestURL.delete(tempRequestURL.indexOf(pathSuffixDelimiter), tempRequestURL.length());
            }
            
            requestURL = tempRequestURL;
        }
        
        return tempRequestURL;
    }
    
    @Override
    public String getPathInfo() {
        if (pathSuffix == null) {
            return super.getPathInfo();
        }
        
        String tempPathInfo = pathInfo;
        
        if (tempPathInfo == null) {
            tempPathInfo = super.getPathInfo();
            
            if (tempPathInfo != null) {
                tempPathInfo = substringBefore(substringBefore(tempPathInfo, pathSuffixDelimiter), MATRIX_PARAMETERS_DELIMITER);
                pathInfo = tempPathInfo;
            }
        }
        
        return tempPathInfo;
    }
    
    @Override
    public String getPathTranslated() {
        if (pathSuffix == null) {
            return super.getPathTranslated();
        }
        
        String tempPathTranslated = pathTranslated;
        
        if (tempPathTranslated == null) {
            tempPathTranslated = super.getPathTranslated();
            
            if (tempPathTranslated != null) {
                tempPathTranslated = substringBefore(substringBefore(tempPathTranslated, pathSuffixDelimiter), MATRIX_PARAMETERS_DELIMITER);
                pathTranslated = tempPathTranslated;
            }
        }
        
        return tempPathTranslated;
    }
    
    private static String substringBefore(String source, String delimiter) {
        int offset = source.indexOf(delimiter);
        
        if (offset == -1) {
            return source;
        }
        
        return source.substring(0, offset);
    }
    
}