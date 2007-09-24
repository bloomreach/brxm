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

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetedReferenceTest extends TestCase {
    private final static String SVN_ID = "$$";

    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    private static String[] contents = new String[] {
        "/documents",                                                   "nt:unstructured",
        "/documents/pages",                                             "nt:unstructured",
        "/documents/pages/index",                                       "hippo:handle",
        "/documents/pages/index/index",                                 "hippo:document",
        "/documents/pages/index/index/links",                           "nt:unstructured",
        "/documents/pages/index/index/thema",                           "nt:unstructured",
        "/documents/articles",                                          "nt:unstructured",
        "/documents/articles/brave-new-world",                          "hippo:handle",
        "/documents/articles/brave-new-world/brave-new-world",          "hippo:document",
        "language","english",
        "/documents/articles/the-invisible-man",                        "hippo:handle",
        "/documents/articles/the-invisible-man/the-invisible-man",      "hippo:document",
        "language","english",
        "/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:document",
        "language","english",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:document",
        "language","dutch",
        "/documents/articles/nineteeneightyfour",                       "hippo:handle",
        "/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:document",
        "language","dutch",
        "/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:document",
        "language","english",
        "/english",                                                     "hippo:facetselect",
        "hippo:docbase", "/documents",
        "hippo:facets",  "language",
        "hippo:values",  "english",
        "hippo:modes",   "stick",
        "/dutch",                                                       "hippo:facetselect",
        "hippo:docbase", "/documents/articles/war-of-the-worlds",
        "hippo:facets",  "language",
        "hippo:facets",  "state",
        "hippo:values",  "dutch",
        "hippo:values",  "published",
        "hippo:modes",   "stick",
        "hippo:modes",   "clear"
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

    public void testFacetedReference() throws Exception {
        Exception firstException = null;
        HippoRepository repository = null;
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            Session session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            build(session, contents);
            session.save();
            Utilities.dump(session.getRootNode());
            assertNotNull(traverse(session,"/documents/articles/war-of-the-worlds/war-of-the-worlds"));
            assertNotNull(traverse(session,"/documents/articles/war-of-the-worlds/war-of-the-worlds[language='dutch']"));
            assertNotNull(traverse(session,"/documents/articles/war-of-the-worlds/war-of-the-worlds[language='english']"));
            assertNull(traverse(session,"/english/articles/war-of-the-worlds/war-of-the-worlds[language='dutch']"));
            assertNotNull(traverse(session,"english/articles/brave-new-world/brave-new-world"));
            assertNotNull(traverse(session,"/english/articles/war-of-the-worlds/war-of-the-worlds[language='english']"));
            assertNotNull(traverse(session,"/dutch/war-of-the-worlds[language='dutch']"));
            assertNull(traverse(session,"/dutch/war-of-the-worlds[language='english']"));
            session.logout();
        } catch (RepositoryException ex) {
            firstException = ex;
        } finally {
            try {
                if (repository != null) {
                    repository.close();
                    repository = null;
                }
            } catch (Exception ex) {
                if (firstException == null) {
                    firstException = ex;
                }
            }
            if (firstException != null)
                throw firstException;
        }
    }
}
