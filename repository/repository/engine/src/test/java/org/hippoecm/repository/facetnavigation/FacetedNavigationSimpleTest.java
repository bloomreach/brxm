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
package org.hippoecm.repository.facetnavigation;

import java.io.IOException;
import java.io.PrintStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.updater.UpdaterProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FacetedNavigationSimpleTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSimple() throws RepositoryException, IOException {
    	commonStart();
    }
    
    private void commonStart() throws RepositoryException{
    	Node test = session.getRootNode().addNode("test");
    	createSimpleStructure(test);
    	
    	
    	createFacetNode(test);
    	
    	session.save();
    	
    	Node node = session.getRootNode().getNode("test/facetnavigation/hippo:facetnavigation");
    	
    	traverse(node);
    	
    }
    
    
    
	private void traverse(Node node) throws RepositoryException {
		traverse(System.out, node, "", 0);
	}
	
	private void traverse(PrintStream out, Node node, String indent, int depth) throws RepositoryException {		
	 
	    if(depth > 20) {
	        System.err.println("Recursive traversion: ERROR");
	        return;
	    }
	    out.println(indent+"+" + node.getName()  + " (" + node.getPrimaryNodeType().getName() + ")");
		if(!node.isNodeType("hippo:testdocument")) {
			dumpProperties(out, node, indent);	
			NodeIterator nodes = node.getNodes();
			indent+="\t";
			while(nodes.hasNext()) {
			    depth++;
				traverse(out, nodes.nextNode(),indent, depth);
			}
		}
	}

	private void createSimpleStructure(Node test) throws RepositoryException {
    	Node documents = test.addNode("documents","nt:unstructured");
    	documents.addMixin("mix:referenceable");
		Node cars = documents.addNode("cars","nt:unstructured");
		
		// car 0
    	Node car = cars.addNode("car0","hippo:testdocument");
    	car.addMixin("hippo:harddocument");
    	
    	
		// car 1
        car = cars.addNode("car1","hippo:testdocument");
    	car.addMixin("hippo:harddocument");
    	car.addMixin("mix:referenceable");
    	car.setProperty("brand", "mercedes");
    	car.setProperty("color", "grey");
    	car.setProperty("product", "car");
    	
    	// car 2
//    	car = cars.addNode("car2","hippo:testdocument");
//    	car.addMixin("hippo:harddocument");
//    	car.addMixin("mix:referenceable");
//    	car.setProperty("brand", "volkswagen");
//    	car.setProperty("color", "grey");
//    	car.setProperty("product", "car");
//    	
//    	// car 3
//    	car = cars.addNode("car3","hippo:testdocument");
//    	car.addMixin("hippo:harddocument");
//    	car.addMixin("mix:referenceable");
//    	car.setProperty("brand", "peugeot");
//    	car.setProperty("color", "blue");
//    	car.setProperty("product", "car");
//    	
//    	// car 4
//    	car = cars.addNode("car4","hippo:testdocument");
//    	car.addMixin("hippo:harddocument");
//    	car.addMixin("mix:referenceable");
//    	car.setProperty("brand", "peugeot");
//    	car.setProperty("color", "grey");
//    	car.setProperty("product", "car");
    	
    	
	}

	private void createFacetNode(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:facetnavigation", HippoNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "brand", "color", "product" });
    }
    

	private void dumpProperties(Node node) throws RepositoryException {
		dumpProperties(System.out, node, "");
	}
	
	private void dumpProperties(PrintStream out, Node node, String indent) throws RepositoryException {
		
		for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            out.print(indent + "- " + prop.getName() + " = ");
            if (prop instanceof UpdaterProperty ? ((UpdaterProperty)prop).isMultiple() : prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                out.print("{ ");
                for (int i = 0; i < values.length; i++) {
                	out.print(i > 0 ? ", " : "");
                    if (values[i].getType() == PropertyType.BINARY) {
                    	out.print("<<binary>>");
                    } else {
                    	out.print(values[i].getString());
                    }
                }
                out.println(" } ");
            } else {
                if (!(prop instanceof UpdaterProperty) && prop.getType() == PropertyType.BINARY) {
                	out.println("<<binary>>");
                } else {
                	out.println(prop.getString());
                }
            }
        }
	}
	
    private void dumpFacetedView(Node node) throws RepositoryException {
    	dumpFacetedView(System.out, node, 0);
	}
    
	private static void dumpFacetedView(PrintStream out, Node node, int level) throws RepositoryException {
		
		if( !(node.isNodeType(HippoNodeType.NT_FACETSEARCH) ||  node.isNodeType(HippoNodeType.NT_FACETSUBSEARCH))) {
			// skip, not in faceted view
			return;
		}
        StringBuffer sb = new StringBuffer();
        if(level > 0) {
            for (int i = 0; i < level-1; i++) {
                sb.append("  ");
            }
            out.print(new String(sb));
            out.print("+ ");
            sb.append("  ");
        }
        // out.print(parent.getPath() + " [name=" + parent.getName() + ",depth=" + parent.getDepth());
        out.print((node.getName().equals("")?"/":node.getName()));
        if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
        	out.print( "(" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        }
        out.println(")" );
        
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            Node child = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
            	dumpFacetedView(out, child, level + 1);
            }
        }
    }
}
