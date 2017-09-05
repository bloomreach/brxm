/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.domains;

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DomainTest extends PluginTest {


    @Test
    public void testCreateAuthRole() throws Exception {
        final Node domainNode = root.getNode("hippo:configuration/hippo:domains/autoexport");
        try {
            Domain domain = new Domain(domainNode);
            assertFalse(domain.getAuthRoles().containsKey("author"));
            assertFalse(domain.getAuthRoles().containsKey("editor"));
            int numAuthRoles = domain.getAuthRoles().size();
            domain.addGroupToRole("author", "author");
            domain.addGroupToRole("editor", "editor");
            domain.addGroupToRole("editor", "webmaster");
            assertEquals(numAuthRoles+2, domain.getAuthRoles().size());
            assertTrue(domain.getAuthRoles().containsKey("author"));
            assertTrue(domain.getAuthRoles().containsKey("editor"));
            assertEquals(1, domain.getAuthRoles().get("author").getGroupnames().size());
            assertTrue(domain.getAuthRoles().get("author").getGroupnames().contains("author"));
            assertEquals(2, domain.getAuthRoles().get("editor").getGroupnames().size());
            assertTrue(domain.getAuthRoles().get("editor").getGroupnames().contains("editor"));
            assertTrue(domain.getAuthRoles().get("editor").getGroupnames().contains("webmaster"));
        } finally {
            // JcrState cleanup
            if (domainNode.hasNode("author")) {
                domainNode.getNode("author").remove();
            }
            if (domainNode.hasNode("editor")) {
                domainNode.getNode("editor").remove();
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
        }
    }
}
