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
package org.hippoecm.hst.container;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * <p>
 * The {@link HstContainerRequestImpl} is a wrapper around the {@link GenericHttpServletRequestWrapper}. As the {@link HstRequestProcessor}
 * is invoked from a {@link Filter}, the original {@link HttpServletRequest} does return an empty {@link HttpServletRequest#getPathInfo()}. 
 * However, in the context of {@link HstRequestProcessor}, the {@link HttpServletRequest#getServletPath()} is equivalent to {@link ResolvedMount#getResolvedMountPath()}
 * and the {@link HttpServletRequest#getPathInfo()} equivalent to the {@link HttpServletRequest#getRequestURI()} after the {@link HttpServletRequest#getServletPath()}
 * (and the same for {@link HttpServletRequest#getPathTranslated()} only then not decoded.). 
 * </p>
 * <p>
 * Therefore, this {@link HstContainerRequestImpl} object has a setter {@link #setServletPath(String)}, that recomputes the {@link #getPathInfo()}. When the constructor
 * is called, the <code>servletPath</code> is set to an empty {@link String} (""). After for example a {@link ResolvedMount} is found, the {@link #setServletPath(String)} can be 
 * called to recompute/reset the <code>servletPath</code> and <code>pathInfo</code> (and <code>pathTranslated</code>)
 * </p>
 * <p>
 * The {@link #getPathInfo()} won't return the part in the {@link #getRequestURI()} after the <code>pathSuffixDelimiter</code>. 
 * The delimiter itself won't be part of the {@link #getPathInfo()} either.
 * Also the return won't include any matrix parameters. 
 * </p>
 * @version $Id$
 */
public class HstContainerRequestImpl extends GenericHttpServletRequestWrapper implements HstContainerRequest {
    
    private String pathSuffix;
    private String pathSuffixDelimiter;

    /**
     * Creates a wrapper {@link HttpServletRequest} with a {@link HttpServletRequest#getServletPath()} that is an empty {@link String} (""). The 
     * {@link HttpServletRequest#getPathInfo()} will be the part of the {@link HttpServletRequest#getRequestURI()} after the {@link HttpServletRequest#getContextPath()}
     * and before the {@link #getPathSuffix()} and before the {@link #MATRIX_PARAMETERS_DELIMITER}
     * @param request
     * @param pathSuffixDelimiter
     */
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
        
        // we call setServletPath for bootstrapping pathInfo && pathTranslated
        setServletPath("");
        
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
    
    /**
     * <p>
     * Sets a new <code>servletPath</code> on the {@link HttpServletRequest}. For an {@link HstRequest}, the <code>servletPath</code> becomes the {@link ResolvedMount#getResolvedMountPath()} of
     * the {@link ResolvedMount} belonging to the {@link HstRequestContext}. 
     * </p>
     * <p>When the {@link ResolvedMount#getMount()} returns a 'root' {@link Mount}, the <code>servletPath</code> is an empty string (""). Otherwise, it always starts with a  slash "/".
     * </p>
     * @see ResolvedMount#getResolvedMountPath()
     * @param servletPath
     */
    @Override
    public void setServletPath(String servletPath) {
        // recompute the pathInfo with the original requestURI
        super.setServletPath(servletPath);
        pathTranslated = getRequestURI().substring(getContextPath().length());
        if(getServletPath() != null) {
            pathTranslated = pathTranslated.substring(getServletPath().length());
        }
        
        // we do not need to strip off the pathSuffix as this is already done in the constructor. 
        pathTranslated = HstRequestUtils.removeAllMatrixParams(pathTranslated);
        // pathTranslated is the not decoded version of pathInfo
        setDecodedPathInfo(pathTranslated);
        
    }
    
    private void setDecodedPathInfo(String pathTranslated) {
        String characterEncoding = getCharacterEncoding();
        if (characterEncoding == null) {
            characterEncoding = "ISO-8859-1";
        }
        try {
            pathInfo = URLDecoder.decode(pathTranslated, characterEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Invalid character encoding: " + characterEncoding, e);
        }
    }

    @Override
    public String getPathInfo() {
        return pathInfo;  
    }
    
    @Override
    public String getPathTranslated() {
       return pathTranslated;
    }
}