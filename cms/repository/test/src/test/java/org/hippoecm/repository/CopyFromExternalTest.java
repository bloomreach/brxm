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
package org.hippoecm.repository;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

import org.junit.*;
import static org.junit.Assert.*;

public class CopyFromExternalTest extends org.hippoecm.repository.TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";


    class Reference {
        Node sourceNode;
        String sourceProperty;
        Node destinationNode;
        String destinationProperty;
        public Reference(Node sourceNode, String sourceProperty, Node destinationNode, String destinationProperty) {
            this.sourceNode = sourceNode;
            this.sourceProperty = sourceProperty;
            this.destinationNode = destinationNode;
            this.destinationProperty = destinationProperty;
        }
    }

    Set<Reference> uuidProperties;
    HippoRepository external;
    ValueFactory valueFactory;

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown(false);
    }

    private void copy(Node source, Node parent) throws RepositoryException {
        System.err.println("COPY "+source.getPath()+" "+parent.getPath()+" "+source.getPrimaryNodeType().getName());

        // find out the name and types of the old node
        String nodeType = source.getPrimaryNodeType().getName();
        NodeType[] mixinNodeTypes = source.getMixinNodeTypes();
        Set<String> mixinTypes = new HashSet();
        for(int i=0; i<mixinNodeTypes.length; i++) {
            mixinTypes.add(mixinNodeTypes[i].getName());
        }
        String nodeName = source.getName();

        // change the type of deprecated structures
        if(nodeType.equals("nt:unstructured") && mixinTypes.contains("hippo:prototyped")) {
            nodeType = "hippostd:folder";
            mixinTypes.remove("hippo:prototyped");
        } else if(nodeType.equals("hippostd:folder") && mixinTypes.contains("hippo:prototyped")) {
            // this type should never been here
            mixinTypes.remove("hippo:prototyped");
        }

        // create the destination node
        Node destination = parent.addNode(nodeName, nodeType);

        // add mixins that should be there
        if(source.isNodeType("hippo:document") && !destination.isNodeType("hippo:harddocument") &&
                                                  !mixinTypes.contains("hippo:harddocument")) {
            mixinTypes.add("hippo:harddocument");
        }
        if(source.isNodeType("hippo:handle") && !destination.isNodeType("hippo:hardhandle") &&
                                                !mixinTypes.contains("hippo:hardhandle")) {
            mixinTypes.add("hippo:hardhandle");
        }

        // remove mixins that are already present
        boolean changed;
        do {
            changed = false;
            for(String mixinType : mixinTypes) {
                if(destination.isNodeType(mixinType)) {
                    mixinTypes.remove(mixinType);
                    changed = true;
                    break;
                }
            }
        } while(changed);

        for(String mixinType : mixinTypes) {
            destination.addMixin(mixinType);
        }

        for(NodeIterator iter = source.getNodes(); iter.hasNext(); ) {
            copy(iter.nextNode(), destination);
        }
        for(PropertyIterator iter = source.getProperties(); iter.hasNext(); ) {
            Property p = iter.nextProperty();
            String propertyName = p.getName();
            String originalPropertyName = propertyName;
            if(destination.isNodeType("hippostd:publishable") && propertyName.equals("hippostd:username")) {
                propertyName = "hippostd:holder";
            }
            // skip properties that are auto generated
            if(propertyName.equals("hippo:paths")) {
                continue;
            }
            // skip properties that are auto generated
            if(propertyName.equals("hippo:prototype")) {
                continue;
            }
            if(!p.getDefinition().isProtected()) {
                if(p.getDefinition().isMultiple()) {
                    Value[] values = p.getValues();
                    if(p.getType() == PropertyType.REFERENCE) {
                        uuidProperties.add(new Reference(source, originalPropertyName, destination, propertyName));
                    } else {
                        Value[] newValues = new Value[values.length];
                        for(int i=0; i<newValues.length; i++)
                            newValues[i] = valueFactory.createValue(values[i].getString(), p.getType());;
                        destination.setProperty(propertyName, values);
                    }
                } else {
                    Value value = p.getValue();
                    if(p.getType() == PropertyType.REFERENCE || propertyName.equals("hippo:docbase")) {
                        uuidProperties.add(new Reference(source, originalPropertyName, destination, propertyName));
                    } else {
                        Value newValue = valueFactory.createValue(value.getString(), p.getType());
                        destination.setProperty(propertyName, value);
                    }
                }
            }
        }
    }

    // HREPTWO-1118: enable this test by commenting replacing @Ignore by @Test
    @Ignore
    public void testCopy() throws Exception {
        // HREPTWO-1118: replace the rmi address below to the actual server to be copied from
        external = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        valueFactory = session.getValueFactory();

        Session source = external.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        uuidProperties = new HashSet<Reference>();
        copy(source.getRootNode().getNode("content"), session.getRootNode());
        for(Reference ref : uuidProperties) {
            Property property = ref.sourceNode.getProperty(ref.sourceProperty);
            if(property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                Value[] newValues = new Value[values.length];
                for(int i=0; i<values.length; i++) {
                    String targetPath = ref.sourceNode.getSession().getNodeByUUID(values[i].getString()).getPath();
                    Node targetNode = ref.destinationNode.getSession().getRootNode().getNode(targetPath.substring(1));
                    newValues[i] = valueFactory.createValue(targetNode.getUUID(), property.getType());
                }
                ref.destinationNode.setProperty(ref.destinationProperty, newValues);
            } else {
                String targetPath = ref.sourceNode.getSession().getNodeByUUID(property.getString()).getPath();
                Node targetNode = ref.destinationNode.getSession().getRootNode().getNode(targetPath.substring(1));
                Value newValue = valueFactory.createValue(targetNode.getUUID(), property.getType());
                ref.destinationNode.setProperty(ref.destinationProperty, newValue);
            }
        }
        session.save();
    }

    @Test
    public void testDummy() {
    }
}
