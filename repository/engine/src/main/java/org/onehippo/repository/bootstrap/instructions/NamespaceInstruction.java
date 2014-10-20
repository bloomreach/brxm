/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap.instructions;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAMESPACE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class NamespaceInstruction extends InitializeInstruction {

    public NamespaceInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_NAMESPACE;
    }

    @Override
    protected boolean canCombine(final InitializeInstruction instruction) {
        return instruction instanceof NodeTypesResourceInstruction;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final String uri = item.getNamespace();
        final String prefix = item.getName();
        log.info("Initializing namespace: {}:{}", prefix, uri);
        final NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
        try {
            String currentURI = registry.getURI(prefix);
            if (currentURI.equals(uri)) {
                log.debug("Namespace already exists: {}:{}", prefix, uri);
            } else {
                throw new RepositoryException("Prefix already used for different namespace: " + prefix + ":" + uri);
            }
        } catch (NamespaceException ex) {
            // mapping does not exist
            registry.registerNamespace(prefix, uri);
        }
        return null;
    }

}
