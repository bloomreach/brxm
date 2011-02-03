package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.NAME_QNAME;
import static org.hippoecm.repository.export.Constants.PROPERTY_QNAME;
import static org.hippoecm.repository.export.Constants.TYPE_QNAME;
import static org.hippoecm.repository.export.Constants.VALUE_QNAME;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

class NamespaceInstructionImpl extends AbstractInstruction implements NamespaceInstruction {

	private String m_namespace;
	private final String m_namespaceroot;
	private Element m_namespacePropertyValue;
	
	NamespaceInstructionImpl(String name, Double sequence, String namespace) {
		super(name, sequence);
		m_namespace = namespace;
		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
		m_namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
	}
	
	@Override
	public Element createInstructionElement() {
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
        m_namespacePropertyValue = namespacePropertyValue;
        element.add(namespaceProperty);
        return element;
	}
	
	public boolean matchesNamespace(String namespace) {
		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
		String namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
		return namespaceroot.equals(m_namespaceroot);
	}
	
	public void updateNamespace(String namespace) {
		m_namespace = namespace;
		m_namespacePropertyValue.setText(namespace);
	}
	
	@Override
	public String toString() {
		return "NamespaceInstruction[namespace=" + m_namespace + "]"; 
	}
}
