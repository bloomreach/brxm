package org.hippoecm.frontend.plugins.admin.editor;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.easymock.MockControl;
import org.hippoecm.repository.api.HippoNode;

public class MockJcr {
    
    public MockControl sessionControl;
    public Session session;

    public MockControl nodeControl;
    public HippoNode node;

    public MockControl propertyIteratorControl;
    public PropertyIterator propertyIterator;

    public MockControl propertyControl;
    public Property property;

    public MockControl propertyDefinitionControl;
    public PropertyDefinition propertyDefinition;

    public MockControl valueControl;
    public Value value;

    public MockJcr() {
        sessionControl = MockControl.createControl(Session.class);
        session = (Session) sessionControl.getMock();
        
        nodeControl = MockControl.createControl(HippoNode.class);
        node = (HippoNode) nodeControl.getMock();

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
            nodeControl.setReturnValue("/testnode", MockControl.ONE_OR_MORE);

            node.getProperties();
            nodeControl.setReturnValue(propertyIterator, MockControl.ONE_OR_MORE);
            
            node.getSession();
            nodeControl.setReturnValue(session, MockControl.ONE_OR_MORE);
            
            session.isLive();
            sessionControl.setReturnValue(true, MockControl.ONE_OR_MORE);

            propertyIterator.getSize();
            propertyIteratorControl.setReturnValue(1, MockControl.ONE_OR_MORE);

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
            propertyControl.setReturnValue(propertyDefinition, MockControl.ONE_OR_MORE);

            propertyDefinition.isProtected();
            propertyDefinitionControl.setDefaultReturnValue(false);

            propertyDefinition.isMultiple();
            propertyDefinitionControl.setDefaultReturnValue(false);

            value.getString();
            valueControl.setReturnValue("testvalue");

            sessionControl.replay();
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
        sessionControl.reset();
        nodeControl.reset();
        propertyIteratorControl.reset();
        propertyControl.reset();
        valueControl.reset();
        propertyDefinitionControl.reset();
    }

}
