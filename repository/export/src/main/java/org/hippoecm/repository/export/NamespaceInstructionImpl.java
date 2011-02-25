package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.NAME_QNAME;
import static org.hippoecm.repository.export.Constants.PROPERTY_QNAME;
import static org.hippoecm.repository.export.Constants.TYPE_QNAME;
import static org.hippoecm.repository.export.Constants.VALUE_QNAME;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

class NamespaceInstructionImpl extends AbstractInstruction implements NamespaceInstruction {

	private String namespace;
	private final String namespaceroot;
	private Element namespacePropertyValue;
	
	NamespaceInstructionImpl(String name, Double sequence, String namespace, Element namespacePropertyValue) {
		super(name, sequence);
		this.namespace = namespace;
		this.namespacePropertyValue = namespacePropertyValue;
		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
		this.namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
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
        namespacePropertyValue.setText(namespace);
        namespaceProperty.add(namespacePropertyValue);
        this.namespacePropertyValue = namespacePropertyValue;
        element.add(namespaceProperty);
        return element;
	}
	
	public boolean matchesNamespace(String namespace) {
		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
		String namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
		return namespaceroot.equals(this.namespaceroot);
	}
	
	public void updateNamespace(String namespace) {
		this.namespace = namespace;
		this.namespacePropertyValue.setText(namespace);
	}
	
	@Override
	public String toString() {
		return "NamespaceInstruction[namespace=" + namespace + "]"; 
	}
}
