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

package org.hippoecm.frontend.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.NodeTypeUtils;

import static org.junit.Assert.*;

public class DocumentListFilterTestMeh extends PluginTest {

    String[] content = new String[]{
            "/test",
            "nt:unstructured",
            "/test/content",
            "hippostd:folder",
            "/test/content/toBeShowed",
            "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/toBeShowed/toBeShowed",
            "test:faqitem",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/toBeHidden",
            "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/toBeHidden/toBeHidden",
            "hippo:document",
            "jcr:mixinTypes", "mix:referenceable",
            "/plugin",
            "nt:unstructured",
            "/plugin/filters",
            "frontend:pluginconfig",
            "/plugin/filters/showFaqItemType",
            "frontend:pluginconfig",
            "child", "hippo:handle",
            "display", "true",
            "subchild", "test:faqitem",
            "/plugin/filters/hideDocuments",
            "frontend:pluginconfig",
            "child", "hippo:handle",
            "display", "false",
            "subchild", "hippo:document",


    };


    private static <T> List<T>
    getListFromIterator(Iterator<T> iterator) {

        // Create an empty list
        List<T> list = new ArrayList<>();

        // Add each element of iterator to the List
        iterator.forEachRemaining(list::add);

        // Return the List
        return list;
    }

    //    @Ignore
    @Test
    public void filter() throws RepositoryException {
        session.getWorkspace().getNamespaceRegistry().registerNamespace("test", "http://www.test.com/test/nt/1.0");
        NodeTypeUtils.initializeNodeTypes(session, getClass().getResourceAsStream("/test.cnd"), "test");
        build(content, session);
        session.save();

        IPluginConfig config = new JcrPluginConfig(new JcrNodeModel(session.getNode("/plugin")));
        DocumentListFilter documentListFilter = new DocumentListFilter(config);

        Node folder = session.getNode("/test/content");

        NodeIterator it = documentListFilter.filter(folder, folder.getNodes());

        List<Node> list = getListFromIterator(it);
        assertTrue(list.size() == 1);
        assertTrue(list.get(0).getName().equals("toBeShowed"));
        removeNode("/plugin");
    }
}
