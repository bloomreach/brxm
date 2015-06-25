/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.journal;

import java.io.File;
import java.util.UUID;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class ExternalRepositorySyncRevisionServiceTest {

    private static final Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());

    @Test
    public void testSyncRevision() throws Exception {
        final File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        final File storage = new File(tmpdir, "repository-" + UUID.randomUUID().toString());
        String originalRepoConfig = System.getProperty("repo.config", null);
        HippoRepository repo = null;
        Exception ex = null;
        try {
            String repoPath = storage.getAbsolutePath();
            System.setProperty("repo.config", "/journal/repository.xml");
            repo = HippoRepositoryFactory.getHippoRepository(repoPath);
            Session session = repo.login(credentials);

            InternalHippoRepository internalHippoRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(session.getRepository());
            ExternalRepositorySyncRevisionService syncRevisionService = internalHippoRepository.getExternalRepositorySyncRevisionService();
            ExternalRepositorySyncRevision syncRevision = syncRevisionService.getSyncRevision("test");
            assertNotNull("With cluster DatabaseJournal syncRevision should not be null", syncRevision);
            assertEquals("SyncRevision.qualifiedId", ExternalRepositorySyncRevisionService.EXTERNAL_REPOSITORY_SYNC_ID_PREFIX+"test", syncRevision.getQualifiedId());
            assertFalse("Initial syncRevision value should not exist", syncRevision.exists());
            syncRevision.set(12345);
            assertTrue("syncRevision should be created", syncRevision.exists());
            assertEquals("Stored revision", 12345, syncRevision.get());
            ExternalRepositorySyncRevision localRevision = syncRevisionService.getSyncRevision("node");
            assertNotNull("Local node revision should exist", localRevision.exists());
        }
        catch (Exception e) {
            ex = e;
        }
        finally {
            if (originalRepoConfig != null) {
                System.setProperty("repo.config", originalRepoConfig);
            }
            else {
                System.clearProperty("repo.config");
            }
            if (repo != null) {
                repo.close();
            }
            FileUtils.deleteQuietly(storage);
            if (ex != null) {
                throw ex;
            }
        }
    }
}
