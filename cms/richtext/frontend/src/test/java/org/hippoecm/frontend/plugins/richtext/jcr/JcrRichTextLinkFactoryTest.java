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
package org.hippoecm.frontend.plugins.richtext.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertTrue;

public class JcrRichTextLinkFactoryTest extends PluginTest {

    @Test
    public void createLink() throws RichTextException, RepositoryException {
        final MockNode root = MockNode.root();

        final Node targetHandle = root.addNode("target", "hippo:handle");
        targetHandle.addNode("target", "hippo:document");

        final Node sourceHandle = root.addNode("source", "hippo:handle");
        final Node html = sourceHandle.addNode("source", "richtexttest:testdocument").addNode("richtexttest:html", "hippostd:html");
        html.setProperty("hippostd:content", "testing 1 2 3");

        JcrRichTextLinkFactory factory = new JcrRichTextLinkFactory(new JcrNodeModel(html));
        factory.createLink(new JcrNodeModel(targetHandle));

        assertTrue(html.hasNode("target"));
    }

}
