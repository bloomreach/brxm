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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class SearchOnNodeNameTest extends RepositoryTestCase {
   
    private static final String TEST_PATH = "test";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        Node testNode = session.getRootNode().addNode(TEST_PATH);
        session.save();
        String[] names = {"foobarlux", "fooluxbar", "barfoolux", "barluxfoo", "luxbarfoo", "luxfoo bar"};
        for (String name : names) {
            testNode.addNode(name);
        }
        session.save();
    }
    
    @Test
    public void testSearchNodeNameInNodeScope() throws RepositoryException {
        String xpath = "//*[jcr:contains(.,'luxfoo')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertTrue(queryResult.getNodes().hasNext());
    }

    @Test
    public void testSearchNodeNameInProp() throws RepositoryException {
        String xpath = "//*[jcr:contains(@hippo:_localname,'luxfoo')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertTrue(queryResult.getNodes().hasNext());
    }

    @Test
    public void testSearchWildcardNodeNameInProp() throws RepositoryException {
        String xpath = "//*[jcr:contains(@hippo:_localname,'luxfoo*')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertTrue(queryResult.getNodes().hasNext());
    }

    @Test
    public void testSearchWildcardNodeNameInNodeScope() throws RepositoryException {
        String xpath = "//*[jcr:contains(.,'luxfoo*')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertTrue(queryResult.getNodes().hasNext());
    }
}
