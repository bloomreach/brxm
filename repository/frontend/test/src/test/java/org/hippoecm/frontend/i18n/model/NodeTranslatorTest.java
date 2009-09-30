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
package org.hippoecm.frontend.i18n.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.TestCase;
import org.junit.Test;

public class NodeTranslatorTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    /**
     * Name of a versioned node is available in the parent history.
     * Translator takes care of this when hippo:paths is present on the node,
     * with the parent as one of its values.
     */
    public void testVersionedNode() throws Exception {
        Node parent = session.getRootNode().addNode("test", "nt:unstructured");
        parent.addMixin("mix:versionable");
        Node child = parent.addNode("child", "nt:unstructured");
        child.addMixin("mix:versionable");
        child.setProperty("hippo:paths", new String[] { child.getUUID(), parent.getUUID() });
        session.save();
        child.checkin();
        parent.checkin();

        VersionHistory vh = child.getVersionHistory();

        parent.checkout();
        child.remove();
        session.save();
        parent.checkin();

        VersionIterator versionIter = vh.getAllVersions();
        versionIter.nextVersion(); // ignore root
        Version version = versionIter.nextVersion();

        NodeTranslator nt = new NodeTranslator(new JcrNodeModel(version.getNode("jcr:frozenNode")));
        assertEquals("child", nt.getNodeName().getObject());
        assertTrue(version.getNode("jcr:frozenNode").isNodeType("mix:referenceable"));
    }

}
