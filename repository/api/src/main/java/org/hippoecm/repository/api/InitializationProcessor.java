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
package org.hippoecm.repository.api;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;

/**
 * Using an InitializationProcessor you can load hippoecm-extension.xml files into the repository, and execute
 * initialize items in order to bootstrap configuration.
 */
public interface InitializationProcessor {

    static final String INITIALIZATION_FOLDER = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH;

    /**
     * Load all hippoecm-extension.xml files that are on the current classpath into the repository.
     *
     * @param session the {@link Session} to use.
     * @return  the {@link List} of initialize item {@link Node}s that are pending after loading
     * @throws RepositoryException
     * @throws IOException
     */
    List<Node> loadExtensions(Session session) throws RepositoryException, IOException;

    /**
     * Load a specific hippoecm-extension.xml file into the repository.
     *
     * @param session the {@link Session} to use.
     * @param extension the hippoecm-extension.xml to load
     * @return  the {@link List} of initialize item {@link Node}s that are pending after loading
     * @throws RepositoryException
     * @throws IOException
     */
    List<Node> loadExtension(Session session, URL extension) throws RepositoryException, IOException;

    /**
     * Process (execute) all pending initialize items currently loaded in the repository.
     *
     * @param session the {@link Session} to use.
     */
    void processInitializeItems(Session session);

    /**
     * Process (execute) a specific list of initialize items.
     *
     * @param session the {@link Session} to use.
     * @param initializeItems the items to process
     */
    void processInitializeItems(Session session, List<Node> initializeItems);

    /**
     * Set alternative logger to write messages to.
     *
     * @param logger  the logger to use
     */
    void setLogger(Logger logger);
}
