/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class RepoPathTest {

    private static class RepoPathTestLocalHippoRepository extends LocalHippoRepository {
        RepoPathTestLocalHippoRepository() throws Exception {
            super();
        }
    }

    @Test
    public void testBaseRepoPath() throws Exception  {
        final String originalRepoBasePath = System.getProperty(LocalHippoRepository.SYSTEM_BASE_PATH_PROPERTY, null);
        final String originalRepoPath = System.getProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, null);
        try {
            final String userDir = new File(System.getProperty("user.dir")).getAbsolutePath();
            final String homeDir = new File(System.getProperty("user.home")).getAbsolutePath();
            final String tmpDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
            final String fileSeparator = System.getProperty("file.separator");
            assertNotSame(tmpDir,userDir);

            // Default workingDirectory should == user.dir
            assertEquals(userDir, new RepoPathTestLocalHippoRepository().getWorkingDirectory());

            // No system property repo.path set: repositoryPath should be == to workingDirectory
            assertEquals(userDir, new RepoPathTestLocalHippoRepository().getRepositoryPath());

            System.setProperty(LocalHippoRepository.SYSTEM_BASE_PATH_PROPERTY, tmpDir);
            assertEquals(tmpDir, System.getProperty(LocalHippoRepository.SYSTEM_BASE_PATH_PROPERTY));

            // Setting repo.base.path without repo.path should have no effect
            assertEquals(userDir, new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // A repo.path which is absolute will be used as such
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "/subPath");
            assertEquals("/subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // A repo.path which is relative will use repo.base.path as prefix if defined
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "subPath");
            assertEquals(tmpDir + fileSeparator + "subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // A repo.path starting with a '.' also is relative and also will use repo.base.path as prefix
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "./subPath");
            assertEquals(tmpDir+fileSeparator+"./subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // A repo.path starting with a '~/' is first expanded to a user.home relative path (thus becoming absolute
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "~/subPath");
            assertEquals(homeDir+"/subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // Tests with no repo.base.path set (or empty)
            System.setProperty(LocalHippoRepository.SYSTEM_BASE_PATH_PROPERTY, "");

            // A repo.path which is absolute will be used as such
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "/subPath");
            assertEquals("/subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // a path not starting which is relative without repo.base.path defined will be treated as if absolute
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "subPath");
            assertEquals("subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // A repo.path starting with a '.' also is relative but without repo.base.path be treated as if absolute
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "./subPath");
            assertEquals("./subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());

            // A repo.path starting with a '~/' is first expanded to a user.home relative path (thus becoming absolute
            System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, "~/subPath");
            assertEquals(homeDir+"/subPath", new RepoPathTestLocalHippoRepository().getRepositoryPath());
        }
        finally {
            if (originalRepoBasePath != null) {
                System.setProperty(LocalHippoRepository.SYSTEM_BASE_PATH_PROPERTY, originalRepoBasePath);
            }
            else {
                System.clearProperty(LocalHippoRepository.SYSTEM_BASE_PATH_PROPERTY);
            }
            if (originalRepoPath != null) {
                System.setProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY, originalRepoPath);
            }
            else {
                System.clearProperty(LocalHippoRepository.SYSTEM_PATH_PROPERTY);
            }
        }
    }
}
