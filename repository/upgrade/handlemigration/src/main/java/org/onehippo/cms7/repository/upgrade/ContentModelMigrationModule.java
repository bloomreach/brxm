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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.onehippo.repository.modules.ExecutableDaemonModule;


public class ContentModelMigrationModule implements ExecutableDaemonModule {

    private HandleMigrator handleMigrator;
    private DocumentMigrator documentMigrator;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        handleMigrator = new HandleMigrator(session.impersonate(new SimpleCredentials("system", new char[] {})));
        handleMigrator.init();
        documentMigrator = new DocumentMigrator(session.impersonate(new SimpleCredentials("system", new char[] {})));
        documentMigrator.init();
    }

    @Override
    public void execute() throws RepositoryException {
//        handleMigrator.migrate();
        documentMigrator.migrate();
    }

    @Override
    public void cancel() {
        handleMigrator.cancel();
        documentMigrator.cancel();
    }

    @Override
    public void shutdown() {
        handleMigrator.shutdown();
    }

}
