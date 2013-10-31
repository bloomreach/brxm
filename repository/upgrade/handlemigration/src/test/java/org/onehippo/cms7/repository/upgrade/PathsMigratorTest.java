/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test that verifies that the migration of the hippo:paths property, from hippo:harddocument to hippo:document,
 * can be executed safely in a running system.
 */
public class PathsMigratorTest {

    private File repoDir;

    @Before
    public void createRepository() throws Exception {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final Path tmpPath = FileSystems.getDefault().getPath(tmpDir);
        repoDir = Files.createTempDirectory(tmpPath, PathsMigratorTest.class.getCanonicalName() + "-repository-").toFile();

        RepositoryImpl jackrabbitRepository = createJRRepository();

        final Session session = jackrabbitRepository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        final Workspace workspace = session.getWorkspace();
        final NamespaceRegistry nsReg = workspace.getNamespaceRegistry();
        try {
            nsReg.getPrefix("hippo");
        } catch (NamespaceException nse) {
            nsReg.registerNamespace("hippo", "http://www.onehippo.org/jcr/hippo/nt/2.0.4");
        }

        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("hippo-old.cnd"));
        CndImporter.registerNodeTypes(reader, session, true);
        reader = new InputStreamReader(getClass().getResourceAsStream("/hipposys.cnd"));
        CndImporter.registerNodeTypes(reader, session, true);

        if (!session.nodeExists("/configuration")) {
            session.importXML("/",
                    getClass().getResourceAsStream("/org/hippoecm/repository/configuration.xml"),
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            session.save();
        }
        jackrabbitRepository.shutdown();
    }

    private void transformToHippoRepository() throws IOException {
        FileUtils.copyURLToFile(getClass().getResource("workspace.xml"), new File(repoDir, "workspaces/default/workspace.xml"));
    }

    private RepositoryImpl createJRRepository() throws RepositoryException {
        final RepositoryConfig repConfig = RepositoryConfig.create(PathsMigratorTest.class.getResourceAsStream("repository.xml"),
                repoDir.getAbsolutePath());

        return new RepositoryImpl(repConfig) {};
    }

    @After
    public void deleteRepository() {
        FileUtils.deleteQuietly(repoDir);
        System.clearProperty(LocalHippoRepository.SYSTEM_CONFIG_PROPERTY);
    }

    @Test
    public void testContentIsNotTouchedDuringUpgrade() throws LoginException, RepositoryException, InterruptedException, IOException, ParseException {
        // create content using jackrabbit repository
        final RepositoryImpl jrRepository = createJRRepository();
        Session session = jrRepository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        final Node test = session.getRootNode().addNode("test", JcrConstants.NT_UNSTRUCTURED);
        test.addMixin("mix:referenceable");
        final Node doc = test.addNode("doc", "hippo:document");
        doc.addMixin(HippoNodeType.NT_HARDDOCUMENT);
        doc.setProperty(HippoNodeType.HIPPO_PATHS, new Value[]{session.getValueFactory().createValue(session.getRootNode().getIdentifier())});
        session.save();
        session.logout();
        jrRepository.shutdown();

        transformToHippoRepository();

        // start up with hippo repository, to migrate the content
        System.setProperty(LocalHippoRepository.SYSTEM_CONFIG_PROPERTY, "/org/onehippo/cms7/repository/upgrade/hippo-repository.xml");
        HippoRepository repository = HippoRepositoryFactory.getHippoRepository("file://" + repoDir.getAbsolutePath());

        try {
            Session testSession;
            // create parallel session
            {
                testSession = repository.login((Credentials) new SimpleCredentials("admin", "admin".toCharArray()));

                // populate type cache
                Node testDoc = testSession.getNode("/test/doc");
                final Property pathsProperty = testDoc.getProperty("hippo:paths");
                final NodeType declaringNodeType = pathsProperty.getDefinition().getDeclaringNodeType();
                assertEquals("hippo:harddocument", declaringNodeType.getName());
            }

            // update using separate session
            {
                session = repository.login((Credentials) new SimpleCredentials("admin", "admin".toCharArray()));
                InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/hippo.cnd"));
                CndImporter.registerNodeTypes(reader, session, true);
                session.logout();
            }

            // verify updated content with clean session
            {
                session = repository.login((Credentials) new SimpleCredentials("admin", "admin".toCharArray()));

                Node newDoc = session.getNode("/test/doc");
                assertTrue(newDoc.hasProperty("hippo:paths"));
                assertTrue("document is not referenceable", newDoc.isNodeType("mix:referenceable"));

                Value[] values = newDoc.getProperty("hippo:paths").getValues();
                assertEquals(1, values.length);
                assertEquals(session.getRootNode().getIdentifier(), values[0].getString());
            }

            // verify parallel session
            {
                Node testDoc = testSession.getNode("/test/doc");
                assertTrue(testDoc.hasProperty("hippo:paths"));
                assertTrue("document is not referenceable", testDoc.isNodeType("mix:referenceable"));

                final Property pathsProperty = testDoc.getProperty("hippo:paths");
                Value[] values = pathsProperty.getValues();
                assertEquals(1, values.length);
                assertEquals(session.getRootNode().getIdentifier(), values[0].getString());

                final NodeType declaringNodeType = pathsProperty.getDefinition().getDeclaringNodeType();
                assertEquals("hippo:harddocument", declaringNodeType.getName());
            }
        } finally {
            repository.close();
        }
    }
}
