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
package org.hippoecm.hst.core.util.dom;

import org.dom4j.dom.DOMAttributeNodeMap;
import org.dom4j.dom.DOMElement;
import org.w3c.dom.Node;

public class DOMAttributeNodeMapImpl extends DOMAttributeNodeMap {

    private DOMElement element;
    
    public DOMAttributeNodeMapImpl(DOMElement element) {
        super(element);
        this.element = element;
    }

    public Node item(int index) {
        return DOMNodeHelperImpl.asDOMAttr(element.attribute(index));
    }
    
}
