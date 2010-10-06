package org.hippoecm.hst.container;

import javax.servlet.http.HttpServletRequest;

/**
 * HstContainerRequest
 * <P>
 * Marker interface for the container request wrapper
 * </P>
 * @version $Id$
 */
public interface HstContainerRequest extends HttpServletRequest {
    
    /**
     * Returns path suffix splitted from the request URI by the specified path suffix delimiter. 
     * @return
     */
    String getPathSuffix();
    
}
