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
package org.hippoecm.repository;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.junit.Before;
import org.junit.Test;

public class SearchOnNodeNameTest extends TestCase {
   
    private static final String TEST_PATH = "test";
    private Node testPath;

    private List<String> names;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        testPath = session.getRootNode().addNode(TEST_PATH);
        session.save();
        names = new ArrayList<String>();
        names.add("foobarlux");
        names.add("fooluxbar");
        names.add("barfoolux");
        names.add("barluxfoo");
        names.add("luxbarfoo");
        names.add("luxfoo bar");
        for (String name : names) {
            testPath.addNode(name);
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
