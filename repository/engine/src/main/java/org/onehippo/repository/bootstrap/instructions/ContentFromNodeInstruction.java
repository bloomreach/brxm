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

import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENT;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.INIT_FOLDER_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.initializeNodecontent;

public class ContentFromNodeInstruction extends InitializeInstruction {

    public ContentFromNodeInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_CONTENT;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final String contentRoot = item.getContentRoot();
        if (contentRoot.equals(INIT_FOLDER_PATH) || contentRoot.startsWith(INIT_FOLDER_PATH + "/")) {
            throw new RepositoryException(String.format("Bootstrapping content to %s is not supported", INIT_FOLDER_PATH));
        }
        final InputStream contentStream = item.getContent();
        initializeNodecontent(session, contentRoot, contentStream, null, false);
        return null;
    }



}
