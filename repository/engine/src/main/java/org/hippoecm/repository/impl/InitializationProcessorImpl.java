/*
 *  Copyright 2012 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.LoadInitializationModule;
import org.hippoecm.repository.api.InitializationProcessor;

public class InitializationProcessorImpl implements InitializationProcessor {

    @Override
    public List<Node> loadExtensions(final Session session) throws RepositoryException, IOException {
        return LoadInitializationModule.loadExtensions(session, session.getNode(INITIALIZATION_FOLDER));
    }

    @Override
    public List<Node> loadExtension(final Session session, final URL extension) throws RepositoryException, IOException {
        return LoadInitializationModule.loadExtension(extension, session, session.getNode(INITIALIZATION_FOLDER));
    }

    @Override
    public void processInitializeItems(final Session session) {
        LoadInitializationModule.processInitializeItems(session);
    }

    @Override
    public void processInitializeItems(final Session session, final List<Node> initializeItems) {
        LoadInitializationModule.processInitializeItems(session, initializeItems);
    }
}
