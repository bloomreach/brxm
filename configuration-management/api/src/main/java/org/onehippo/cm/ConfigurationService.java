/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cms7.services.SingletonService;

/**
 * Service providing access to the current runtime ConfigurationModel and some operations related to configuration.
 */
@SingletonService
public interface ConfigurationService {

    /**
     * Retrieve the current (partial) runtime ConfigurationModel This model will not contain
     * content definitions, which are not stored/retained in the runtime ConfigurationModel.
     */
    ConfigurationModel getRuntimeConfigurationModel();

    /**
     * @return Returns true if AutoExport is allowed (though perhaps currently disabled)
     */
    boolean isAutoExportAvailable();

    /**
     * Export a JCR node, all descendants, and binary resources to a zip file.
     * @param nodeToExport {@link Node}
     * @return Zip {@link File} containing content with binaries. File must be deleted by caller as necessary.
     */
    File exportZippedContent(Node nodeToExport) throws RepositoryException, IOException;

    /**
     * Export a JCR node and all descendants as text only. Note that this may be an incomplete representation if any
     * included nodes have binary properties, which would normally be represented with separate resource files.
     * @param nodeToExport {@link Node}
     * @return Textual representation of the node and its descendants
     */
    String exportContent(Node nodeToExport) throws RepositoryException, IOException;

    /**
     * Import a JCR node, all descendants, and binary resources into a given parent node. The input file is expected
     * to match the format exported by {@link #exportZippedContent(Node)}.
     * TODO: define input ZIP format
     * @param zipFile zip {@link File}
     * @param parentNode parent {@link Node}
     */
    void importZippedContent(File zipFile, Node parentNode) throws RepositoryException, IOException;

    /**
     * Import a single content definition into a given parent JCR node. The inputStream should contain a YAML-formatted
     * content definition as created by {@link #exportContent(Node)}.
     * @param inputStream {@link InputStream} representation of yaml
     * @param parentNode parent {@link Node}
     */
    void importPlainYaml(final InputStream inputStream, final Node parentNode) throws RepositoryException;

}
