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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.WebFile;
import org.onehippo.cms7.services.webfiles.WebFileNotFoundException;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebFileBundleImplTest {

    private WebFileBundleImpl bundle;

    @Before
    public void setUp() throws RepositoryException {
        Node root = MockNode.root();
        Node webFilesRoot = root.addNode("webfiles", "nt:folder");
        Node css = webFilesRoot.addNode("css", "nt:folder");
        final Node file = css.addNode("style.css", "nt:file");
        file.addNode("jcr:content", "nt:resource");
        bundle = new WebFileBundleImpl(root.getSession(), webFilesRoot);
    }

    @Test
    public void nonExistingWebFilesDoNotExist() {
        assertFalse(bundle.exists(null));
        assertFalse(bundle.exists(""));
        assertFalse(bundle.exists("/"));
        assertFalse(bundle.exists("/noSuchWebFile"));
        assertFalse(bundle.exists("/css/noSuchWebFile"));
    }

    @Test
    public void existingFoldersAreNotAnExistingWebFile() {
        assertFalse(bundle.exists("/css"));
        assertFalse(bundle.exists("/css/"));
    }

    @Test
    public void existingWebFileExists() {
        assertTrue(bundle.exists("/css/style.css"));
    }

    @Test
    public void relativePathDoesNotExist() {
        assertFalse(bundle.exists("css/style.css"));
    }

    @Test(expected = WebFileNotFoundException.class)
    public void getNullPath() {
        bundle.get(null);
    }

    @Test(expected = WebFileNotFoundException.class)
    public void getEmptyPath() {
        bundle.get("");
    }

    @Test(expected = WebFileNotFoundException.class)
    public void getRootPath() {
        bundle.get("/");
    }

    @Test(expected = WebFileNotFoundException.class)
    public void getNonExistingWebFile() {
        bundle.get("/noSuchWebFile");
    }

    @Test(expected = WebFileNotFoundException.class)
    public void getNonExistingWebFileInExistingFolder() {
        bundle.get("/css/noSuchWebFile");
    }

    @Test(expected = WebFileNotFoundException.class)
    public void getExistingFolder() {
        bundle.get("/css");
    }

    @Test
    public void getExistingWebFile() {
        final WebFile webFile = bundle.get("/css/style.css");
        assertNotNull(webFile);
        assertEquals("/webfiles/css/style.css", webFile.getPath());
        assertEquals("style.css", webFile.getName());
    }

}