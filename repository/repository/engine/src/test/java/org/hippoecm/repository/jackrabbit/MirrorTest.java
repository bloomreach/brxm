/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.servicing;

import java.io.PrintStream;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;

public class MirrorTest extends TestCase {
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();
   
    private static String[] contents = new String[] {
        "/documents", "nt:unstructured",
        "niet", "hier",
        "/navigation", "nt:unstructured",
        "/navigation/mirror", "hippo:mirror",
        "hippo:docbase", "/documents",

        "/documents/test1", "nt:unstructured",
        "/documents/test2", "nt:unstructured",
        "wel","anders",
        "/documents/test3", "nt:unstructured",
        "/documents/test3/test4", "nt:unstructured",
        "lachen", "zucht",
        "/documents/test3/test4/test5", "nt:unstructured",
        "/documents/test3/test4/test5", "nt:unstructured"
    };

    private void build(Session session, String[] contents) throws RepositoryException {
        Node node = null;
        for (int i=0; i<contents.length; i+=2) {
            if (contents[i].startsWith("/")) {
                String path = contents[i].substring(1);
                node = session.getRootNode();
                if (path.contains("/")) {
                    node = node.getNode(path.substring(0,path.lastIndexOf("/")));
                    path = path.substring(path.lastIndexOf("/")+1);
                }
                node = node.addNode(path, contents[i+1]);
            } else {
                PropertyDefinition propDef = null;
                PropertyDefinition[] propDefs = node.getPrimaryNodeType().getPropertyDefinitions();
                for (int propidx=0; propidx<propDefs.length; propidx++)
                    if(propDefs[propidx].getName().equals(contents[i])) {
                        propDef = propDefs[propidx];
                        break;
                    }
                if (propDef != null && propDef.isMultiple()) {
                    Value[] values;
                    if (node.hasProperty(contents[i])) {
                        values = node.getProperty(contents[i]).getValues();
                        Value[] newValues = new Value[values.length+1];
                        System.arraycopy(values,0,newValues,0,values.length);
                        values = newValues;
                    } else {
                        values = new Value[1];
                    }
                    values[values.length-1] = session.getValueFactory().createValue(contents[i+1]);
                    node.setProperty(contents[i], values);
                } else {
                    node.setProperty(contents[i], contents[i+1]);
                }
            }
        }
    }

    private Node traverse(Session session, String path) throws RepositoryException {
        if(path.startsWith("/"))
            path = path.substring(1);
        return traverse(session.getRootNode(), path);
    }

    private Node traverse(Node node, String path) throws RepositoryException {
        String[] pathElts = path.split("/");
        for(int pathIdx=0; pathIdx<pathElts.length && node != null; pathIdx++) {
            String relPath = pathElts[pathIdx];
            Map<String,String> conditions = null;
            if(relPath.contains("[") && relPath.endsWith("]")) {
                conditions = new TreeMap<String,String>();
                String[] conditionElts = relPath.substring(relPath.indexOf("[")+1,relPath.lastIndexOf("]")).split(",");
                for(int conditionIdx=0; conditionIdx<conditionElts.length; conditionIdx++) {
                    int pos = conditionElts[conditionIdx].indexOf("=");
                    if(pos >= 0) {
                        String key = conditionElts[conditionIdx].substring(0,pos);
                        String value = conditionElts[conditionIdx].substring(pos+1);
                        if(value.startsWith("'") && value.endsWith("'"))
                            value = value.substring(1,value.length()-1);
                        conditions.put(key, value);
                    } else
                        conditions.put(conditionElts[conditionIdx], null);
                }
                relPath = relPath.substring(0,relPath.indexOf("["));
            }
            if(conditions == null || conditions.size() == 0) {
                if(node.hasNode(relPath)) {
                    try {
                        node = node.getNode(relPath);
                    } catch(PathNotFoundException ex) {
                        return null;
                    }
                } else
                    return null;
            } else {
                for(NodeIterator iter = node.getNodes(relPath); iter.hasNext(); ) {
                    node = iter.nextNode();
                    for(Map.Entry<String,String> condition: conditions.entrySet()) {
                        if(node.hasProperty(condition.getKey())) {
                            if(condition.getValue() != null) {
                                try {
                                    if(!node.getProperty(condition.getKey()).getString().equals(condition.getValue())) {
                                        node = null;
                                        break;
                                    }
                                } catch(PathNotFoundException ex) {
                                    node = null;
                                    break;
                                } catch(ValueFormatException ex) {
                                    node = null;
                                    break;
                                }
                            }
                        } else {
                           node = null;
                            break;
                        }
                    }
                    if(node != null)
                        break;
                }
            }
        }
        return node;
    }

    public void testMirror() throws Exception {
        PrintStream pstream = new PrintStream("dump.txt");

        HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
        Session session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        build(session, contents);
        session.save();
        session.logout();

        session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        Utilities.dump(pstream, session.getRootNode());
        pstream.println("===");

        assertNotNull(session.getRootNode());
        assertTrue(session.getRootNode().hasNode("navigation"));
        assertNotNull(session.getRootNode().getNode("navigation"));
        assertTrue(session.getRootNode().getNode("navigation").hasNode("mirror"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasProperty("hippo:docbase"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getProperty("hippo:docbase"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test1"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1"));

        Utilities.dump(pstream, session.getRootNode());
        pstream.println("===");

        session.getRootNode().addNode("dummy");
        session.getRootNode().getNode("documents").addNode("test-a","nt:unstructured").setProperty("test-b","test-c");
        session.getRootNode().getNode("documents").getNode("test1").addNode("test-x");
        session.save();
        session.refresh(true);

        Utilities.dump(pstream, session.getRootNode());
        pstream.println("===");

        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test-a"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test-a"));
        assertTrue(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1").hasNode("test-x"));
        assertNotNull(session.getRootNode().getNode("navigation").getNode("mirror").getNode("test1").getNode("test-x"));
        assertFalse(session.getRootNode().getNode("navigation").getNode("mirror").hasNode("test1[2]"));

        session.logout();

        repository.close();
    }
}
