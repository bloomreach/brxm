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
package org.hippoecm.hst.test.basic;

import static org.junit.Assert.assertTrue;

import javax.jcr.Session;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Test;

public class BasicHstTestCase extends AbstractHstTestCase{

    public final static String TEST_VIRTURALHOSTS_PATH = "/hst:hst/hst:hosts";
    public final static String TEST_LIVE_SITE_PATH= "/hst:hst/hst:sites/unittestproject";
    public final static String TEST_SITE_CONTENT_PATH = "/unittestcontent/documents/unittestproject";
   
    
    /**
     * This test assures that their is some default node structure other tests
     * can rely on.
     * @throws Exception
     */
    @Test
    public void testDefaultHstConfiguration() throws Exception {

       Session session = this.getSession();
       assertTrue("Node '"+TEST_SITE_CONTENT_PATH+"' must exist",session.nodeExists(TEST_SITE_CONTENT_PATH));
       assertTrue("Node '"+TEST_LIVE_SITE_PATH+"' must exist",session.nodeExists(TEST_LIVE_SITE_PATH));
       assertTrue("Node '"+TEST_VIRTURALHOSTS_PATH+"s' must exist", session.nodeExists(TEST_VIRTURALHOSTS_PATH));

    }
    
}
