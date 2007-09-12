package org.hippoecm.repository.plugins.admin.editor;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.easymock.MockControl;
import org.hippoecm.repository.servicing.ServicingNode;

public class MockJcr {

    public MockControl nodeControl;
    public ServicingNode node;

    public MockControl propertyIteratorControl;
    public PropertyIterator propertyIterator;

    public MockControl propertyControl;
    public Property property;

    public MockControl propertyDefinitionControl;
    public PropertyDefinition propertyDefinition;

    public MockControl valueControl;
    public Value value;

    public MockJcr() {
        nodeControl = MockControl.createControl(ServicingNode.class);
        node = (ServicingNode) nodeControl.getMock();

        propertyIteratorControl = MockControl.createControl(PropertyIterator.class);
        propertyIterator = (PropertyIterator) propertyIteratorControl.getMock();

        propertyControl = MockControl.createControl(Property.class);
        property = (Property) propertyControl.getMock();

        valueControl = MockControl.createControl(Value.class);
        value = (Value) valueControl.getMock();

        propertyDefinitionControl = MockControl.createControl(PropertyDefinition.class);
        propertyDefinition = (PropertyDefinition) propertyDefinitionControl.getMock();
    }

    public void setUp() {
        try {
            node.getPath();
            nodeControl.setReturnValue("/testnode", 2);

            node.getProperties();
            nodeControl.setReturnValue(propertyIterator, 2);

            propertyIterator.getSize();
            propertyIteratorControl.setReturnValue(1, 2);

            propertyIterator.skip(0);

            propertyIterator.nextProperty();
            propertyIteratorControl.setReturnValue(property);

            property.getPath();
            propertyControl.setReturnValue("/testnode/testproperty");

            property.getName();
            propertyControl.setReturnValue("testproperty");

            property.getValue();
            propertyControl.setReturnValue(value);

            property.getDefinition();
            propertyControl.setReturnValue(propertyDefinition, 6);

            propertyDefinition.isProtected();
            propertyDefinitionControl.setDefaultReturnValue(false);

            propertyDefinition.isMultiple();
            propertyDefinitionControl.setDefaultReturnValue(false);

            value.getString();
            valueControl.setReturnValue("testvalue");

            nodeControl.replay();
            propertyIteratorControl.replay();
            propertyControl.replay();
            valueControl.replay();
            propertyDefinitionControl.replay();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void tearDown() {
        nodeControl.reset();
        propertyIteratorControl.reset();
        propertyControl.reset();
        valueControl.reset();
        propertyDefinitionControl.reset();
    }

}
