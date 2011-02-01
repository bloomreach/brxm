/*
 *  Copyright 2011 Hippo (www.hippo.nl).
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
package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.*;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

class NamespaceInstruction extends Instruction {

	private final String m_namespace;
	private final String m_namespaceroot;
	
	NamespaceInstruction(String name, Double sequence, String namespace) {
		super(name, sequence);
		m_namespace = namespace;
		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
		m_namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
	}
	
	@Override
	Element createInstructionElement() {
        Element element = createBaseInstructionElement();
        // create element:
        // <sv:property sv:name="hippo:namespace" sv:type="String">
        //   <sv:value>{this.m_namespace}</sv:value>
        // </sv:property>
        Element namespaceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, NAME_QNAME, "hippo:namespace"));
        namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, TYPE_QNAME, "String"));
        Element namespacePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        namespacePropertyValue.setText(m_namespace);
        namespaceProperty.add(namespacePropertyValue);
        element.add(namespaceProperty);
        return element;
	}
	
	boolean matchesNamespace(String namespace) {
		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
		String namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
		return namespaceroot.equals(m_namespaceroot);
	}
			
	@Override
	public String toString() {
		return "NamespaceInstruction[namespace=" + m_namespace + "]"; 
	}
}