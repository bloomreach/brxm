package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.NAME_QNAME;
import static org.hippoecm.repository.export.Constants.NODE_QNAME;
import static org.hippoecm.repository.export.Constants.PROPERTY_QNAME;
import static org.hippoecm.repository.export.Constants.TYPE_QNAME;
import static org.hippoecm.repository.export.Constants.VALUE_QNAME;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractInstruction implements Instruction {
	
	
    static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");

    
    final String m_name;
    final Double m_sequence;

    
    AbstractInstruction(String name, Double sequence) {
        m_name = name;
        m_sequence = sequence;
    }
    
    public String getName() {
    	return m_name;
    }
    
    Element createBaseInstructionElement() {
        // create element:
        // <sv:node sv:name="{m_name}"/>
        Element instructionNode = DocumentFactory.getInstance().createElement(NODE_QNAME);
        instructionNode.add(DocumentFactory.getInstance().createAttribute(instructionNode, NAME_QNAME, m_name));
        // create element:
        // <sv:property sv:name="jcr:primaryType" sv:type="Name">
        //   <sv:value>hippo:initializeitem</sv:value>
        // </sv:property>
        Element primaryTypeProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        primaryTypeProperty.add(DocumentFactory.getInstance().createAttribute(primaryTypeProperty, NAME_QNAME, "jcr:primaryType"));
        primaryTypeProperty.add(DocumentFactory.getInstance().createAttribute(primaryTypeProperty, TYPE_QNAME, "Name"));
        Element primaryTypePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        primaryTypePropertyValue.setText("hippo:initializeitem");
        primaryTypeProperty.add(primaryTypePropertyValue);
        instructionNode.add(primaryTypeProperty);
        // create element:
        // <sv:property sv:name="hippo:sequence" sv:type="Double">
        //   <sv:value>{m_sequence}</sv:value>
        // </sv:property>
        Element sequenceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        sequenceProperty.add(DocumentFactory.getInstance().createAttribute(sequenceProperty, NAME_QNAME, "hippo:sequence"));
        sequenceProperty.add(DocumentFactory.getInstance().createAttribute(sequenceProperty, TYPE_QNAME, "Double"));
        Element sequencePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        sequencePropertyValue.setText(String.valueOf(m_sequence));
        sequenceProperty.add(sequencePropertyValue);
        instructionNode.add(sequenceProperty);
        return instructionNode;
    }

	
}
