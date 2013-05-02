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
package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * The <CODE>HstURL</CODE> interface represents a URL
 * that reference the HstComponent itself.
 * <p>
 * An HstURL is created through the <CODE>HstResponse</CODE>.
 * Parameters can be added to <CODE>HstURL</CODE> objects. 
 * <P>
 * There are four types of HstURLs:
 * <ul>
 * <li>Action URLs, they are created with <CODE>{@link HstResponse#createActionURL()}</CODE>, and 
 *     trigger an action request followed by a render request.
 * <li>Render URLs, they are created with <CODE>{@link HstResponse#createRenderURL()}</CODE>, and
 *     trigger a render request.
 * <li>Resource URLs, they are created with <CODE>{@link HstResponse#createResourceURL()}</CODE>, and
 *     trigger a resource rendering request.
 * <li>Navigational Render URLs, they are created with <CODE>{@link HstResponse#createNavigationalURL(String)}</CODE>, and
 *     trigger a render request to another navigation link url.
 * <li>Component Rendering URLs, they are created with <code> {@link HstResponse#createComponentRenderingURL()} </code>, and trigger
 *     a render request for a specific component
 * </ul>
 * <p>
 * The string representation of a HstURL does not need to be a valid 
 * URL at the time the <code>{@link HstComponent}</code> is generating its content. It may contain  
 * special tokens that will be converted to a valid URL by the container
 * before the content is returned to the client.
 * </p>
 * 
 * @version $Id$
 */
public interface HstURL {
    
    String ACTION_TYPE = "action";
    
    String RENDER_TYPE = "render";

    String COMPONENT_RENDERING_TYPE = "component-rendering";

    String RESOURCE_TYPE = "resource";
    
    /**
     * Returns the url type: render, action or resource
     * @return
     */
    String getType();
    
    /**
     * Returns the reference namespace
     * @return String
     */
    String getReferenceNamespace();
    
    /**
     * Sets a parameter of this url.
     * @param name
     * @param value
     */
    void setParameter(String name, String value);

    /**
     * Sets a parameter array of this url.
     * @param name
     * @param values
     */
    void setParameter(String name, String[] values);
    
    /**
     * Sets parameter map of this url
     * @param parameters
     */
    void setParameters(Map<String, String[]> parameters);
    
    /**
     * Returns string representation of this url.
     * @return
     */
    String toString();
    
    /**
     * Returns the parameter map of this url.
     * @return
     */
    Map<String, String[]> getParameterMap();
    
    /**
     * Writes the string representation of this url.
     * @param out
     * @throws IOException
     */
    void write(Writer out) throws IOException;
    
    /**
     * Writes the string representation of this url, as xml-escaped.
     * @param out
     * @param escapeXML
     * @throws IOException
     */
    void write(Writer out, boolean escapeXML) throws IOException;
    
    /**
     * Allows setting a resource ID that can be retrieved when serving the resource
     * through HstRequest.getResourceID() method in a HstComponent instance. 
     * 
     * @param resourceID
     */
    void setResourceID(String resourceID);
    
    /**
     * Returns the resource ID
     * @return String
     */
    String getResourceID();
        
}
