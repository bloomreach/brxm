/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.onehippo.cms7.jcrdiff.JcrDiffException;
import org.onehippo.cms7.jcrdiff.content.jcr.JcrTreeNode;
import org.onehippo.cms7.jcrdiff.delta.Operation;
import org.onehippo.cms7.jcrdiff.delta.Patch;
import org.onehippo.cms7.jcrdiff.match.Matcher;
import org.onehippo.cms7.jcrdiff.match.MatcherItemInfo;
import org.onehippo.cms7.jcrdiff.match.PatchFactory;
import org.onehippo.cms7.jcrdiff.serialization.PatchWriter;
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
        assertTrue(compare(session.getNode("/test"), session.getNode("/compare/test")));
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

    private boolean compare(Node node1, Node node2) throws RepositoryException, JcrDiffException, IOException {

        final Matcher matcher = new Matcher();
        final MatcherItemInfo currentInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(node1));
        final MatcherItemInfo referenceInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(node2));
        matcher.setSource(referenceInfo);
        matcher.setResult(currentInfo);
        matcher.match();

        final PatchFactory factory = new PatchFactory();
        final Patch patch = factory.createPatch(referenceInfo, currentInfo);

        final List<Operation> operations = patch.getOperations();
        if (!operations.isEmpty()) {
            final PatchWriter patchWriter = new PatchWriter(patch, new OutputStreamWriter(System.out));
            patchWriter.writePatch();
            return false;
        }
        return true;
    }

}
