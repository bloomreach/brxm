/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;

import javax.jcr.ImportUUIDBehavior;

public abstract class EditorTestCase extends PluginTest {

    // the tests will modify templates, so backup and restore
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode("hippo:namespaces/test")) {
            JcrUtils.copy(session, "/hippo:namespaces/test", "/namespace-test-backup");
            session.save();
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (session != null && session.getRootNode().hasNode("namespace-test-backup")) {
            session.getRootNode().getNode("hippo:namespaces/test").remove();
            session.move("/namespace-test-backup", "/hippo:namespaces/test");
            session.save();
        }
        super.tearDown();
    }

}
