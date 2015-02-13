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

package org.onehippo.cms7.essentials;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class TestPluginContext extends DefaultPluginContext {

    private static final Logger log = LoggerFactory.getLogger(TestPluginContext.class);
    private static final long serialVersionUID = 1L;


    private final MemoryRepository repository;

    private HippoRepository hippoRepository;
    private boolean useHippoSession;
    public TestPluginContext(final MemoryRepository repository) {
        this.repository = repository;
    }




    @Override
    public String getProjectNamespacePrefix() {
        final String projectNamespacePrefix = super.getProjectNamespacePrefix();
        if (Strings.isNullOrEmpty(projectNamespacePrefix)) {
            return BaseTest.PROJECT_NAMESPACE_TEST;
        }
        return projectNamespacePrefix;
    }

    public HippoRepository getHippoRepository() {
        return hippoRepository;
    }

    public void setHippoRepository(final HippoRepository hippoRepository) {
        this.hippoRepository = hippoRepository;
    }

    public boolean isUseHippoSession() {
        return useHippoSession;
    }

    public void setUseHippoSession(final boolean useHippoSession) {
        this.useHippoSession = useHippoSession;
    }

    @Override
    public Session createSession() {
        try {
            if(useHippoSession){
                Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
                return hippoRepository.login(credentials);
            }
            return repository.getSession();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return null;
    }




}
