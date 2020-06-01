/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class ChildAxisQueryTest extends RepositoryTestCase {

    private String[] content = new String[]{
            "/test", "nt:unstructured",
            "/test/Document1", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/Document1/Document1", "hippo:testsearchdocument",
            "jcr:mixinTypes", "mix:referenceable",
            "title", "foo",
    };


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testExistingChildAxis() throws Exception {
        String xpath = "/jcr:root/test/Document1/Document1";
        assertEquals(1L, session.getWorkspace().getQueryManager().createQuery(xpath,"xpath").execute().getNodes().getSize());
    }

    @Test
    public void testExistingChildAxisAndConstraint() throws Exception {
        String xpath = "/jcr:root/test/Document1/Document1[@title='foo']";
        assertEquals(1L, session.getWorkspace().getQueryManager().createQuery(xpath,"xpath").execute().getNodes().getSize());
    }

    @Test
    public void testNoneExistingPathChildAxisAndConstraint() throws Exception {
        String xpath = "/jcr:root/test/foo/bar";
        assertEquals(0L, session.getWorkspace().getQueryManager().createQuery(xpath,"xpath").execute().getNodes().getSize());
    }

    @Test
    public void testNoneExistingSiblingChildAxis() throws Exception {
        String xpath = "/jcr:root/test/Document1/Document1[2]";
        assertEquals(0L, session.getWorkspace().getQueryManager().createQuery(xpath,"xpath").execute().getNodes().getSize());

        xpath = "/jcr:root/test/Document1[8]";
        assertEquals(0L, session.getWorkspace().getQueryManager().createQuery(xpath,"xpath").execute().getNodes().getSize());

        xpath = "/jcr:root/test[6]";
        assertEquals(0L, session.getWorkspace().getQueryManager().createQuery(xpath,"xpath").execute().getNodes().getSize());
    }

}
