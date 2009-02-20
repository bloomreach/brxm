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
    
    String RENDER_TYPE = "render";
    String ACTION_TYPE = "action";
    String RESOURCE_TYPE = "resource";
    
    HstRequestContext getRequestContext();
    
    HstComponentWindow getComponentWindow();
    
    String getType();
    
    Map<String, Object> getParameterMap();
    
    Map<String, Object> getParameterMap(String namespace);
    
    Map<String, Object> getAttributeMap();
    
    Map<String, Object> getAttributeMap(String namespace);
    
    String getResourceID();
    
}
