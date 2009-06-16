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

import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Note: Because the DOMElement of dom4j (v1.6.1) is built on DOM Level2 API,
 * it does not have implementations on setTextContent() and getTextContent() of
 * org.w3c.dom.Node interface.
 * However, in Java 1.5 environment, we need to allow the methods invocations.
 * Also, because dom4j DOMElement does not support getOwnerDocument(),
 * we need to provide the method to create nodes with document.
 * 
 * @version $Id$
 */
public class DOMElementImpl extends DOMElement {
    
    private static final long serialVersionUID = 1L;
    
    private Document document;

    public DOMElementImpl(String name) {
        super(name);
    }

    public DOMElementImpl(QName qname) {
        super(qname);
    }

    public DOMElementImpl(QName qname, int attributeCount) {
        super(qname, attributeCount);
    }

    public DOMElementImpl(String name, Namespace namespace) {
        super(name, namespace);
    }

    @Override
    public Document getOwnerDocument() {
        if (document == null) {
            document = new DOMDocument(this);
        }
        
        return document;
    }

    public void setTextContent(String textContent) {
        setText(textContent);
    }
    
    public String getTextContent() {
        return getText();
    }
    
    public NamedNodeMap getAttributes() {
        return new DOMAttributeNodeMapImpl(this);
    }
    
    public NodeList getChildNodes() {
        return DOMNodeHelperImpl.createNodeList(content());
    }
    
}
