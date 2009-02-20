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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public interface HstURL {
    
    String TYPE_ACTION = "action";
    String TYPE_RENDER = "render";
    String TYPE_RESOURCE = "resource";
    
    void setType(String type);
    
    String getType();
    
    void setParameter(String name, String value);
    
    void setParameter(String name, String[] values);
    
    void setParameters(Map<String, String[]> parameters);
    
    String toString();
    
    Map<String, String[]> getParameterMap();
    
    void write(Writer out) throws IOException;
    
    void write(Writer out, boolean escapeXML) throws IOException;
    
    /**
     * Allows setting a resource ID that can be retrieved when serving the resource
     * through HstRequest.getResourceID() method in a HstComponent instance. 
     * 
     * @param resourceID
     */
    void setResourceID(String resourceID);
    
}
