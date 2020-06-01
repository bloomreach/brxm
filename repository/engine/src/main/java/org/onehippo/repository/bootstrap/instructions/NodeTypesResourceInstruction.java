/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NODETYPES;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.initializeNodetypes;

public class NodeTypesResourceInstruction extends InitializeInstruction {

    public NodeTypesResourceInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_NODETYPES;
    }

    @Override
    protected boolean canCombine(final InitializeInstruction instruction) {
        return instruction instanceof NamespaceInstruction;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final URL cndURL = item.getNodetypesResourceURL();
        try (InputStream is = cndURL.openStream()) {
            initializeNodetypes(session, is, cndURL.toString());
        } catch (IOException e) {
            throw new RepositoryException("Failed to read cnd " + cndURL.toString(), e);
        }
        return null;
    }
}
