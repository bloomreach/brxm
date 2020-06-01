/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.MockPluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DocumentListFilterTest extends MockPluginTest {

    @Test
    public void filterChildDocuments() throws IOException, RepositoryException {
        MockNodeFactory.importYaml("/org/hippoecm/frontend/plugins/standards/DocumentListFilterTest-child-documents.yaml", root);

        final JcrPluginConfig pluginConfig = new JcrPluginConfig(new JcrNodeModel(root.getNode("test/plugin")));
        final DocumentListFilter documentListFilter = new DocumentListFilter(pluginConfig);

        final Node folder = root.getNode("test/content");
        final NodeIterator nodeIterator = documentListFilter.filter(folder, folder.getNodes());

        assertEquals("toBeShowed", nodeIterator.nextNode().getName());
        assertFalse("There should be only one child node left", nodeIterator.hasNext());
    }
}
