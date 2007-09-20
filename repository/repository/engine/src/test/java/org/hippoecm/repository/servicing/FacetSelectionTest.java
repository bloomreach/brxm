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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetSelectionTest extends TestCase {
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
                                                                                                "language", "english",
        "/documents/articles/the-invisible-man",                        "hippo:handle",
        "/documents/articles/the-invisible-man/the-invisible-man",      "hippo:document",
                                                                                                "language", "english",
        "/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:document",
                                                                                                "language", "english",
        "/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:document",
                                                                                                "language", "dutch",
        "/documents/articles/nineteeneightyfour",                       "hippo:handle",
        "/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:document",
                                                                                                "language", "dutch",
        "/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:document",
                                                                                                "language", "english",
        "/english",                                                     "hippo:reference",
                                                                                                "hippo:docbase", "/documents",
        "/dutch",                                                       "hippo:facetselect",
        "hippo:docbase", "/documents/articles/war-of-the-worlds",
        "hippo:facets",  "language",
        "hippo:facets",  "state",
        "hippo:values", "english",
        "hippo:values", "published",
        "hippo:modes", "stick",
        "hippo:modes", "clear"
    };

    public void testFacetSelection() throws Exception {
        Exception firstException = null;
        HippoRepository repository = null;
        System.out.println("\n\n\n\n\n");
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            Session session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

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
            session.save();

            Utilities.dump(session.getRootNode());

            System.out.println("\n\n\n\n\n");
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
