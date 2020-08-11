/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.PropertyIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
import static org.hippoecm.repository.api.ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class EnhancedImportTest extends RepositoryTestCase {

    @Rule public final TestName testName = new TestName();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        removeNode("/test");
        removeNode("/compare");
        session.getRootNode().addNode("test");
        session.getRootNode().addNode("compare");
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/test");
        removeNode("/compare");
        session.save();
        super.tearDown();
    }

    private void test() throws Exception {
        test(null);
    }

    private void test(String[] expectedContextPaths) throws Exception {
        String name = testName.getMethodName().substring(4).toLowerCase();
        importXML("/test", name + "-fixture.xml");
        final ImportResult importResult = importXML("/test", name + "-merge.xml");
        if (expectedContextPaths != null) {
            final Collection<String> contextPaths = importResult.getContextPaths();
            assertArrayEquals(expectedContextPaths, contextPaths.toArray(new String[contextPaths.size()]));
        }
        importXML("/compare", name + "-result.xml");
        assertTrue(equals(session.getNode("/test"), session.getNode("/compare/test")));
    }

    private ImportResult importXML(final String path, final String resource) throws Exception {
        final ImportResult importResult = ((HippoSession) session).
                importEnhancedSystemViewXML(path, getClass().getResourceAsStream("/import/" + resource),
                        IMPORT_UUID_CREATE_NEW, IMPORT_REFERENCE_NOT_FOUND_REMOVE, null);
        session.save();
        return importResult;
    }

    @Test
    public void testSanity() throws Exception {
        test();
    }

    @Test
    public void testSkip() throws Exception {
        test();
    }

    @Test
    public void testCombine() throws Exception {
        test(new String[] { "/test/aap", "/test/aap/noot" });
    }

    @Test
    public void testOverlay() throws Exception {
        test(new String[] { "/test/aap", "/test/aap/noot" });
    }

    @Test
    public void testOverride() throws Exception {
        test();
    }

    @Test
    public void testAppend() throws Exception {
        test();
    }

    @Test
    public void testInsert() throws Exception {
        test();
    }

    @Test
    public void testProperty() throws Exception {
        test();
    }
    
    @Test
    public void testCombineTopProperty() throws Exception {
        test();
    }

    @Test
    public void testImplicitMerge() throws Exception {
        test();
    }

    protected boolean equals(Node a, Node b) throws RepositoryException {
        final boolean virtualA = JcrUtils.isVirtual(a);
        if (virtualA != JcrUtils.isVirtual(b)) {
            return false;
        } else if (virtualA) {
            return true;
        }

        final PropertyIterator aProperties = a.getProperties();
        final PropertyIterator bProperties = b.getProperties();

        Map<String, Property> properties = new HashMap<>();
        for (Property property : new PropertyIterable(aProperties)) {
            final String name = property.getName();
            if (property.getDefinition().isProtected()) {
                continue;
            }
            if (!b.hasProperty(name)) {
                return false;
            }

            properties.put(name, property);
        }
        for (Property bProp : new PropertyIterable(bProperties)) {
            final String name = bProp.getName();
            if (bProp.getDefinition().isProtected()) {
                continue;
            }
            if (!properties.containsKey(name)) {
                return false;
            }

            Property aProp = properties.get(name);
            if (!equals(bProp, aProp)) {
                return false;
            }
        }

        NodeIterator aIter = a.getNodes();
        NodeIterator bIter = b.getNodes();
        if (aIter.getSize() != bIter.getSize()) {
            return false;
        }
        while (aIter.hasNext()) {
            Node aChild = aIter.nextNode();
            Node bChild = bIter.nextNode();
            if (!equals(aChild, bChild)) {
                return false;
            }
        }
        return true;
    }

    private boolean equals(final Property bProp, final Property aProp) throws RepositoryException {
        if (aProp.isMultiple() != bProp.isMultiple() || aProp.getType() != bProp.getType()) {
            return false;
        }

        if (aProp.isMultiple()) {
            Value[] aValues = aProp.getValues();
            Value[] bValues = bProp.getValues();
            if (aValues.length != bValues.length) {
                return false;
            }
            for (int i = 0; i < aValues.length; i++) {
                if (!equals(aValues[i], bValues[i])) {
                    return false;
                }
            }
        } else {
            Value aValue = aProp.getValue();
            Value bValue = bProp.getValue();
            if (!equals(aValue, bValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean equals(final Value aValue, final Value bValue) throws RepositoryException {
        return aValue.getString().equals(bValue.getString());
    }
}
