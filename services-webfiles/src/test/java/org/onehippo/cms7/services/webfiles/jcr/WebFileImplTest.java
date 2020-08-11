/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.WebFileNotFoundException;
import org.onehippo.repository.mock.MockBinary;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;

public class WebFileImplTest {

    private Node webFiles;
    private Node styleCssNode;

    @Before
    public void setUp() throws RepositoryException, IOException {
        Node root = MockNode.root();
        webFiles = root.addNode("webfiles", "nt:folder");
        Node css = webFiles.addNode("css", "nt:folder");
        styleCssNode = css.addNode("style.css", "nt:file");
        Node content = styleCssNode.addNode("jcr:content", "nt:resource");
        final InputStream inputStream = IOUtils.toInputStream("Hello world!");
        content.setProperty("jcr:data", new MockBinary(inputStream));
        content.setProperty("jcr:mimeType", "text/css");
        content.setProperty("jcr:lastModified", Calendar.getInstance());
    }

    @Test
    public void getPath() throws RepositoryException {
        WebFileImpl styleCss = new WebFileImpl(styleCssNode);
        assertEquals("/webfiles/css/style.css", styleCss.getPath());
    }

    @Test
    public void getName() throws RepositoryException {
        WebFileImpl styleCss = new WebFileImpl(styleCssNode);
        assertEquals("style.css", styleCss.getName());
    }

    @Test
    public void getContent() throws Exception {
        WebFileImpl styleCss = new WebFileImpl(styleCssNode);
        final String content = IOUtils.toString(styleCss.getBinary().getStream());
        assertEquals("Hello world!", content);
    }

    @Test
    public void versionedNode_supported_as_webFileImpl() throws RepositoryException, IOException {
        styleCssNode.addMixin(JcrConstants.MIX_VERSIONABLE);
        final VersionManager versionManager = styleCssNode.getSession().getWorkspace().getVersionManager();
        final Version checkedInBeforeChange = versionManager.checkin(styleCssNode.getPath());
        WebFileImpl versionedStyleCssBeforeChange = new WebFileImpl(checkedInBeforeChange.getFrozenNode());

        versionManager.checkout(styleCssNode.getPath());
        Node content = styleCssNode.getNode("jcr:content");
        final InputStream inputStream = IOUtils.toInputStream("Hello world again!");
        content.setProperty("jcr:data", new MockBinary(inputStream));

        final Version checkedInAfterChange = versionManager.checkin(styleCssNode.getPath());
        WebFileImpl versionedStyleCssAfterChange = new WebFileImpl(checkedInAfterChange.getFrozenNode());

        WebFileImpl styleCss = new WebFileImpl(styleCssNode);
        final String workspaceContent = IOUtils.toString(styleCss.getBinary().getStream());
        assertEquals("Hello world again!", workspaceContent);

        final String versionedContentBeforeChange = IOUtils.toString(versionedStyleCssBeforeChange.getBinary().getStream());
        assertEquals("Hello world!", versionedContentBeforeChange);

        final String versionedContentAfterChange = IOUtils.toString(versionedStyleCssAfterChange.getBinary().getStream());
        assertEquals("Hello world again!", versionedContentAfterChange);
    }

    @Test(expected = WebFileNotFoundException.class)
    public void unsupported_webFileImpl_nodetype() throws RepositoryException {
        Node folder = webFiles.addNode("folder", "nt:folder");
        new WebFileImpl(folder);
    }

}
