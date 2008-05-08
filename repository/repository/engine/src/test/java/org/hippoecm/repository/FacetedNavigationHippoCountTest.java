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
package org.hippoecm.repository;

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

import org.hippoecm.repository.TestCase;
import org.junit.*;
import static org.junit.Assert.*;

public class FacetedNavigationHippoCountTest extends TestCase
{
    private static String[] contents1 = new String[] {
        "/test", "nt:unstructured",
        "/test/documents", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/document", "hippo:testdocument",
        "type", "text",
        "/test/documents/document1", "hippo:testdocument",
        "type", "html"
    };
    private static String[] contents2 = new String[] {
        "/test/docsearch", "nt:unstructured",
        "/test/docsearch/byType", "hippo:facetsearch",
        "hippo:docbase", "/test/documents",
        "hippo:queryname", "byType",
        "hippo:facets", "type"
    };

    @Test public void testHippoCount() {
        try {
            build(session, contents1);
            session.save();
            build(session, contents2);
            session.save();
            assertTrue(traverse(session, "/test/docsearch/byType/hippo:resultset").hasProperty("hippo:count"));
        } catch(RepositoryException ex) {
            ex.printStackTrace();
        }
    }
}
