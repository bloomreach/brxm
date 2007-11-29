package org.hippoecm.frontend.plugins.admin.editor;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.MockControl;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.repository.api.HippoNode;

public class Application extends WicketTester.DummyWebApplication {

    public MockControl sessionControl;
    public javax.jcr.Session session;

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
    
    public MockControl workspaceControl;
    public Workspace workspace;
    
    public MockControl ntmgrControl;
    public NodeTypeManager ntmgr;
    
    public MockControl ntiterControl;
    public NodeTypeIterator ntiter;

    public MockControl nodeTypeControl;
    public NodeType nodeType;
    
    // custom Session; since UserSession is not an interface, we have to provide
    // an explicit implementation.
    private class Session extends UserSession {
        private static final long serialVersionUID = 1L;

        public Session(Request request) {
            super(request);
        }
        
        @Override
        public javax.jcr.Session getJcrSession() {
            return session;
        }
    }
    
    public Application() {
        super();

        sessionControl = MockControl.createControl(javax.jcr.Session.class);
        session = (javax.jcr.Session) sessionControl.getMock();

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
        
        workspaceControl = MockControl.createControl(Workspace.class);
        workspace = (Workspace) workspaceControl.getMock();
        
        ntmgrControl = MockControl.createControl(NodeTypeManager.class);
        ntmgr = (NodeTypeManager) ntmgrControl.getMock();
        
        ntiterControl = MockControl.createControl(NodeTypeIterator.class);
        ntiter = (NodeTypeIterator) ntiterControl.getMock();
        
        nodeTypeControl = MockControl.createControl(NodeType.class);
        nodeType = (NodeType) nodeTypeControl.getMock();
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
            propertyIteratorControl.setReturnValue(property, MockControl.ONE_OR_MORE);

            property.getPath();
            propertyControl.setReturnValue("/testnode/testproperty", MockControl.ONE_OR_MORE);

            property.getName();
            propertyControl.setReturnValue("testproperty", MockControl.ONE_OR_MORE);

            property.getValue();
            propertyControl.setReturnValue(value, MockControl.ONE_OR_MORE);

            property.getDefinition();
            propertyControl.setReturnValue(propertyDefinition, MockControl.ONE_OR_MORE);

            propertyDefinition.isProtected();
            propertyDefinitionControl.setReturnValue(false, MockControl.ONE_OR_MORE);

            propertyDefinition.isMultiple();
            propertyDefinitionControl.setReturnValue(false, MockControl.ONE_OR_MORE);

            value.getType();
            valueControl.setReturnValue(PropertyType.STRING, MockControl.ONE_OR_MORE);

            value.getString();
            valueControl.setReturnValue("testvalue", MockControl.ONE_OR_MORE);

            // set up the node types editor
            node.getMixinNodeTypes();
            nodeControl.setReturnValue(new NodeType[0], MockControl.ONE_OR_MORE);
            
            session.getWorkspace();
            sessionControl.setReturnValue(workspace, MockControl.ONE);

            workspace.getNodeTypeManager();
            workspaceControl.setReturnValue(ntmgr, MockControl.ONE);

            ntmgr.getMixinNodeTypes();
            ntmgrControl.setReturnValue(ntiter, MockControl.ONE);
            
            ntiter.hasNext();
            ntiterControl.setReturnValue(true, MockControl.ONE);

            ntiter.nextNodeType();
            ntiterControl.setReturnValue(nodeType, MockControl.ONE);

            nodeType.getName();
            nodeTypeControl.setReturnValue("test type", MockControl.ONE_OR_MORE);

            ntiter.hasNext();
            ntiterControl.setReturnValue(false, MockControl.ONE_OR_MORE);

            sessionControl.replay();
            nodeControl.replay();
            propertyIteratorControl.replay();
            propertyControl.replay();
            valueControl.replay();
            propertyDefinitionControl.replay();
            workspaceControl.replay();
            ntmgrControl.replay();
            ntiterControl.replay();
            nodeTypeControl.replay();
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

    @Override
    public Session newSession(Request request, Response response) {
        return new Session(request);
    }
}
