/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowseLinkTargetTest extends PluginTest {

    @Test
    public void testExistingTarget() throws Exception {
        Node testNode = root.addNode("test", HippoNodeType.NT_HANDLE);
        BrowseLinkTarget target = new BrowseLinkTarget(testNode.getPath());
        assertEquals("test", target.getName());
        assertEquals("/test", target.getDisplayPath());
    }

    @Test
    public void testNonExistingTarget() throws Exception {
        BrowseLinkTarget target = new BrowseLinkTarget("/test");
        assertEquals("test", target.getName());
        assertEquals("/test", target.getDisplayPath());
    }

}
