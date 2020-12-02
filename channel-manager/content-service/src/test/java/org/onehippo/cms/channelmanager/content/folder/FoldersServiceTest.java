/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.folder;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.slug.SlugFactory;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({SlugFactory.class})
public class FoldersServiceTest {

    private final FoldersService fs = FoldersService.get();

    private MockNode root;
    private UserContext userContext;

    @Before
    public void setup() throws RepositoryException {
        PowerMock.mockStatic(SlugFactory.class);
        root = MockNode.root();
        userContext = new TestUserContext(root.getSession());
    }

    @Test
    public void emptyPathReturnsEmptyList() throws Exception {
        assertTrue(fs.getFolders(null, userContext).isEmpty());
        assertTrue(fs.getFolders("", userContext).isEmpty());
    }

    @Test
    public void rootPathReturnsEmptyList() throws Exception {
        assertTrue(fs.getFolders("/", userContext).isEmpty());
    }

    @Test
    public void testNonExistingPath() throws Exception {
        expect(SlugFactory.createSlug("planets", null)).andReturn("planets");
        expect(SlugFactory.createSlug("neptune", null)).andReturn("neptune");
        replayAll();

        final List<Folder> folders = fs.getFolders("/planets/neptune", userContext);
        assertThat(folders.size(), is(2));
        assertFolder(folders.get(0), "planets", "planets", "/planets", null, false);
        assertFolder(folders.get(1), "neptune", "neptune", "/planets/neptune", null, false);

        verifyAll();
    }

    @Test
    public void testExistingPath() throws Exception {
        final Node planetsNode = root.addNode("planets", HippoStdNodeType.NT_FOLDER);
        final Node neptuneNode = planetsNode.addNode("neptune", HippoStdNodeType.NT_FOLDER);
        neptuneNode.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        neptuneNode.setProperty(HippoTranslationNodeType.LOCALE, "en");


        final List<Folder> folders = fs.getFolders("/planets/neptune", userContext);
        assertThat(folders.size(), is(2));
        assertFolder(folders.get(0), "planets", "planets", "/planets", null, true);
        assertFolder(folders.get(1), "neptune", "neptune", "/planets/neptune", "en", true);
    }

    @Test
    public void testPartiallyExistingPath() throws Exception {
        root.addNode("planets", HippoStdNodeType.NT_FOLDER);
        expect(SlugFactory.createSlug("neptune", null)).andReturn("neptune");
        replayAll();

        final List<Folder> folders = fs.getFolders("/planets/neptune", userContext);
        assertThat(folders.size(), is(2));
        assertFolder(folders.get(0), "planets", "planets", "/planets", null, true);
        assertFolder(folders.get(1), "neptune", "neptune", "/planets/neptune", null, false);
        verifyAll();
    }

    @Test
    public void testNonExistingPathIsEncoded() throws Exception {
        expect(SlugFactory.createSlug("The planet Neptune", "de")).andReturn("the-planet-neptune");
        replayAll();

        final Node planetsNode = root.addNode("planets", HippoStdNodeType.NT_FOLDER);
        planetsNode.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        planetsNode.setProperty(HippoTranslationNodeType.LOCALE, "de");

        final List<Folder> folders = fs.getFolders("/planets/The planet Neptune", userContext);
        assertThat(folders.size(), is(2));
        assertFolder(folders.get(1), "the-planet-neptune", "The planet Neptune", "/planets/the-planet-neptune", "de", false);
        verifyAll();
    }

    @Test
    public void testDisplayName() throws Exception {
        final Node planetsNode = root.addNode("planets", HippoStdNodeType.NT_FOLDER);
        final Node neptuneNode = planetsNode.addNode("neptune", HippoStdNodeType.NT_FOLDER);
        neptuneNode.setProperty(HippoNodeType.HIPPO_NAME, "Neptune");

        final List<Folder> folders = fs.getFolders("/planets/neptune", userContext);
        assertThat(folders.get(1).getDisplayName(), is("Neptune"));
    }

    @Test
    public void testLocale() throws Exception {
        final Node planetsNode = root.addNode("planets", HippoStdNodeType.NT_FOLDER);
        final Node neptuneNode = planetsNode.addNode("neptune", HippoStdNodeType.NT_FOLDER);
        neptuneNode.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        neptuneNode.setProperty(HippoTranslationNodeType.LOCALE, "de");
        neptuneNode.addNode("laomedeia", HippoStdNodeType.NT_FOLDER);

        final List<Folder> folders = fs.getFolders("/planets/neptune/laomedeia", userContext);
        assertNull(folders.get(0).getLocale());
        assertThat(folders.get(1).getLocale(), is("de"));
        assertThat(folders.get(2).getLocale(), is("de"));
    }

    private static void assertFolder(final Folder folder, final String name, final String displayName,
                              final String path, final String locale, final boolean exists) {
        assertThat(folder.getName(), is(name));
        assertThat(folder.getDisplayName(), is(displayName));
        assertThat(folder.getPath(), is(path));
        assertThat(folder.getLocale(), is(locale));
        assertThat(folder.getExists(), is(exists));
    }
}
