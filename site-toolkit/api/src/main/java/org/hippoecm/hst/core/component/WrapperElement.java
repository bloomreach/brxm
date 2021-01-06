/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * WrapperElement interface to wrap markup contents
 * 
 * @version $Id$
 */
public interface WrapperElement extends Serializable, Cloneable
{

    /**
     * Return the tag name of the head contribution element
     */
    String getTagName();
    
    /**
     * Checks if the head contribution element has the attribute specified by the attribute name
     * @param name
     */
    boolean hasAttribute(String name);
    
    /**
     * Returns the attribute value of the head contribution element specified by the attribute name.
     * Returns null if there's no attribute specified by the attribute name.
     * @param name
     */
    String getAttribute(String name);
    
    /**
     * Returns unmodifiable attribute map of the head contribution element.
     * Returns an empty unmodifiable attribute map if there's no attribute.
     */
    Map<String, String> getAttributeMap();
    
    /**
     * Sets attribute on the head contribution element.
     * Returns null if there's no attribute in the head contribution element.
     * @param name
     * @param value
     */
    void setAttribute(String name, String value);
    
    /**
     * Removes the attribute from the head contribution element specified by the attribute name.
     * Returns the attribute value if the attribute exists, or returns null if the attribute doesn't exist.
     * @param name
     */
    String removeAttribute(String name);
    
    /**
     * Returns the text content of the head contribution element.
     */
    String getTextContent();
    
    /**
     * Sets the text content of the head contribution element.
     * @param textContent
     */
    void setTextContent(String textContent);

    /**
     * @return the set of attributes for which the values should not be encoded. If no specific attributes should be
     * skipped an empty {@link Set} is returned
     */
    Set<String> getSkipEscapingAttrs();
    
    /**
     * @see Object#clone()
     */
    Object clone() throws CloneNotSupportedException;
}
