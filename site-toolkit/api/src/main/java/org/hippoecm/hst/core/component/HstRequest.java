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
package org.hippoecm.hst.core.component;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstRequest extends HttpServletRequest {
    
    /**
     * Returns the current request context
     * @return
     */
    HstRequestContext getRequestContext();
    
    /**
     * Returns the parameter map of this component window.
     * If the request is in the action lifecycle, then only action parameters can be accessible.
     * Otherwise, then only render parameters can be accessible.
     */
    Map<String, Object> getParameterMap();
    
    /**
     * Returns the parameter map of the specified reference path component window.
     * If the request type is in the action lifecycle, the referencePath parameter will be just ignored
     * and the operation will be equivalent to {@link #getParameterMap()}.
     * @param referencePath
     * @return
     */
    Map<String, Object> getParameterMap(String referencePath);
    
    /**
     * Returns the attribute map of this component window.
     * @return
     */
    Map<String, Object> getAttributeMap();
    
    /**
     * Returns the attribute map of the specified reference path component window.
     * @return
     */
    Map<String, Object> getAttributeMap(String referencePath);
    
    /**
     * Returns the resource ID which was set by the resource HST URL.
     * @return
     */
    String getResourceID();
    
}
