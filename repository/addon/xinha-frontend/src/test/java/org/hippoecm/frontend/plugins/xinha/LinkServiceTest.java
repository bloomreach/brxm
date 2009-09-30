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
package org.hippoecm.frontend.plugins.xinha;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLinkService;
import org.junit.Before;
import org.junit.Test;

public class LinkServiceTest extends PluginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String[] content = {
            "/test", "nt:unstructured",
                "/test/target", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/target/target", "hippo:document",
                        "jcr:mixinTypes", "hippo:harddocument",
                "/test/source", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/source/source", "xinhatest:testdocument",
                        "jcr:mixinTypes", "hippo:harddocument",
                        "/test/source/source/xinhatest:html", "hippostd:html",
                            "hippostd:content", "testing 1 2 3"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);
        session.save();
    }

    @Test
    public void linkLifecycleTest() throws RepositoryException {
        Node html = root.getNode("test/source/source/xinhatest:html");
        XinhaLinkService linkService = new XinhaLinkService(new JcrNodeModel(html)) {

            @Override
            protected String getXinhaName() {
                return "xinha";
            }
        };

        Node target = root.getNode("test/target");
        linkService.attach(new JcrNodeModel(target));

        assertTrue(root.hasNode("test/source/source/xinhatest:html/target"));

        linkService.cleanup(new TreeSet<String>());
        assertFalse(root.hasNode("test/source/source/xinhatest:html/target"));
    }

}
