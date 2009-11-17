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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * HeadElement interface wrapping a head contribution element
 * 
 * @version $Id$
 */
public interface HeadElement extends Serializable
{
    
    String getTagName();
    
    boolean hasAttribute(String name);
    
    String getAttribute(String name);
    
    Map<String, String> getAttributeMap();
    
    String getTextContent();
    
    void setTextContent(String textContent);
    
    boolean hasChildHeadElements();
    
    Collection<HeadElement> getChildHeadElements();
    
}
