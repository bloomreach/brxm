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
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example class to show how to write unit tests for the repository.
 */
public class HREPTWO3402IssueTest extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(HREPTWO3402IssueTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * This test only fails while running over SPI. When this 
     * bug is fixed the testDeletesAllowed test should be re-enabled.
     * @see FacetedAuthorizationTest#testDeletesAllowed()
     * @throws RepositoryException
     */
    @Test
    public void testIssue() throws RepositoryException {

        Session first= server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Session second = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        
        // setup data
        Node test = first.getRootNode().addNode("testdata");
        test.addNode("extra");
        test.addNode("writedoc0").addNode("subwrite");
        first.save();
        
        // do some node manipulation with second session
        Node testData = second.getRootNode().getNode("testdata");
        Node node = testData.getNode("writedoc0/subwrite");
        node.remove();
        second.save();
        
        // start fresh with first session...
        first.refresh(false);
        if (first.getRootNode().hasNode("testdata")) {
            try {
                first.getRootNode().getNode("testdata").remove();
                // running embedded or over rmi
            } catch (RepositoryException e) {
                // running over spi
                log.error("This session is the only one removing the node, but it ran into an error: " + e.getMessage());
            }
        }
        first.save();
    }
}

