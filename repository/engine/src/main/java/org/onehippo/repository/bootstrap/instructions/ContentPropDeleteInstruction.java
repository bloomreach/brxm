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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPDELETE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class ContentPropDeleteInstruction extends InitializeInstruction {

    public ContentPropDeleteInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_CONTENTPROPDELETE;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final String path = item.getContentPropDeletePath();
        if (!path.startsWith("/")) {
            throw new RepositoryException(String.format("Invalid property delete item %s: %s is not an absolute path", item.getName(), path));
        }
        if (session.propertyExists(path)) {
            session.getProperty(path).remove();
        } else {
            log.info("Content property delete {} property {} not found", item.getName(), path);
        }
        return null;
    }

}
