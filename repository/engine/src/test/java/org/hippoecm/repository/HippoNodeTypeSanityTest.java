/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class HippoNodeTypeSanityTest extends RepositoryTestCase {

    private static final String PREFIX = "hippo:";

    private final Logger log = LoggerFactory.getLogger(HippoNodeTypeSanityTest.class);

    @Test
    public void testSanity() throws Exception {
        Workspace workspace = session.getWorkspace();
        NodeTypeManager ntmanager = workspace.getNodeTypeManager();

        // collect all the names defined through the CND
        Set<String> usedNodeTypes = new TreeSet<String>();
        Map<String, Set<String>> usedPropertyNames = new HashMap<String, Set<String>>();
        Map<String, Set<String>> usedNodeNames = new HashMap<String, Set<String>>();
        for (NodeTypeIterator iter = ntmanager.getAllNodeTypes(); iter.hasNext();) {
            NodeType nt = iter.nextNodeType();
            if (nt.getName().startsWith(PREFIX)) {
                String ntname = nt.getName().substring(PREFIX.length());
                usedNodeTypes.add(ntname);
                NodeDefinition[] nodedefs = nt.getChildNodeDefinitions();
                for (int i = 0; i < nodedefs.length; i++) {
                    if (nodedefs[i].getName().startsWith(PREFIX)) {
                        String name = nodedefs[i].getName().substring(PREFIX.length());
                        if (!usedNodeNames.containsKey(name))
                            usedNodeNames.put(name, new TreeSet<String>());
                        usedNodeNames.get(name).add(ntname);
                    } else if (!nodedefs[i].getName().startsWith("jcr:") && !nodedefs[i].getName().equals("*")) {
                        fail("definition in cnd file of " + nodedefs[i].getName() + " as defined in node "
                                + nt.getName() + " is invalid namespaced child node");
                    }
                }
                PropertyDefinition[] propdefs = nt.getPropertyDefinitions();
                for (int i = 0; i < propdefs.length; i++) {
                    if (propdefs[i].getName().startsWith(PREFIX)) {
                        String name = propdefs[i].getName().substring(PREFIX.length());
                        if (!usedPropertyNames.containsKey(name))
                            usedPropertyNames.put(name, new TreeSet<String>());
                        usedPropertyNames.get(name).add(ntname);
                    } else if (!propdefs[i].getName().startsWith("jcr:") && !propdefs[i].getName().equals("*")) {
                        fail("definition in cnd file of " + propdefs[i].getName() + " as defined in node "
                                + nt.getName() + " is invalid namespaced property");
                    }
                }
            }
        }

        // collect all the names in the HippoNodeType class file
        Field[] fields = HippoNodeType.class.getDeclaredFields();
        Set<String> definedNodeTypes = new TreeSet<String>();
        Map<String, Set<String>> definedPropertyNames = new HashMap<String, Set<String>>();
        Map<String, Set<String>> definedNodeNames = new HashMap<String, Set<String>>();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().startsWith("NT_")) {
                String ntname = fields[i].getName().substring("NT_".length()).toLowerCase();
                if (!(PREFIX + ntname).equals((String) (fields[i].get(null)))) {
                    log.warn("Convention to keep variable name and value in HippoNodeType in sync not kept");
                }
                definedNodeTypes.add(ntname);
            } else if (fields[i].getName().startsWith("HIPPO_")) {
                String name = fields[i].getName().substring("HIPPO_".length()).toLowerCase();
                if (!(PREFIX + name).equals((String) (fields[i].get(null)))) {
                    log.warn("Convention to keep variable name and value in HippoNodeType in sync not kept");
                }
                assertFalse("node name " + name + " multiple defined", definedNodeNames.containsKey(name));
                assertFalse("property name " + name + " multiple defined", definedPropertyNames.containsKey(name));
                if (usedNodeNames.containsKey(name) && usedPropertyNames.containsKey(name)) {
                    Set<String> definitions = new TreeSet<String>();
                    definitions.addAll(usedNodeNames.get(name));
                    definedNodeNames.put(name, definitions);
                    definitions = new TreeSet<String>();
                    definitions.addAll(usedPropertyNames.get(name));
                    definedPropertyNames.put(name, definitions);
                } else if (usedNodeNames.containsKey(name)) {
                    Set<String> definitions = new TreeSet<String>();
                    definitions.addAll(usedNodeNames.get(name));
                    definedNodeNames.put(name, definitions);
                } else if (usedPropertyNames.containsKey(name)) {
                    Set<String> definitions = new TreeSet<String>();
                    definitions.addAll(usedPropertyNames.get(name));
                    definedPropertyNames.put(name, definitions);
                } else {
                    log.warn("Field " + name + " defined in HippoNodeType is not found in cnd file");
                }
            } else if (fields[i].getName().endsWith("_PATH")) {
                String name = fields[i].getName().substring(0, fields[i].getName().length() - "_PATH".length())
                        .toLowerCase();
            } else if (fields[i].getName().equals("SVN_ID")) {
                // ignore
            } else {
                log.warn("Field " + fields[i] + " defined in HippoNodeType is of unrecognized naming convention");
            }
        }

        // compare both definitions
        for (String definedNodeType : definedNodeTypes) {
            if (!usedNodeTypes.contains(definedNodeType)) {
                log.warn("Missing definition of nodetype " + definedNodeType + " in cnd file");
            }
        }
        for (String usedNodeType : usedNodeTypes) {
            if (!definedNodeTypes.contains(usedNodeType)) {
                log.warn("Missing definition of nodetype " + usedNodeType + " in HippoNodeType class");
            }
        }
        // assertTrue(definedNodeTypes.equals(usedNodeTypes));

        for (String definedNodeName : definedNodeNames.keySet()) {
            if (!usedNodeNames.containsKey(definedNodeName)) {
                log.warn("Missing definition of child node name " + definedNodeName + " in cnd file");
            } else if (!usedNodeNames.get(definedNodeName).equals(definedNodeNames.get(definedNodeName))) {
                log.warn("Unequal definition of child node name " + definedNodeName + " in cnd file");
            }
        }
        for (String usedNodeName : usedNodeNames.keySet()) {
            if (!definedNodeNames.containsKey(usedNodeName)) {
                log.warn("Missing definition of child node name " + usedNodeName + " in HippoNodeType class");
            } else if (!definedNodeNames.get(usedNodeName).equals(usedNodeNames.get(usedNodeName))) {
                log.warn("Unequal definition of child node name " + usedNodeName + " in HippoNodeType class");
            }
        }
        // assertTrue(definedNodeNames.equals(usedNodeNames));

        for (String definedPropertyName : definedPropertyNames.keySet()) {
            if (!usedPropertyNames.containsKey(definedPropertyName)) {
                log.warn("Missing definition of child node name " + definedPropertyName + " in cnd file");
            } else if (!usedPropertyNames.get(definedPropertyName)
                    .equals(definedPropertyNames.get(definedPropertyName))) {
                log.warn("Unequal definition of child node name " + definedPropertyName + " in cnd file");
            }
        }
        for (String usedPropertyName : usedPropertyNames.keySet()) {
            if (!definedPropertyNames.containsKey(usedPropertyName)) {
                log.warn("Missing definition of child node name " + usedPropertyName + " in HippoPropertyType class");
            } else if (!definedPropertyNames.get(usedPropertyName).equals(usedPropertyNames.get(usedPropertyName))) {
                log.warn("Unequal definition of child node name " + usedPropertyName + " in HippoPropertyType class");
            }
        }
        // assertTrue(definedPropertyNames.equals(usedPropertyNames));
    }
}
