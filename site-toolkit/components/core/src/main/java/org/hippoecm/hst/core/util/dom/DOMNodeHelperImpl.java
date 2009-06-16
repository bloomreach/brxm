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

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.proxy.Invoker;
import org.dom4j.Node;
import org.dom4j.dom.DOMNodeHelper;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.w3c.dom.NodeList;

public class DOMNodeHelperImpl extends DOMNodeHelper {

    protected DOMNodeHelperImpl() {
        super();
    }

    public static org.w3c.dom.Attr asDOMAttr(final Node attribute) {
        if (attribute == null) {
            return null;
        }

        if (attribute instanceof org.w3c.dom.Attr) {
            return (org.w3c.dom.Attr) attribute;
        } else {
            ProxyFactory factory = new ProxyFactory();
            
            Invoker invoker = new Invoker() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodName = method.getName();
                    
                    if ("getName".equals(methodName)) {
                        return attribute.getName();
                    } else if ("getValue".equals(methodName)) {
                        return attribute.getText();
                    } else {
                        notSupported();
                    }
                    
                    return null;
                }
                
            };
            
            return (org.w3c.dom.Attr) factory.createInvokerProxy(invoker, new Class [] { org.w3c.dom.Attr.class });
        }
    }
    
    // Helper methods
    // -------------------------------------------------------------------------
    public static NodeList createNodeList(final List list) {
        return new NodeList() {
            public org.w3c.dom.Node item(int index) {
                if (index >= getLength()) {
                    /*
                     * From the NodeList specification: If index is greater than
                     * or equal to the number of nodes in the list, this returns
                     * null.
                     */
                    return null;
                } else {
                    return DOMNodeHelperImpl.asDOMNode((Node) list.get(index));
                }
            }

            public int getLength() {
                return list.size();
            }
        };
    }
    
    public static org.w3c.dom.Node asDOMNode(final Node node) {
        if (node == null) {
            return null;
        }

        org.w3c.dom.Node domNode = null;
        
        if (node instanceof org.w3c.dom.Node) {
            domNode = (org.w3c.dom.Node) node;
        } else {
            switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                domNode = (org.dom4j.dom.DOMElement) node;
                break;
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
            case Node.COMMENT_NODE:
            case Node.ENTITY_REFERENCE_NODE:
            case Node.PROCESSING_INSTRUCTION_NODE:
                ProxyFactory factory = new ProxyFactory();
                
                Invoker invoker = new Invoker() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        
                        if ("getNodeType".equals(methodName)) {
                            return node.getNodeType();
                        } else if ("getNodeValue".equals(methodName) || "getData".equals(methodName)) {
                            return node.getText();
                        } else if ("getNodeName".equals(methodName)) {
                            return node.getName();
                        } else {
                            notSupported();
                        }
                        
                        return null;
                    }
                };
                
                domNode = (org.w3c.dom.Node) factory.createInvokerProxy(invoker, new Class [] { org.w3c.dom.Node.class });

                break;
                
            default:
                System.out.println("Cannot convert: " + node + " into a W3C DOM Node");
                notSupported();
                break;
            }
        }
        
        return domNode;
    }
    
}
