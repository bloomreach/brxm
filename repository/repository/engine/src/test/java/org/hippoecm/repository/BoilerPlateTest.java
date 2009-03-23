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

import static org.junit.Assert.*;

import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * An example class to show how to write unit tests for the repository.
 */
public class BoilerPlateTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // add test specific setup code here
    }

    @Override
    @After
    public void tearDown() throws Exception {
        // add test specific teardown code here
        super.tearDown();
    }
    
    /**
     * A trivial test as demo
     * @throws RepositoryException
     */
    @Test
    public void testSessionLiveTest() throws RepositoryException {
        assertTrue("super.setUp failed to create session", session.isLive());
    }
}

