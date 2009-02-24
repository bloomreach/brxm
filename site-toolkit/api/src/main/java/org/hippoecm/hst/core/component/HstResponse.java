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

import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;

public interface HstResponse extends HttpServletResponse {
    
    /**
     * Creates a HST URL targeting the HstComponent.
     * The created URL will per default not contain any parameters of the current request.
     *  
     * @return
     */
    HstURL createURL(String type);
    
    /**
     * The value returned by this method should be prefixed or appended to elements, 
     * such as JavaScript variables or function names, to ensure they are unique 
     * in the context of the HST-managed page.
     * The namespace value must be constant for the lifetime of the HstComponentWindow. 
     * @return
     */
    String getNamespace();
    
    /**
     * Creates an element of the type specified to be used in the {@link #addProperty(String, Element)} method. 
     * 
     * @param tagName the tag name of the element
     * @return DOM element with the tagName
     */
    Element createElement(String tagName);
    
    /**
     * Adds an header element property to the response.
     * If a header element with the provided key already exists 
     * the provided element will be stored in addition to 
     * the existing element under the same key.
     * If the element is null the key is removed from the response.
     * If these header values are intended to be transmitted to the client 
     * they should be set before the response is committed.
     * 
     * @param key
     * @param element
     */
    void addProperty(String key, Element element);
    
    /**
     * Retrieves header element property map.
     * This method is supposed to be invoked by the parent HstComponent
     * to render some header tag elements in a non-portal environment.
     * Under portal environment, this method is not supposed to be invoked
     * because the header tag elements should be written by the portal.
     * Under portal environment, the HstComponents can write some body
     * tag fragments only. If a HstComponent contributes some header
     * tag elements by invoking {@link #addProperty(String, Element)},
     * then the portal will write all the merged head tag elements finally.
     * 
     * @param key
     * @param element
     */
    Map<String, Element> getProperties();
    
}
