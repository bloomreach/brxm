/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.onehippo.cms7.essentials.plugin.sdk.services.JcrServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("repository-test")
@Configuration
public class TestJcrService {

    @Bean
    @Primary
    public JcrService getJcrService() {
        return new TestJcrService.Service();
    }

    public static class Service extends JcrServiceImpl {

        private static final Logger LOG = LoggerFactory.getLogger(TestJcrService.class);

        private HippoRepository hippoRepository;
        private boolean initialized;

        public void setHippoRepository(final HippoRepository hippoRepository) {
            this.hippoRepository = hippoRepository;
        }

        public void reset() {
            MemoryRepository.reset();
            resetNodes();
        }

        public void registerNodeTypes(final String cndResourcePath) throws RepositoryException, IOException {
            ensureRepository();
            MemoryRepository.registerNodeTypes(cndResourcePath);
        }

        @Override
        public Session createSession() {
            if (hippoRepository != null) {
                try {
                    Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
                    return hippoRepository.login(credentials);
                } catch (RepositoryException e) {
                    LOG.error("Failed to create JCR session.", e);
                    return null;
                }
            }

            ensureRepository();
            return MemoryRepository.createSession();
        }

        void resetNodes() {
            initialized = false;
        }

        private void ensureRepository() {
            if (!initialized) {
                MemoryRepository.initialize();
                initialized = true;
            }
        }
    }
}
